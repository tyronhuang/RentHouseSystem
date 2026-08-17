# 包租公

「包租公」是提供小型房東使用的 Android 單機租務管理 App。資料只保存在手機的 Room／SQLite 資料庫，不需帳號、網站、伺服器或網路權限。

## MVP 功能

- 本月應收、已收、未收、逾期與房間狀態總覽
- 場館、房間、房客與租約管理
- 預建第一場館 8 間、第二場館 4 間房
- 跨月份自動補建租金帳單，唯一鍵避免重複開帳
- 水電、管理費、停車費等彈性帳單項目
- 全額／部分收款、更正紀錄與帳單作廢
- 租金及租約到期本機提醒
- 透過 Android 分享選單傳送催繳文字
- CSV 收租報表
- PBKDF2＋AES-GCM 加密完整備份及換機還原
- 透過 Android Storage Access Framework 連結 Google Drive 資料夾，支援立即與每週自動加密備份
- 可即時切換並保存明亮模式或霓虹青／洋紅配色的賽博暗黑模式

## 技術

- Kotlin、Jetpack Compose
- Room／SQLite
- WorkManager
- Android Storage Access Framework
- 最低 Android 10（API 29）

## 建置

1. 使用 Android Studio 開啟專案，確認 Android SDK 35 已安裝。
2. 使用 JDK 17 執行 Gradle。
3. 執行 `testDebugUnitTest assembleDebug`。

Debug APK 會產生於 `app/build/outputs/apk/debug/app-debug.apk`。

## 隱私

App 未宣告網路權限。Google Drive 備份透過使用者明確授權的 Android 文件提供者資料夾寫入，實際雲端同步由 Google Drive App 處理；除此之外，資料只有在使用者主動匯出備份／CSV 或分享催繳文字時才會離開 App。
