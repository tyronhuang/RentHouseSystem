package tw.com.baozugong

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import tw.com.baozugong.data.AppRepository
import tw.com.baozugong.data.IosDatabaseProvider
import tw.com.baozugong.ui.BaoZuGongTheme
import tw.com.baozugong.ui.BillsScreen
import tw.com.baozugong.ui.DashboardScreen
import tw.com.baozugong.ui.PeopleScreen
import tw.com.baozugong.ui.RoomsScreen
import tw.com.baozugong.ui.ScreenTitle

fun MainViewController(): UIViewController = ComposeUIViewController {
    val scope = rememberCoroutineScope()
    val controller = remember { AppController(AppRepository(IosDatabaseProvider.create()), scope) }
    var darkTheme by remember { mutableStateOf(false) }
    BaoZuGongTheme(darkTheme) {
        IosApp(controller, darkTheme, { darkTheme = it })
    }
}

private enum class IosTab(val label: String, val symbol: String) {
    HOME("總覽", "⌂"), ROOMS("房源", "屋"), BILLS("收租", "$"), PEOPLE("租務", "約"), SETTINGS("設定", "⚙")
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun IosApp(controller: AppController, darkTheme: Boolean, onThemeChange: (Boolean) -> Unit) {
    var tab by remember { mutableStateOf(IosTab.HOME) }
    var message by remember { mutableStateOf<String?>(null) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("包租公", fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                IosTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Text(item.symbol) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                IosTab.HOME -> DashboardScreen(controller) { tab = IosTab.BILLS }
                IosTab.ROOMS -> RoomsScreen(controller) { message = it }
                IosTab.BILLS -> BillsScreen(controller, { message = it }, ::shareText)
                IosTab.PEOPLE -> PeopleScreen(controller, { message = it }, ::dialPhone)
                IosTab.SETTINGS -> IosSettingsScreen(controller, darkTheme, onThemeChange) { message = it }
            }
        }
    }
    message?.let { value ->
        AlertDialog(
            onDismissRequest = { message = null },
            confirmButton = { TextButton(onClick = { message = null }) { Text("知道了") } },
            text = { Text(value) },
        )
    }
}

@Composable
private fun IosSettingsScreen(
    controller: AppController,
    darkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onMessage: (String) -> Unit,
) {
    var clearStep by remember { mutableIntStateOf(0) }
    val venues by controller.venues.collectAsState()
    val rooms by controller.rooms.collectAsState()
    Column(Modifier.fillMaxSize()) {
        ScreenTitle("設定與資料", "資料只保存在這支 iPhone")
        ListItem(
            headlineContent = { Text(if (darkTheme) "賽博暗黑" else "明亮模式") },
            supportingContent = { Text("Android 與 iOS 共用核心介面與資料規則") },
            trailingContent = { Switch(darkTheme, onThemeChange) },
        )
        ListItem(
            headlineContent = { Text("本機資料") },
            supportingContent = { Text("${venues.size} 個場館 · ${rooms.size} 間房") },
        )
        Text(
            "iPhone 的檔案備份、iCloud／Google Drive 與通知會由 iOS 平台服務接入；共用資料庫格式已完成。",
            Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { clearStep = 1 }) { Text("清除全部資料", color = MaterialTheme.colorScheme.error) }
        }
    }
    if (clearStep > 0) {
        AlertDialog(
            onDismissRequest = { clearStep = 0 },
            title = { Text(if (clearStep == 1) "清除全部資料？" else "最後確認") },
            text = { Text(if (clearStep == 1) "場館、房間、房客、租約、帳單及收款都會被清除。" else "這項操作無法復原。確定要繼續嗎？") },
            dismissButton = { TextButton(onClick = { clearStep = 0 }) { Text("取消") } },
            confirmButton = {
                Button(onClick = {
                    if (clearStep == 1) clearStep = 2 else {
                        controller.run { clearAll() }
                        clearStep = 0
                        onMessage("全部資料已清除")
                    }
                }) { Text(if (clearStep == 1) "我了解，繼續" else "永久清除") }
            },
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun shareText(text: String) {
    val shareController = UIActivityViewController(listOf(text), null)
    UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(shareController, true, null)
}

@OptIn(ExperimentalForeignApi::class)
private fun dialPhone(phone: String) {
    NSURL.URLWithString("tel:$phone")?.let { UIApplication.sharedApplication.openURL(it) }
}
