# Changelog

所有重要變更都記錄於此。格式參考 [Keep a Changelog](https://keepachangelog.com/zh-TW/1.0.0/)。

---

## [v1.1.1] — 2026-08-18

### 修正
- 修正 `minSdk` 誤設為 31（Android 12）導致 Android 11 以下裝置（包含小米 9 等機型）安裝時出現「剖析套件時發生問題」的錯誤。程式碼本身未使用任何 Android 12 專屬 API，故將 `minSdk` 調降為 26（Android 8.0），維持前台定位服務、模擬定位等既有功能不受影響。

---

## [v1.1] — 2026-08-10

### 新增
- 加入正式簽章金鑰（`keystore.properties`，repo 外管理），debug 與 release 共用同一把金鑰，方便 `adb install -r` 互相覆蓋更新。
- 改由 GitHub Releases 發布 APK，不再將編譯後的 `.apk` 提交進 repo。

### 修正
- 修正座標輸入欄位在 Android 15 上會被鍵盤遮住的問題。
- Manifest 補上 `ACCESS_MOCK_LOCATION` 權限，修正未取得模擬定位權限即無法啟動的問題。

### 安全
- 抑制 `ACCESS_MOCK_LOCATION` 的 lint MockLocation 誤報（模擬定位為本 App 核心功能）。

> ⚠️ v1.0 使用臨時 debug 簽章，與 v1.1 起的正式簽章不同，兩者無法互相覆蓋安裝，需先解除安裝舊版。

---

## [v1.0] — 2026-06-26

### 新增
- 首次發布：地圖選點、地點搜尋（Nominatim）、常用/最近地點清單。
- 靜態模式與步行模式（可設定移動半徑與速度）兩種模擬定位模式。
- 前台定位服務（Foreground Service Location）搭配忽略電池優化提示，維持背景模擬穩定運行。
- 以 Room 儲存收藏與最近使用紀錄。
