package tw.com.baozugong.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tw.com.baozugong.AppViewModel
import tw.com.baozugong.data.AppSettings
import tw.com.baozugong.backup.CloudBackupState

@Composable
fun SettingsScreen(vm:AppViewModel,onExportBackup:(String)->Unit,onImportBackup:()->Unit,onExportCsv:()->Unit,cloudState:CloudBackupState,onLinkCloud:()->Unit,onCloudBackupNow:()->Unit,onSetCloudAuto:(Boolean)->Unit,onDisconnectCloud:()->Unit,onMessage:(String)->Unit) {
    var settings by remember{mutableStateOf(vm.settingsStore.get())};var editSettings by remember{mutableStateOf(false)};var passwordDialog by remember{mutableStateOf(false)};var clearStep by remember{mutableIntStateOf(0)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom=24.dp)) {
        ScreenTitle("設定與資料","所有資料只保存在這支手機")
        SettingsSection("房東與提醒") {
            ListItem(headlineContent={Text("房東稱呼")},trailingContent={Text(settings.landlordName)})
            ListItem(headlineContent={Text("租金提前提醒")},trailingContent={Text("${settings.rentReminderDays} 天")})
            ListItem(headlineContent={Text("租約到期提醒")},trailingContent={Text("${settings.leaseReminderDays} 天")})
            TextButton(onClick={editSettings=true},modifier=Modifier.padding(horizontal=12.dp)){Text("編輯設定")}
        }
        SettingsSection("備份與報表") {
            ListItem(headlineContent={Text("加密完整備份")},supportingContent={Text("換機時可完整還原所有資料")},trailingContent={Button(onClick={passwordDialog=true}){Text("匯出")}})
            ListItem(headlineContent={Text("還原完整備份")},supportingContent={Text("會先顯示內容並要求確認")},trailingContent={OutlinedButton(onClick=onImportBackup){Text("匯入")}})
            ListItem(headlineContent={Text("Excel 相容報表")},supportingContent={Text("匯出全部月份收租明細 CSV")},trailingContent={OutlinedButton(onClick=onExportCsv){Text("CSV")}})
        }
        SettingsSection("Google Drive 雲端備份") {
            if (!cloudState.connected) {
                ListItem(headlineContent={Text("尚未連結")},supportingContent={Text("選擇 Google Drive 中的資料夾，只授權該資料夾")})
                Button(onClick=onLinkCloud,modifier=Modifier.fillMaxWidth().padding(16.dp)){Text("連結 Google Drive 資料夾")}
            } else {
                ListItem(headlineContent={Text("已連結 Google Drive")},supportingContent={Text(cloudState.lastBackupAt?.let{"上次成功：${it.take(16).replace('T',' ')}"}?:"尚未完成備份")},trailingContent={Button(onClick=onCloudBackupNow){Text("立即備份")}})
                Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text("每週自動備份");Text("由 Google Drive App 負責同步上傳",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(cloudState.autoBackup,onSetCloudAuto)}
                if(!cloudState.lastError.isNullOrBlank()) Text("上次錯誤：${cloudState.lastError}",Modifier.padding(16.dp),color=MaterialTheme.colorScheme.error)
                Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick=onImportBackup){Text("從雲端檔案還原")};TextButton(onClick=onDisconnectCloud){Text("解除連結",color=MaterialTheme.colorScheme.error)}}
            }
        }
        SettingsSection("關於") {
            ListItem(headlineContent={Text("包租公")},supportingContent={Text("Android 單機版 · 無網路權限")},trailingContent={Text("1.0.0")})
        }
        SettingsSection("危險操作") {
            Text("清除後無法復原，請先匯出完整備份。",Modifier.padding(horizontal=16.dp),color=MaterialTheme.colorScheme.error)
            TextButton(onClick={clearStep=1},Modifier.padding(horizontal=8.dp)){Text("清除全部資料",color=MaterialTheme.colorScheme.error)}
        }
    }
    if(editSettings) SettingsDialog(settings,{editSettings=false}){settings=it;vm.settingsStore.save(it);editSettings=false;onMessage("設定已儲存")}
    if(passwordDialog) ExportPasswordDialog({passwordDialog=false}){passwordDialog=false;onExportBackup(it)}
    if(clearStep==1) AlertDialog(onDismissRequest={clearStep=0},title={Text("清除全部資料？")},text={Text("場館、房間、房客、租約、帳單及收款都會被清除。")},dismissButton={TextButton(onClick={clearStep=0}){Text("取消")}},confirmButton={Button(onClick={clearStep=2}){Text("我了解，繼續")}})
    if(clearStep==2) AlertDialog(onDismissRequest={clearStep=0},title={Text("最後確認")},text={Text("這項操作無法復原。確定已經完成備份嗎？")},dismissButton={TextButton(onClick={clearStep=0}){Text("取消")}},confirmButton={Button(onClick={vm.run{clearAll()};vm.settingsStore.clear();settings=vm.settingsStore.get();clearStep=0;onMessage("全部資料已清除")}){Text("永久清除")}})
}

@Composable private fun SettingsSection(title:String,content:@Composable ColumnScope.()->Unit){Text(title,Modifier.padding(start=16.dp,top=16.dp,bottom=6.dp),style=MaterialTheme.typography.titleMedium);Card(Modifier.fillMaxWidth().padding(horizontal=16.dp)){Column(content=content)}}
@Composable private fun SettingsDialog(value:AppSettings,onDismiss:()->Unit,onSave:(AppSettings)->Unit){var name by remember{mutableStateOf(value.landlordName)};var rent by remember{mutableStateOf(value.rentReminderDays.toString())};var lease by remember{mutableStateOf(value.leaseReminderDays.toString())};val valid=name.isNotBlank()&&rent.toIntOrNull() in 0..30&&lease.toIntOrNull() in 1..365;AlertDialog(onDismissRequest=onDismiss,title={Text("提醒設定")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){FormField(name,{name=it},"房東稱呼");FormField(rent,{rent=it},"繳租日前幾天提醒",numeric=true);FormField(lease,{lease=it},"租約到期前幾天提醒",numeric=true)}},dismissButton={TextButton(onClick=onDismiss){Text("取消")}},confirmButton={Button(enabled=valid,onClick={onSave(AppSettings(name.trim(),rent.toInt(),lease.toInt()))}){Text("儲存")}})}
@Composable private fun ExportPasswordDialog(onDismiss:()->Unit,onSave:(String)->Unit){var password by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")};AlertDialog(onDismissRequest=onDismiss,title={Text("設定本次備份密碼")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text("還原時必須輸入相同密碼，請妥善保存。");FormField(password,{password=it},"備份密碼（至少 6 字元）");FormField(confirm,{confirm=it},"再次輸入密碼")}},dismissButton={TextButton(onClick=onDismiss){Text("取消")}},confirmButton={Button(enabled=password.length>=6&&password==confirm,onClick={onSave(password)}){Text("建立備份")}})}
