# 「包租公」iOS MVP 開發規格與 Codex 接棒文件

文件日期：2026-09-01。讀者：使用者另一台 MacBook 上的 Codex。

## 1. 任務與交接原則

請接續現有 Kotlin Multiplatform（KMP）專案完成可在 iPhone 使用的「包租公」，不要另外重寫一套租務系統。優先讓現有 iOS 外殼成功建置、啟動，再補齊平台功能並驗收。

- GitHub：<https://github.com/tyronhuang/RentHouseSystem.git>
- 本文件盤點基準分支：`feature/kmp-shared-architecture`。
- 程式碼基準 commit：`0f1eaa3`（`feat: allow recorded payments to be cancelled`）；本文件提交會在其後。
- 使用者有一台可使用的 MacBook，已安裝 Codex；Mac 晶片、macOS、Xcode、JDK、iPhone、簽署帳號尚未確認。
- 交接時只確認過 Windows 工作區原始碼，**沒有本次 iOS 編譯成功、模擬器或實機測試通過的證據**。存在 iOS 原始碼不代表已可發布。
- 使用者長期要求：每一批完成的修改須 commit 並 push 到上述 GitHub；提交前檢查 diff，不混入他人或無關修改。
- 不自動合併 `main`、打 tag、建立付費服務或發布 App Store／TestFlight；這些另需使用者指示。
- 保留 Android 功能、資料、遷移與備份相容性，不以刪除資料、卸載 App 或 destructive migration 解決問題。
- 先讀本文件及工作區適用的 `AGENTS.md`，再確認 Git 狀態及最新遠端版本；若現況已變，以實際程式與最新使用者指示為準，更新本文件狀態。

## 2. 產品邊界

### 必須維持

- App 名稱「包租公」；手機主人就是房東，沒有房客端或登入。
- 單機 Room／SQLite，沒有自建網站、API、伺服器、雲端資料庫或自動跨裝置同步。
- 核心租務功能離線可用；雲端檔案提供者是否能同步取決於網路及提供者。
- 繁體中文、新台幣整數金額；業務日期以 `Asia/Taipei` 為目標，日期 `yyyy-MM-dd`、月份 `yyyy-MM`。
- 預建第一場館 8 間、第二場館 4 間，僅新資料庫首次建立時初始化，不覆蓋既有資料。
- 明亮／賽博暗黑可手動切換並保存；延續共用青色、洋紅霓虹視覺，以文字可讀性為先。
- 不保存身分證照片、銀行帳號、卡號；不新增追蹤、廣告、分析 SDK 或自動上傳租客資料。

### 本次不做

iPhone 與 Android 即時同步、多房東協作、SaaS 訂閱、房客登入、銀行對帳、線上支付、LINE 自動催收、電子簽約、維修、抄表計算、稅務或完整會計。

## 3. 已有架構與主要檔案

下列路徑皆相對 repository 根目錄。

| 路徑 | 現況及用途 |
| --- | --- |
| `shared/src/commonMain/kotlin/tw/com/baozugong/data/` | `Models.kt`、`AppDao.kt`、`AppDatabase.kt`、`AppRepository.kt`：共用 Room 資料層與租務操作 |
| `shared/src/commonMain/kotlin/tw/com/baozugong/domain/` | `BillingRules.kt`、`PhoneRules.kt`：月份、繳租日期、續約日期及手機號碼規則 |
| `shared/src/commonMain/kotlin/tw/com/baozugong/AppController.kt` | 畫面狀態、首頁彙總；初始化時補開帳單 |
| `shared/src/commonMain/kotlin/tw/com/baozugong/ui/` | 總覽、房源、收租、租務、日期元件、主題等共用 Compose UI |
| `shared/src/androidMain/` | Android Room 建立器 |
| `shared/src/iosMain/kotlin/tw/com/baozugong/data/IosDatabaseProvider.kt` | iOS Room 建立器，現存於 Documents 下的 `baozugong.db` |
| `shared/src/iosMain/kotlin/tw/com/baozugong/MainViewController.kt` | Compose iOS 入口、五分頁、簡易設定、分享／撥號橋接 |
| `iosApp/iosApp/ContentView.swift` | SwiftUI 包裝 `MainViewControllerKt.MainViewController()` |
| `iosApp/iosApp/iOSApp.swift` | SwiftUI App 入口 |
| `iosApp/iosApp.xcodeproj/project.pbxproj` | Xcode target、framework 連結、Gradle build phase |
| `iosApp/Configuration/Config.xcconfig` | `TEAM_ID`、`BUNDLE_ID`、`APP_NAME` |
| `app/src/main/java/tw/com/baozugong/backup/BackupManager.kt` | Android 備份 JSON、加解密與 CSV 的現有格式依據 |
| `app/src/main/java/tw/com/baozugong/backup/CloudBackupStore.kt`、`CloudBackupWorker.kt` | Android 雲端資料夾與每週備份參考；不能直接搬至 iOS |
| `app/src/main/java/tw/com/baozugong/data/SettingsStore.kt` | Android 設定名稱與預設值參考 |
| `app/src/main/java/tw/com/baozugong/notifications/ReminderWorker.kt` | Android 提醒參考，不代表所有邊界情況已正確 |
| `app/schemas/tw.com.baozugong.data.AppDatabase/` | 共用 Room schema 1、2、3；雖在 app 目錄，仍為 shared 的 schemaDirectory |
| `shared/src/commonTest/kotlin/tw/com/baozugong/domain/` | 已有 `BillingRulesTest.kt`、`PhoneRulesTest.kt`；不是完整 iOS 驗收測試 |

