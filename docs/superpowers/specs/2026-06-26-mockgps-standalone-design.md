# MockGPS Standalone — 設計規格

**日期：** 2026-06-26  
**狀態：** 已通過使用者確認  
**目標：** 一個完全獨立的 Android App，無需連接電腦即可模擬 GPS 位置，支援靜態定位與自動漫步，並提供地圖介面與地點搜尋。

---

## 背景

原 `Pikmin-auto` 專案中的 `MockLocationService` 需透過 ADB broadcast 從電腦端接收座標。本專案將其拆分為獨立 App，所有操作在手機上完成。核心 mock 邏輯（同時覆蓋 GPS + NETWORK provider 防止 snap-back）直接沿用。

**前提條件（一次性設定）：**  
手機「開發者選項 → 選取模擬位置應用程式」需設為本 App。

---

## 技術選型

| 項目 | 選擇 | 理由 |
|---|---|---|
| UI | Jetpack Compose | 現代化、與 ViewModel 整合佳 |
| 地圖 | OSMDroid | 免費、不需 API Key |
| 地點搜尋 | Nominatim REST API | 免費公共服務，搜尋準確度滿足日常需求 |
| 資料庫 | Room | 官方 ORM，收藏/歷史持久化 |
| 非同步 | Kotlin Coroutines + Flow | Walker 邏輯與 DB 查詢統一用 coroutine |
| 最低 SDK | 26 (Android 8.0) | `ProviderProperties` API 需求 |

---

## 架構

```
┌─────────────────────────────────────────────┐
│  UI Layer                                   │
│  MainScreen (Compose)                       │
│  ├─ MapView (AndroidView / OSMDroid)        │
│  └─ BottomSheetScaffold                     │
│     ├─ SearchBar + Results                  │
│     ├─ LatLon inputs + Mode toggle          │
│     ├─ Walker config (radius, speed)        │
│     └─ Favorites / Recents tabs            │
├─────────────────────────────────────────────┤
│  ViewModel Layer                            │
│  MainViewModel                              │
│  ├─ UiState (mode, lat, lon, radius, speed) │
│  ├─ Flow<List<FavoriteEntity>>              │
│  └─ Flow<List<RecentEntity>>               │
├─────────────────────────────────────────────┤
│  Service Layer                              │
│  MockLocationService (Foreground)           │
│  ├─ 靜態模式：每 1s push GPS + NETWORK      │
│  └─ Walker 模式：Coroutine 計算 + push      │
├─────────────────────────────────────────────┤
│  Data Layer                                 │
│  LocationRepository                         │
│  ├─ FavoriteDao (FavoriteEntity)            │
│  └─ RecentDao (RecentEntity, max 10)       │
└─────────────────────────────────────────────┘
```

Service ↔ ViewModel 透過 `LocalBinder`（已有模式）溝通。

---

## 資料模型

```kotlin
@Entity data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val lat: Double,
    val lon: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity data class RecentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String?,   // 搜尋結果的顯示名，直接輸入座標時為 null
    val lat: Double,
    val lon: Double,
    val usedAt: Long = System.currentTimeMillis()
)
```

`RecentDao` 在 insert 後執行清理，保留最新 10 筆。

---

## UI 規格

### 主畫面佈局

- **上方（65%）**：OSMDroid 全螢幕地圖
  - 可拖曳 Pin：放開後更新 ViewModel 座標
  - 點擊地圖任意位置移動 Pin
  - Walker 模式：Pin 周圍半透明圓形 Overlay 顯示漫步半徑
  - 右上角 FAB：「重新置中地圖到 Pin」
- **下方（35%，可上拉展開）**：BottomSheet

### BottomSheet — 收合狀態

```
[當前座標：25.0330, 121.5654]  [模式：靜態]  [● 開始 / ■ 停止]
```

### BottomSheet — 展開狀態

1. **搜尋列**：輸入關鍵字 → Nominatim（debounce 500ms）→ 下拉結果 → 點擊移動 Pin
2. **座標輸入**：緯度 / 經度 TextField，與 Pin 雙向同步
3. **模式切換**：`靜態` | `漫步` SegmentedButton
4. **Walker 設定**（漫步模式才顯示）：
   - 半徑 Slider：50 m ～ 2000 m（顯示數值）
   - 速度 Slider：1 ～ 10 m/s（顯示數值）
5. **收藏 / 最近** Tabs：點擊任一筆跳到該位置；收藏可長按刪除

---

## Service 邏輯

### 靜態模式

每 1000ms push 一次，GPS + NETWORK provider 各一筆（與原 `MockLocationService` 相同）。  
accuracy 加小隨機抖動（±1f）使 App 不覺得座標異常靜止。

### Walker 模式（移植自 Python gps/walker.py）

```
初始化：center = Pin 位置，radius，speed (m/s)
目標選取：在 center 半徑內隨機選目標點
每 1s：
  bearing = 從當前位置到目標的方位角
  前進 speed 公尺（加 ±0.5m 抖動）
  若距離目標 < 5m → 重選新目標
push GPS + NETWORK（與靜態模式相同機制）
```

Walker coroutine 在 Service 內以 `CoroutineScope(Dispatchers.Default)` 運行，`stopMocking()` 時 cancel。

---

## 優化項目

### 電池豁免提示
首次啟動（`SharedPreferences` 標記）且未在豁免名單時，彈出 Dialog 引導用戶至系統設定 `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`。

### 通知快捷停止
`MockLocationService` 的 Foreground Notification 加一個 `PendingIntent` Action Button「停止」，廣播給 Service 呼叫 `stopMocking()`，不需開 App。

### 開發者選項深連結
`providerReady = false` 時，UI 顯示警告卡片 + 「前往設定」按鈕，跳轉 `ACTION_APPLICATION_DEVELOPMENT_SETTINGS`。

### 複製座標
座標 Text 支援長按 → 複製「25.0330, 121.5654」至剪貼簿。

---

## 套件結構

```
com.mockgps.standalone
├── data/
│   ├── db/          AppDatabase, FavoriteDao, RecentDao
│   ├── model/       FavoriteEntity, RecentEntity
│   └── repository/  LocationRepository
├── service/
│   └── MockLocationService
├── ui/
│   ├── MainViewModel
│   ├── screen/      MainScreen
│   ├── component/   MapView, BottomSheetContent, SearchBar,
│   │                FavoritesTab, RecentsTab, WalkerConfig
│   └── theme/       Color, Theme, Type
└── util/
    └── GeoMath      (方位角計算，移植自 gps/geo.py)
```

---

## 不在本版本範圍內

- 路線規劃（A → B 路線模擬）
- 速度曲線（加速 / 減速）
- 分享路線檔（GPX import/export）

---

## 驗收標準

1. 手機不連電腦，設定模擬位置 App 後，靜態模式可在 Google Maps 顯示指定位置
2. 漫步模式下藍點在設定半徑內移動，不 snap-back
3. 搜尋「台北車站」可找到並跳到正確位置
4. 收藏 / 最近紀錄在 App 重啟後仍保留
5. 背景執行超過 10 分鐘 Service 未被系統殺掉（電池豁免已設定的前提下）
