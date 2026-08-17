package tw.com.baozugong

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch
import tw.com.baozugong.ui.*
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    private val vm by viewModels<AppViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var darkTheme by remember { mutableStateOf(vm.settingsStore.get().themeMode == "DARK") }
            BaoZuGongTheme(darkTheme) { MainApp(vm, darkTheme, onThemeChange = { darkTheme = it }) }
        }
    }
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF006C71), onPrimary = Color.White,
    secondary = Color(0xFF6247AA), tertiary = Color(0xFFB32668),
    background = Color(0xFFF3FAFA), surface = Color(0xFFFBFFFF),
    surfaceVariant = Color(0xFFE2F0F1), outline = Color(0xFF6F8B8D),
    error = Color(0xFFB3261E)
)

private val CyberDarkColors = darkColorScheme(
    primary = Color(0xFF00F5D4), onPrimary = Color(0xFF00201B),
    primaryContainer = Color(0xFF004D46), onPrimaryContainer = Color(0xFF76FFE8),
    secondary = Color(0xFFFF4ECD), onSecondary = Color(0xFF3A0030),
    secondaryContainer = Color(0xFF5B164D), onSecondaryContainer = Color(0xFFFFD8F2),
    tertiary = Color(0xFF8C7CFF), onTertiary = Color(0xFF16005E),
    background = Color(0xFF050914), onBackground = Color(0xFFE3F7FF),
    surface = Color(0xFF0B1220), onSurface = Color(0xFFE3F7FF),
    surfaceVariant = Color(0xFF121E32), onSurfaceVariant = Color(0xFFB7C9D9),
    outline = Color(0xFF2C7082), outlineVariant = Color(0xFF1B3E4D),
    error = Color(0xFFFF5C7A), onError = Color(0xFF3F0012)
)

@Composable
fun BaoZuGongTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) CyberDarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = colors.background.toArgb()
        window.navigationBarColor = colors.surface.toArgb()
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(
            headlineSmall = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
            titleLarge = MaterialTheme.typography.titleLarge.copy(letterSpacing = 0.3.sp),
            labelLarge = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.5.sp)
        ),
        shapes = Shapes(
            extraSmall = CutCornerShape(topEnd = 4.dp, bottomStart = 4.dp),
            small = CutCornerShape(topEnd = 7.dp, bottomStart = 7.dp),
            medium = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
            large = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp),
            extraLarge = CutCornerShape(topEnd = 24.dp, bottomStart = 24.dp)
        ),
        content = content
    )
}