### 鎖定版本快照（不是升級建議）

| 項目 | 現有值 |
| --- | --- |
| Kotlin／Compose compiler plugin | 2.2.10 |
| Compose Multiplatform | 1.9.3 |
| Android Gradle Plugin | 8.13.2 |
| Gradle wrapper | 8.13 |
| KSP | 2.2.10-2.0.2 |
| Room／SQLite bundled driver | 2.8.4／2.6.2 |
| Coroutines／kotlinx-datetime | 1.9.0／0.6.1 |
| JDK | 17 |
| Android compileSdk／minSdk | 35／29 |
| iOS deployment target | 15.0（需實際核對相依套件最低要求） |
| Kotlin iOS targets | `iosArm64`、`iosSimulatorArm64`，沒有 `iosX64` |
| Framework | `BaoZuGongShared`，static framework |
| iOS bundle ID／target | `tw.com.baozugong.ios`／`iosApp` |
| iOS 版本／build | `1.2.0`／`3`（Info.plist） |
| Room schema／JSON backup version | 3／3（兩者是獨立版本概念） |

版本不宜全部直接升到最新。先依 Mac 的 Xcode 檢查 [Kotlin 官方相容表](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)。表中 Kotlin 2.2.0–2.2.10 列出的 Xcode 是 16.3、AGP 範圍至 8.10.0；目前 AGP 8.13.2 超出該列範圍，是待驗證風險，不能宣稱此組合受完整支援。必要的版本調整須一起驗證 Kotlin、Compose、KSP、Room、Gradle 與 Android。

## 4. 現有功能與 iOS 缺口

### 共用程式已存在，須在 iOS 驗證

- 首頁應收／已收／未收／逾期及房間數。
- 場館、房間、房客、新增／修改租約、續約、退租。
- 辦理入住手機號碼固定 `09` 前綴，其餘 8 位數；完整格式 `09xx-xxxxxx`，驗證總共 10 位數。
- 租約日期透過日曆挑選，不能讓到期日早於起租日。
- 押金下方水費、管理費、電費；作為固定月費加入新產生的帳單，不是抄表計價。
- 每月補開帳單、彈性費用、部分／全額收款、更正收款、取消收款、帳單作廢。
- 房間／房客相關紀錄及月份／場館查詢；須逐項比對原 MVP，缺少的篩選或歷史入口不能標示為完成。

### iOS 已有但不完整

- 主題切換僅 `remember`，重開會回明亮；需永久保存。
- 文字分享已使用 `UIActivityViewController`，但使用 `keyWindow` 取 root controller；需改成正確前景 scene／最上層 controller，處理 iPad popover anchor。
- 撥號已有 `tel:` 橋接；須檢查電話正規化、無電話能力及失敗處理，不要求通訊錄權限。
- 清除資料已有兩次確認，但目前啟動 coroutine 後即顯示成功；需等待交易成功才提示，失敗時保留資料並回報。
- Room 建立器已有 schema migration 與 seed，但 connection/controller 的生命週期須確認不重複建立或洩漏。

