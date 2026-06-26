# MockGPS-standalone

MockGPS-standalone 是一個獨立且輕量化的 Android 模擬 GPS 定位應用程式（基於 OpenStreetMap 核心與 Jetpack Compose 開發）。它提供穩定的背景模擬定位服務，非常適合開發人員測試定位相關的功能，或是需要模擬動態位置變化的情境。

---

## 📱 下載與安裝

可以直接在此專案的根目錄下載最新編譯完成的安裝檔：
* **[MockGPS.apk](./MockGPS.apk)** (點擊下載直接安裝)

---

## ✨ 主要特色與功能

* 🗺️ **地圖選點**：使用 OpenStreetMap，直接在地圖上點擊或拖移，即可精確選擇要模擬的經緯度。
* 🔍 **地點搜尋**：整合地點搜尋功能，輸入名稱或關鍵字即可快速跳轉至指定位置。
* ⭐ **常用與最近地點**：支援將常用地點儲存至我的最愛（Favorites），並會自動記錄最近使用的模擬歷史（Recents）。
* 🚶 **雙重模擬模式**：
  * **靜態模式 (Static)**：將系統定位固定在特定的單一座標點。
  * **步行模式 (Walker)**：可在指定的**移動半徑 (Radius)** 與**移動速度 (Speed)** 下，模擬在該地點周圍隨機步行移動，特別適合測試動態軌跡或步數/運動相關的服務。
* 🛡️ **背景穩定運行**：使用 Android 前台定位服務 (Foreground Service Location)，搭配提醒用戶忽略電池優化 (Request Ignore Battery Optimizations)，確保在螢幕關閉或 App 進入背景時，模擬定位服務不會被系統輕易終止。

---

## 🛠️ 使用前設定 (重要)

由於 Android 系統安全機制限制，要正常使用 MockGPS，請依照以下步驟進行設定：

1. **開啟開發者選項**：
   * 進入 Android 裝置的 `設定` -> `關於手機`。
   * 連續點擊 `版本號碼 (Build Number)` 7 次，直到系統提示已開啟開發者模式。
2. **設定模擬定位應用程式**：
   * 返回 `設定` -> `系統` -> `開發者選項`（或直接在設定搜尋「開發者選項」）。
   * 找到 **「選取模擬定位應用程式」 (Select mock location app)**。
   * 點選清單中的 **「MockGPS」**。
3. **啟動模擬**：
   * 開啟 MockGPS App。
   * 允許定位權限與忽略電池優化（建議開啟，避免背景運作時中斷）。
   * 在地圖上選取位置，設定模式後點選 **Start** 即可開始模擬！若要結束請點選 **Stop**。

---

## 🛠️ 開發環境與架構

* **程式語言**：Kotlin
* **UI 框架**：Jetpack Compose (Material Design 3)
* **地圖核心**：OsmDroid (OpenStreetMap)
* **依賴注入/狀態管理**：Kotlin Coroutines Flow, Jetpack ViewModel
* **構建工具**：Gradle Kotlin DSL (Gradle 8.4+)
* **相容性**：支援至 Android 15 (API 35)
