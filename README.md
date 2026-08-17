# 包租公

「包租公」是提供小型房東使用的 Android／iOS 單機租務管理 App。專案採 Kotlin Multiplatform 與 Compose Multiplatform 架構，資料只保存在裝置的 Room／SQLite 資料庫，不需帳號、網站或伺服器。

## MVP 功能

- 本月應收、已收、未收、逾期與房間狀態總覽
- 場館、房間、房客與租約管理
- 預建第一場館 8 間、第二場館 4 間房
- 跨月份自動補建租金帳單，唯一鍵避免重複開帳
- 水電、管理費、停車費等彈性帳單項目
- 全額／部分收款、更正與取消收款（保留取消紀錄），以及帳單作廢
- 租金及租約到期本機提醒
- 透過平台原生分享選單傳送催繳文字
- CSV 收租報表
- PBKDF2＋AES-GCM 加密完整備份及換機還原
- 透過 Android Storage Access Framework 連結 Google Drive 資料夾，支援立即與每週自動加密備份
- 可即時切換並保存明亮模式或霓虹青／洋紅配色的賽博暗黑模式

## 雙平台架構

- `shared/commonMain`：商業規則、Room entities／DAO／repository、畫面狀態與主要 Compose UI。
- `shared/androidMain`：Android Room 資料庫建立器，沿用既有 `baozugong.db` 與 migration。
- `shared/iosMain`：iOS Room 資料庫建立器與 Compose `UIViewController` 入口。
- `app`：Android 原生外殼，負責通知、檔案選擇器、Google Drive 資料夾、備份加密與系統分享。
- `iosApp`：SwiftUI／Xcode 外殼；iOS 的檔案備份與通知服務預留在此層接入。

共用層使用 Kotlin 2.2、Compose Multiplatform、Room KMP、SQLite bundled driver 與 Coroutines。Android 最低版本為 Android 10（API 29），iOS deployment target 為 iOS 15。

## 建置

1. 使用 Android Studio 開啟專案，確認 Android SDK 35 已安裝。
2. 使用 JDK 17 執行 Gradle。
3. 執行 `./gradlew :shared:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug`。

Debug APK 會產生於 `.gradle/build/app/outputs/apk/debug/app-debug.apk`。

### iOS

1. 在 macOS 安裝 Xcode 與 JDK 17。
2. 開啟 `iosApp/iosApp.xcodeproj`。
3. 在 `iosApp/Configuration/Config.xcconfig` 填入 Apple Developer Team ID。
4. 選擇模擬器或已簽署的 iPhone 後執行；Xcode 會呼叫 Gradle 建立並嵌入 `BaoZuGongShared` framework。

Windows 無法執行 Kotlin/Native iOS linker 或 Xcode 簽署，因此 iOS 最終封裝必須在 macOS 驗證。

## 隱私

Android App 未宣告網路權限。Google Drive 備份透過使用者明確授權的 Android 文件提供者資料夾寫入，實際雲端同步由 Google Drive App 處理；除此之外，資料只有在使用者主動匯出備份／CSV 或分享催繳文字時才會離開 App。iOS 版本目前先提供本機資料與核心租務功能，檔案備份與通知將由 iOS 平台層接續實作。