### iOS 尚未完成

設定保存、備份密碼管理、加密備份／還原、Files 雲端檔案提供者、CSV 匯出、本機通知、App 圖示、發布資產、iOS 自動化／實機驗收。設定畫面目前有「平台服務待接入」提示，完成後才可移除。

## 5. Mac 接手第一輪：取得可重現建置

### 5.1 取得正確版本

新 clone 可執行：

```bash
git clone https://github.com/tyronhuang/RentHouseSystem.git
cd RentHouseSystem
git fetch origin
git switch feature/kmp-shared-architecture
git pull --ff-only
git status --short --branch
git log -5 --oneline
```

若已有 clone，先檢查未提交修改，不覆蓋、不強制切換。確認已取得本文件與 `0f1eaa3` 或含該提交的後續版本；不要直接從可能尚未包含 KMP 的 `main` 開始。開發前可從此基準建立 `feature/ios-mvp`（若遠端已存在則接續該分支），避免兩台電腦同時改同一分支。

### 5.2 環境診斷

```bash
uname -m
sw_vers
xcode-select -p
xcodebuild -version
/usr/libexec/java_home -V
java -version
```

- 確認完整 Xcode、Command Line Tools、必要 iOS simulator runtime 已安裝並完成初次啟動／授權。
- 用 JDK 17 執行 repo 的 `./gradlew`，不要沿用 Windows 機器的 Gradle 9.4.1 路徑。
- `gradlew` Git mode 目前是 `100644`；若 Permission denied，執行 `chmod +x gradlew` 並提交 executable bit 修正。
- Apple Silicon 可使用 `iosSimulatorArm64`。Intel Mac 沒有對應 simulator target；先確認版本支援，再補 `iosX64` 與對應 KSP，或改用實體 iPhone；不得宣稱目前已支援 Intel 模擬器。
- 因 Gradle 同時配置 Android modules，可能需要 Android SDK 35 與本機 `local.properties`。請依實際錯誤補齊 SDK，不刪 Android modules；本機 SDK 路徑不提交。
- Xcode GUI 的 JAVA_HOME 可能與 shell 不同；遇到找不到 Java，修正可攜的 build script／本機環境，不提交個人絕對路徑。

### 5.3 建置順序

```bash
./gradlew --version
./gradlew :shared:tasks --all
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 --stacktrace
xcodebuild -list -project iosApp/iosApp.xcodeproj
xcrun simctl list devices available
open iosApp/iosApp.xcodeproj
```

以上 simulator framework 指令適用 Apple Silicon。Xcode 實際 scheme 請從 `-list` 確認，不假設已有 shared scheme。必要時新增並提交可重現的 shared scheme。

選擇已安裝的 iPhone 模擬器 Build & Run；CLI 範例如下，`SIMULATOR_UDID` 須換成查到的值：

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Debug -destination 'platform=iOS Simulator,id=SIMULATOR_UDID' \
  CODE_SIGNING_ALLOWED=NO build