enum class MainTab(val label: String) { HOME("總覽"), ROOMS("房源"), BILLS("收租"), PEOPLE("租務"), SETTINGS("設定") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainApp(vm: AppViewModel, darkTheme: Boolean, onThemeChange: (Boolean) -> Unit) {
    var tab by remember { mutableStateOf(MainTab.HOME) }
    var message by remember { mutableStateOf<String?>(null) }
    var exportPassword by remember { mutableStateOf<String?>(null) }
    var importUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var importPassword by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<tw.com.baozugong.backup.BackupPreview?>(null) }
    var cloudState by remember { mutableStateOf(vm.cloudBackup.state()) }
    var pendingCloudFolder by remember { mutableStateOf<android.net.Uri?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val invoices by vm.invoices.collectAsState()

    val backupExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null && exportPassword != null) scope.launch { runCatching { vm.backup.export(context.contentResolver, uri, exportPassword!!) }.onSuccess { message="備份完成" }.onFailure { message=it.message } }
    }
    val csvExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch { runCatching { vm.backup.exportCsv(context.contentResolver, uri, invoices) }.onSuccess { message="CSV 匯出完成" }.onFailure { message=it.message } }
    }
    val backupImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) importUri=uri }
    val cloudFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
                .onSuccess { pendingCloudFolder=uri }
                .onFailure { message="無法取得雲端資料夾存取權：${it.message}" }
        }
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) { if (android.os.Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text("包租公",fontWeight=FontWeight.Black,letterSpacing=2.sp,color=MaterialTheme.colorScheme.primary) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                MainTab.entries.forEach { item -> NavigationBarItem(selected = tab==item, onClick={tab=item}, icon={Text(when(item){MainTab.HOME->"⌂";MainTab.ROOMS->"屋";MainTab.BILLS->"$";MainTab.PEOPLE->"約";MainTab.SETTINGS->"⚙"})}, label={Text(item.label)}) }
            }
        },
        snackbarHost = { SnackbarHost(remember { SnackbarHostState() }) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when(tab) {
                MainTab.HOME -> DashboardScreen(vm, onOpenBills={tab=MainTab.BILLS})
                MainTab.ROOMS -> RoomsScreen(vm, onMessage={message=it})
                MainTab.BILLS -> BillsScreen(vm, onMessage={message=it}, onShare={ text -> context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/plain";putExtra(Intent.EXTRA_TEXT,text) },"分享催繳訊息")) })
                MainTab.PEOPLE -> PeopleScreen(vm, onMessage={message=it})
                MainTab.SETTINGS -> SettingsScreen(vm,
                    onExportBackup={password -> exportPassword=password;backupExport.launch("包租公備份_${LocalDateTime.now().toLocalDate()}.bzg")},
                    onImportBackup={backupImport.launch(arrayOf("application/octet-stream","*/*"))},
                    onExportCsv={csvExport.launch("包租公收租報表.csv")},
                    cloudState=cloudState,
                    onLinkCloud={cloudFolderPicker.launch(null)},
                    onCloudBackupNow={scope.launch {
                        val state=vm.cloudBackup.state();val password=vm.cloudBackup.password()
                        runCatching { require(state.connected&&password!=null);vm.backup.exportToFolder(context.contentResolver,android.net.Uri.parse(state.folderUri),password,false) }
                            .onSuccess { vm.cloudBackup.markSuccess(LocalDateTime.now().toString());cloudState=vm.cloudBackup.state();message="Google Drive 備份完成" }
                            .onFailure { vm.cloudBackup.markError(it.message?:"雲端備份失敗");cloudState=vm.cloudBackup.state();message=it.message?:"雲端備份失敗" }
                    }},
                    onSetCloudAuto={enabled->vm.cloudBackup.setAutoBackup(enabled);cloudState=vm.cloudBackup.state()},
                    onDisconnectCloud={
                        cloudState.folderUri?.let { uri->runCatching{context.contentResolver.releasePersistableUriPermission(android.net.Uri.parse(uri),Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)} }
                        vm.cloudBackup.disconnect();cloudState=vm.cloudBackup.state();message="已解除 Google Drive 連結"
                    },
                    darkTheme=darkTheme,
                    onThemeChange=onThemeChange,
                    onMessage={message=it})
            }
        }
    }

    if (message != null) AlertDialog(onDismissRequest={message=null}, confirmButton={TextButton(onClick={message=null}){Text("知道了")}}, text={Text(message!!)})
    if (importUri != null && importPassword == null) PasswordDialog("輸入備份密碼", onDismiss={importUri=null}, onConfirm={ password ->
        importPassword=password;scope.launch { runCatching { vm.backup.preview(context.contentResolver,importUri!!,password) }.onSuccess { preview=it }.onFailure { message=it.message;importUri=null;importPassword=null } }
    })
    if (preview != null) AlertDialog(onDismissRequest={preview=null;importUri=null;importPassword=null}, title={Text("確認覆蓋目前資料？")}, text={Text("備份時間：${preview!!.createdAt.take(16)}\n場館 ${preview!!.venues}、房間 ${preview!!.rooms}、房客 ${preview!!.tenants}\n租約 ${preview!!.leases}、帳單 ${preview!!.invoices}、收款 ${preview!!.payments}\n\n還原後目前資料無法復原。")}, dismissButton={TextButton(onClick={preview=null;importUri=null;importPassword=null}){Text("取消")}}, confirmButton={Button(onClick={
        val uri=importUri!!;val password=importPassword!!;preview=null
        scope.launch { runCatching { vm.backup.restore(context.contentResolver,uri,password) }.onSuccess { message="還原完成" }.onFailure { message=it.message };importUri=null;importPassword=null }
    }){Text("確定還原")}})
    if (pendingCloudFolder != null) PasswordDialog("設定雲端備份密碼", onDismiss={pendingCloudFolder=null}, onConfirm={password->
        val folder=pendingCloudFolder!!;pendingCloudFolder=null
        scope.launch {
            runCatching {
                vm.cloudBackup.connect(folder,password)
                vm.cloudBackup.setAutoBackup(true)
                vm.backup.exportToFolder(context.contentResolver,folder,password,false)
            }.onSuccess {
                vm.cloudBackup.markSuccess(LocalDateTime.now().toString());cloudState=vm.cloudBackup.state();message="Google Drive 已連結，首次備份完成"
            }.onFailure {
                vm.cloudBackup.markError(it.message?:"雲端備份失敗");cloudState=vm.cloudBackup.state();message=it.message?:"雲端備份失敗"
            }
        }
    })
}

@Composable
private fun PasswordDialog(title:String,onDismiss:()->Unit,onConfirm:(String)->Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={OutlinedTextField(password,{password=it},label={Text("至少 6 個字元")},singleLine=true)},dismissButton={TextButton(onClick=onDismiss){Text("取消")}},confirmButton={Button(enabled=password.length>=6,onClick={onConfirm(password)}){Text("繼續")}})
}
