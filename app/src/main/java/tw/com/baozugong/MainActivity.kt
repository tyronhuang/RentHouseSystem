package tw.com.baozugong

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tw.com.baozugong.ui.*
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    private val vm by viewModels<AppViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BaoZuGongTheme { MainApp(vm) } }
    }
}

private val Green = Color(0xFF2D5B4E)
private val Cream = Color(0xFFFFF8F0)

@Composable
fun BaoZuGongTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Green, secondary = Color(0xFFF4B860), background = Cream, surface = Color.White, error = Color(0xFFB3261E)), content = content)
}

enum class MainTab(val label: String) { HOME("總覽"), ROOMS("房源"), BILLS("收租"), PEOPLE("租務"), SETTINGS("設定") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainApp(vm: AppViewModel) {
    var tab by remember { mutableStateOf(MainTab.HOME) }
    var message by remember { mutableStateOf<String?>(null) }
    var exportPassword by remember { mutableStateOf<String?>(null) }
    var importUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var importPassword by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<tw.com.baozugong.backup.BackupPreview?>(null) }
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
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) { if (android.os.Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("包租公") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)) },
        bottomBar = {
            NavigationBar(containerColor = Cream) {
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
                    onExportCsv={csvExport.launch("包租公收租報表.csv")}, onMessage={message=it})
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
}

@Composable
private fun PasswordDialog(title:String,onDismiss:()->Unit,onConfirm:(String)->Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={OutlinedTextField(password,{password=it},label={Text("至少 6 個字元")},singleLine=true)},dismissButton={TextButton(onClick=onDismiss){Text("取消")}},confirmButton={Button(enabled=password.length>=6,onClick={onConfirm(password)}){Text("繼續")}})
}