```

Xcode 現有 build phase 會呼叫 `:shared:embedAndSignAppleFrameworkForXcode`；此 task 依賴 Xcode 注入的環境，不拿它當一般 shell 的獨立建置指令。整合方式見 [Kotlin direct integration](https://kotlinlang.org/docs/multiplatform/multiplatform-direct-integration.html)。

### 5.4 優先排查項目

- `project.pbxproj` 的 `FRAMEWORK_SEARCH_PATHS` 與 `OTHER_LDFLAGS` framework 名稱帶有 `\n`，需核對實際 build settings，必要時去除尾端換行。
- 共用輸出在 `.gradle/build/shared/`，不是標準 `shared/build/`；不能把 framework search path 改到錯誤位置。
- 若 Xcode script sandbox 拒絕 Gradle 存取，依官方整合文件處理該 target 的 User Script Sandboxing；不全面關閉系統安全性。
- 驗證 UIKit／Foundation Kotlin interop、Room KSP constructor、Native link、Compose 日期元件與 Material3 API 相容性。
- App icon asset 的 `images` 目前是空陣列，尚未具備發布圖示。
- `.gitignore` 尚缺完整 Xcode 本機產物規則；接手時補上 DerivedData、xcuserdata、個人簽署設定等，不誤排除共用 scheme。

先保存第一個真正錯誤與完整建置日誌；不能只報最後一行 `BUILD FAILED`。上述是靜態檢查風險，不是已由 Mac 證實的失敗原因。

### 5.5 實機簽署

使用者在 Xcode 登入自己的 Apple ID、選擇 Team，連接並信任 iPhone，按系統要求設定 Developer Mode。可用本機 xcconfig override 或本機 build setting 管理 Team；不得提交密碼、憑證私鑰、provisioning profile 或帳號 token。缺少帳號時先完成 simulator，不替使用者付費或註冊服務。

## 6. 實作要求

### 6.1 共用與平台責任

- 保留 `shared/commonMain` 的租務規則與畫面，不複製到 Swift 成為第二套規則。
- 將可測試的設定模型、提醒計算、備份 DTO／序列化、CSV 格式等抽到共用層；UIKit、通知、Keychain、檔案存取、加解密實作放平台層。
- 以小型 interface／注入 callback 接平台服務；不必為 MVP 建造大型框架。
- 不在 commonMain 引用 Android、`java.*`、`javax.crypto`、`org.json` 或 UIKit。
- 資料異動須有成功／失敗結果，UI 不先報成功；防連點、處理取消、回主畫面重新整理，耗時 IO 不阻塞主執行緒。
- Room bundled driver 請沿用 `useWriterConnection { immediateTransaction { ... } }` 等 KMP transaction API；既有 commit `ac57a44` 修過交易相容問題，不退回 Android 專用交易實作。

### 6.2 設定與本機隱私

- 永久保存房東稱呼（預設「房東」）、租金提前天數（3）、租約到期提前天數（30）、主題（LIGHT／DARK，預設 LIGHT）。非敏感設定可用 `NSUserDefaults`。
- 天數欄位限制為合理非負整數，空白、非法字元須提示。
- 備份密碼若使用者選擇保存，放 Keychain，不放 UserDefaults、log、repo 或明文檔案；也允許每次輸入。
- 變更密碼只影響之後的備份，舊備份仍需舊密碼；忘記密碼無法解密，UI 要說明。
- 清除全部資料必須兩次確認；定義是否連設定、Keychain 與待送通知一起清除，確認畫面明列範圍。本機清除不應刪除已匯出的雲端備份。
- 原產品強調資料不自動離開 App，但 iOS 可能由系統備份 sandbox；接手時檢查 DB、WAL/SHM、暫存檔及系統備份排除屬性，明確區別系統備份與使用者手動匯出，不能直接照搬 Android「沒有網路權限」的說法。

### 6.3 租務與計算一致性

- 維持原始整數金額，禁止負數／非法輸入；押金不自動當成每月租金收入。
- 房客退租、續約保留歷史；修改租約不靜默改寫已有付款的歷史帳單。
- 部分、全額與更正收款須更新彙總；取消收款採 `Payment.voided=true`，保留原金額、日期、方法、備註以及 `updatedAt`、`voidedAt`，不實體刪除。
- 取消收款代表「這筆收款登記無效」，不是退款；帳單仍存在，狀態依剩餘有效付款重算。取消後可以重新登記新收款，避免直接復活原紀錄。
- 帳單作廢代表整張應收失效，不是撤銷付款。須二次確認並清楚區分文字／按鈕；有有效付款時先更正或取消付款，不讓款項憑空消失。
- 所有首頁、列表、報表、提醒排除已取消付款；作廢帳單不計入有效應收。
- 應收按帳單月份計算，「本月已收」是本月帳單對應的有效收款，不等於本月收款日期的現金流。若新增現金流報表要另命名，不能混算。
- 現有唯一鍵為 `(leaseId, billingMonth)`；repository 另檢查同房間同月份有效帳單。須測續約／同月換租／作廢後重開，不能宣稱 DB 已有 `(roomId, month)` 唯一鍵。
- 現有計費是涵蓋月份的完整月租，不按天比例；不要自行加入比例計費。
- 日期與時間注入可測 clock；目前 repository/UI 有使用裝置預設時區，未完全符合台灣時間目標，修正需放共用層並回歸 Android。
- 目前開帳只遍歷 active leases，續約會先結束舊租約；先補舊租約應開月份再退租／續約，或用可測試規則處理未補月份，避免承襲漏帳問題。既有有資料的月份不得重複開。
- App 回到前景、跨日／跨月亦需重新整理，不只依賴 controller 首次初始化。
- 現有 `displayStatus` 對有付款的逾期帳單顯示「部分收款」，逾期提醒要以 `dueDate < today && paid < total && 非作廢` 判斷，不只比較顯示文字。

### 6.4 加密備份及還原（必要）

使用 Files 文件選擇器／匯出流程，支援「我的 iPhone」、iCloud Drive 及裝置上可用的第三方 File Provider。相關入口見 [Apple UIDocumentPickerViewController](https://developer.apple.com/documentation/uikit/uidocumentpickerviewcontroller)。

備份包含七組資料：`venues`、`rooms`、`tenants`、`leases`、`invoices`、`items`、`payments`。須包含已退租、已作廢帳單及取消收款紀錄，不只目前畫面可見資料。現有 Android 格式不含 App 設定／密碼，保持相容時不可假設它們存在。

現有 `.bzg` 二進位格式，以 `BackupManager.kt` 為準：

| 項目 | 格式 |
| --- | --- |
| bytes 0–3 | ASCII `BZG1` |
| bytes 4–19 | 16-byte 隨機 salt |
| bytes 20–31 | 12-byte 隨機 IV／nonce |
| bytes 32 起 | AES-GCM ciphertext，尾端附 16-byte authentication tag |
| 金鑰推導 | PBKDF2-HMAC-SHA256，120,000 iterations，256-bit key |
| 加密 | AES-256-GCM，128-bit tag，無額外 AAD |
| 明文 | UTF-8 JSON，root 含 `version`、`createdAt` 及七組資料 |
| 現有密碼門檻 | 至少 6 字元；跨平台需測非 ASCII 密碼與字元計數差異 |

- 每次匯出產生新的安全隨機 salt／nonce；不要自製加密演算法。CryptoKit/CommonCrypto 或受維護函式庫需先確認 API 及工具鏈支援。
- 原始 Java GCM 輸出為 ciphertext＋tag；Apple API 的 combined representation 不一定等於本格式，請明確拆裝 nonce／ciphertext／tag。
- JSON version 1 缺水／管理／電費時預設 0；version 1、2 缺 `voided` 時預設 false、`voidedAt` 為 null。空字串 nullable 日期依現有格式正規化。
- `BZG1` envelope、JSON version 與 Room schema 是不同層，不因 migration 自動換 header。
- 中文／emoji 密碼須以真實 Android→iOS、iOS→Android fixtures 證明 PBKDF2 編碼一致，不能只做同平台 round trip。
- 匯出用一致交易快照；不要直接複製仍開啟的 SQLite 主檔而漏掉 WAL。
- 還原先解密、驗證版本／型別／ID／外鍵／金額／日期，再顯示日期、版本與七組筆數，使用者再次確認後才整批原子交易覆蓋。
- 密碼錯、檔案截斷、tag 錯、未知新版本、資料不合法或寫入失敗均不得先清空現有 DB；失敗後資料應完整回滾。
- v3 備份的 `createdAt` 是未帶 offset 的 Android 本機日期時間；不要直接當 UTC。新格式若要調整須兼容舊格式並明列遷移。
- 檔案提供者下載／寫入可能失敗或取消，需管理 security-scoped access、協調讀寫與暫存清理。除必要記憶體處理外，不在公開目錄留下明文 JSON。
- 顯示「已匯出／已交由檔案提供者保存」，不要沒有證據就宣稱雲端已同步成功。

### 6.5 Google Drive／iCloud 與自動備份邊界

- iOS 不沿用 Android SAF tree URI，也不假設 Google Drive 必然提供可持久寫入的資料夾。
- MVP 必須先完成使用者主動匯出到 Files。Google Drive 若已安裝登入並在「檔案」啟用位置，就測試其可用匯出流程；未出現時提供操作說明／其他儲存位置，不默默改成本地。
- 不新增 Google OAuth／Drive REST API／CloudKit 同步伺服器。
- 「連結資料夾」只有實測該 provider 支援且可重用 bookmark 時才顯示，處理授權撤銷、bookmark 失效、帳號登出及離線。
- Android 已有每週背景備份，但 iOS 不能承諾相同固定背景執行。第一版以手動雲端匯出為必要；自動備份為後續能力評估，不以假按鈕宣稱完成。
- 如實作自動備份，只能用系統允許的機會式執行／前景補備份；要顯示最近成功時間及失敗原因，不保證準點。此項不阻擋第一輪 iOS MVP 驗收。

### 6.6 本機提醒（必要）

- 透過 `UNUserNotificationCenter` 申請使用者授權並排程本機通知；使用者拒絕後核心功能仍可用，設定頁顯示狀態。[Apple 本機通知文件](https://developer.apple.com/documentation/usernotifications/scheduling-a-notification-locally-from-your-app)
- 租金提前天數預設 3、租約到期提前 30，可修改；逾期有未收餘額即提醒，包含部分收款。
- 建議固定台灣時間上午 09:00，作為 MVP 預設，非必須增加時間設定頁。
- App 開啟／回前景，以及付款、更正、取消付款、作廢、租約修改／退租、還原或提醒設定更新後，重新整理相關排程。
- 使用穩定 identifier；已付清／作廢／退租後取消不適用通知，不重複累積。清除資料後亦清理通知。
- 在系統 pending notification 限制內排有限未來窗口，優先最近事項並可彙總；由已知租約推算未來提醒，不依賴背景定時開帳。
- 不保證 App 長期未開、權限關閉、專注模式等情況仍準時顯示；通知狀態不能替代 DB 的逾期計算。
- 通知只給房東；催繳文字由房東主動分享至 LINE／簡訊，永不自動發給房客。
- 避免鎖定畫面預設暴露房客姓名、電話等細節。

### 6.7 CSV、分享與畫面

- CSV 依使用者選定月份／場館匯出；欄位沿用月份、場館、房間、房客、到期日、應收、已收、狀態。
- UTF-8 BOM；逗號、引號、換行正確 escape。使用者可控文字須考慮 Excel 公式注入防護，匯出處理不得修改 DB 原文。
- 報表說明作廢帳單是否列為歷史，統計必須排除作廢帳單與取消付款。
- 使用原生分享／儲存流程；iPad 分享 popover 正確定位，分享取消不報錯。
- 五分頁沿用「總覽／房源／收租／租務／設定」。日曆、電話數字鍵盤、安全區、鍵盤避讓、大字體、深淺主題、iPad 橫直向皆需測試。
- 有效租約、即將到期、已到期、已退租狀態須清楚；部分收款且逾期不能只顯示已付款造成誤解。
- 移除未完成 placeholder 前須有真實流程與錯誤狀態，不能把無作用按鈕當完成。

## 7. 工作階段與完成定義

1. **P0 建置與啟動**：環境盤點、相容組合、Gradle／Xcode 修正、shared scheme、simulator 冷啟動與 12 間 seed。提交可重現指令與結果。
2. **P1 核心租務**：完整入住→開帳→部分／全額收款→取消→更正→續約／退租；主題與設定保存、日期及生命周期錯誤修正。
3. **P2 資料可攜**：加密備份、預覽／還原、跨平台 fixtures、CSV、Files／可用 Google Drive／iCloud 提供者。
4. **P3 本機提醒與 UI**：權限、排程、取消／更新、實機分享與電話、大小螢幕、失敗提示、App icon。
5. **P4 交付**：實體 iPhone 安裝與驗收、Android 回歸、README／本文件更新、commit/push。TestFlight／上架僅後續選項，等使用者授權。

每個階段完成可獨立驗證的修改後 commit/push；遇到缺少 iPhone／簽署帳號，完成可做的 simulator／測試並明列阻塞，不假造實機結果。

## 8. 驗收案例

全部使用合成測試資料，禁止將真實房客資料、密碼或備份提交到 GitHub。固定測試業務日期，例如 `2026-09-10`，以注入 clock 驗證，不必改使用者手機時鐘。

| 案例 | 必須結果 |
| --- | --- |
| 初次安裝／冷啟動 | 第一場館 8 間、第二場館 4 間；重開不重複 seed、不閃退 |
| 手機欄位 | `09` 固定、8 位後碼可輸入；完整呈現 `0912-345678`；位數錯誤阻止入住 |
| 租約日期／修改 | 日曆選取、前後日期驗證；修改可保存，歷史已付款帳單不被靜默重算 |
| 月費組成 | 租金 10,000＋水 200＋管理 500＋電 300＝應收 11,000；押金不列入 |
| 部分收款 | 收 4,000 後已收 4,000、未收 7,000；重新開啟一致 |
| 全額／取消 | 再收 7,000 後已收 11,000；取消此筆後回到 4,000／7,000，原紀錄與取消時間可查 |
| 取消所有付款 | 已收 0、未收 11,000；若繳租日早於測試日期，逾期金額亦為 11,000 |
| 作廢 | 無有效付款帳單作廢後應收不計入、歷史仍保留，且不是「取消收款」 |
| 跨月補帳 | 8 月至 10 月租約在 9 月打開只開至 9 月；10 月再開補 10 月，多次開啟不重複 |
| 續約／退租 | 房間狀態正確、保留舊租約；漏開月份不遺失、同房同月無重複有效租金 |
| 月底及閏年 | 31 日繳租遇短月以月底處理；2 月、跨年、閏年、台灣午夜跨月正確 |
| 歷史查詢／CSV | 月份、場館、房間、房客對應正確；合計與首頁一致，中文及特殊字元 Excel 可讀 |
| 設定與重啟 | 主題、稱呼、提前天數保存；暗黑／明亮文字按鈕皆可讀 |
| 備份還原 | 匯出含七組資料；清除測試資料後還原，主鍵關聯、帳單項目、取消收款均一致 |
| 失敗安全 | 錯密碼、破損檔、不支援版本、非法關聯、寫入中斷不改目前資料；取消選檔不顯示成功 |
| 跨平台備份 | Android v1/v2/v3→iOS；iOS v3→Android；ASCII、中文／emoji 密碼測試向量均通過 |
| 提醒 | 授權／拒絕、即將到期、部分付款逾期、付清取消、取消付款後重排、還原後重排 |
| 離線 | 所有核心租務、可用本地備份正常；雲端 provider 離線有合理提示、不謊報同步完成 |
| UI／實機 | 小尺寸與大尺寸 iPhone、iPad 分享；鍵盤不擋保存按鈕，日曆與兩種主題正常 |
| Android 回歸 | 共用修改後 Android 編譯、既有 domain 測試、schema/備份／取消收款無退化 |

### 測試指令與記錄

先以 `:shared:tasks --all` 確認 task，再於適用 Mac 執行：

```bash
./gradlew :shared:iosSimulatorArm64Test
./gradlew :shared:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug
```

第一條需 Apple Silicon 與可用模擬器；現有 commonTest 僅涵蓋日期／電話，需補開帳、付款取消、金額統計、備份 codec、資料遷移／原子還原等測試。沒有 test target 就建立適當測試，不把 build success 等同測試通過。

交付時記錄：commit、Mac 架構、macOS、Xcode、JDK、Gradle、simulator/iPhone OS、實際指令、通過／失敗案例、尚未驗證項目與必要截圖。不提交 DerivedData、個人憑證、租客資料、完整含機密 log。

## 9. 完成後的交接輸出

- 更新本文件：哪些已做、哪些仍待驗證；不要只把待辦全部勾完。
- README 增加 Mac 實際可用建置、設定、備份／提醒限制。
- 若引入 SDK／Keychain／平台 API，檢查必要的隱私 manifest、使用說明與權限；不猜測商店審核要求。
- 每批修改執行 `git diff --check`、適用測試並檢閱 diff，精準 add、commit、push 工作分支；push 失敗要明確報告，不宣稱已同步。
- 回覆使用者目前分支、commit、已 push 與否、安裝狀態、驗收結果、下一個需要使用者處理的事項。

## 10. 可貼給 Mac Codex 的啟動訊息

> 請完整閱讀 repository 根目錄的 iOS_spec.md，接手「包租公」iOS MVP。先檢查工作區指示、Git 狀態、分支、Mac 晶片及 Xcode/JDK 環境，從現有 KMP 架構開始，不另寫一套。優先完成 P0：在這台 Mac 成功建置、於 iPhone 模擬器啟動，再依文件逐階段補齊 iOS 功能及驗收。保留 Android、既有資料與跨平台備份相容性；每一批修改都 commit 並 push 到 https://github.com/tyronhuang/RentHouseSystem.git。不要自行合併 main、打 tag 或發布商店；需要 Apple 簽署或實機操作時再告訴我。請如實區分已完成、已測試與尚未驗證的項目。
