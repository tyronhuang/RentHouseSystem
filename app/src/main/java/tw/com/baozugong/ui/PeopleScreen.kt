package tw.com.baozugong.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tw.com.baozugong.AppViewModel
import tw.com.baozugong.data.*
import java.time.LocalDate

@Composable
fun PeopleScreen(vm:AppViewModel,onMessage:(String)->Unit) {
    val leases by vm.leases.collectAsState();val rooms by vm.rooms.collectAsState();val tenants by vm.tenants.collectAsState();val context=LocalContext.current
    var newLease by remember { mutableStateOf(false) };var editTenant by remember { mutableStateOf<Tenant?>(null) };var editLease by remember { mutableStateOf<LeaseListRow?>(null) };var renew by remember { mutableStateOf<LeaseListRow?>(null) };var confirmEnd by remember { mutableStateOf<LeaseListRow?>(null) }
    var showTenants by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=24.dp)) {
        item { ScreenTitle("租約與房客","入住、續約與退租",action={Button(onClick={newLease=true}){Text("＋辦理入住")}}) }
        item { Row(Modifier.padding(horizontal=16.dp)){FilterChip(!showTenants,{showTenants=false},{Text("租約")},Modifier.padding(end=8.dp));FilterChip(showTenants,{showTenants=true},{Text("房客名冊")})} }
        if(!showTenants) {
            val active=leases.filter{it.status==LeaseStatus.ACTIVE};val history=leases.filter{it.status!=LeaseStatus.ACTIVE}
            item{Text("有效租約（${active.size}）",Modifier.padding(16.dp),fontWeight=FontWeight.Bold)}
            if(active.isEmpty()) item{EmptyHint("目前沒有有效租約，可從「辦理入住」開始")}
            items(active,key={it.id}){lease->LeaseCard(lease,onCall={if(lease.tenantPhone.isNotBlank())context.startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:${lease.tenantPhone}")))},onEdit={editLease=lease},onRenew={renew=lease},onEnd={confirmEnd=lease})}
            if(history.isNotEmpty()){item{Text("歷史租約",Modifier.padding(16.dp),fontWeight=FontWeight.Bold)};items(history,key={it.id}){lease->LeaseCard(lease,null,{editLease=lease},null,null)}}
        } else {
            items(tenants,key={it.id}) { tenant -> ListItem(headlineContent={Text(tenant.name)},supportingContent={Text(listOf(tenant.phone,if(tenant.lineName.isBlank())"" else "LINE ${tenant.lineName}").filter{it.isNotBlank()}.joinToString(" · ").ifBlank{"未填聯絡方式"})},trailingContent={Text("編輯")},modifier=Modifier.clickable{editTenant=tenant});HorizontalDivider() }
        }
    }
    if(newLease) NewLeaseDialog(rooms.filter{it.status==RoomStatus.VACANT},{newLease=false}) { tenant,lease -> vm.run { val tenantId=saveTenant(tenant);createLease(lease.copy(tenantId=tenantId));generateMissingInvoices() };newLease=false }
    if(editTenant!=null) TenantDialog(editTenant!!,{editTenant=null}){vm.run{saveTenant(it)};editTenant=null}
    if(editLease!=null) EditLeaseDialog(editLease!!,{editLease=null}) { lease -> vm.run { updateLease(lease);generateMissingInvoices() };editLease=null;onMessage("租約已更新；既有帳單金額不會自動變更，如有需要請到收租頁調整。") }
    if(renew!=null) RenewDialog(renew!!,{renew=null}){end->vm.run{renewLease(renew!!.id,end);generateMissingInvoices()};renew=null}
    if(confirmEnd!=null) AlertDialog(onDismissRequest={confirmEnd=null},title={Text("辦理退租")},text={Text("確定將 ${confirmEnd!!.roomName} 的租約結束並改為空房？歷史帳單會保留。")},dismissButton={TextButton(onClick={confirmEnd=null}){Text("取消")}},confirmButton={Button(onClick={vm.run{endLease(confirmEnd!!.id)};confirmEnd=null}){Text("確定退租")}})
}

@Composable private fun LeaseCard(value:LeaseListRow,onCall:(()->Unit)?,onEdit:(()->Unit)?,onRenew:(()->Unit)?,onEnd:(()->Unit)?) {
    Card(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp)){Column(Modifier.padding(16.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${value.venueName} · ${value.roomName}",fontWeight=FontWeight.Bold);Text(if(value.status==LeaseStatus.ACTIVE)"有效" else "已退租",color=MaterialTheme.colorScheme.primary)};Text("${value.tenantName}　${value.tenantPhone}");Text("${value.startDate} ～ ${value.endDate}");Text("月租 ${money(value.monthlyRent)}　押金 ${money(value.deposit)}　每月 ${value.dueDay} 日繳");if(onCall!=null||onEdit!=null||onRenew!=null||onEnd!=null)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){if(onCall!=null)TextButton(onClick=onCall){Text("電話")};if(onEdit!=null)TextButton(onClick=onEdit){Text("修改")};if(onRenew!=null)TextButton(onClick=onRenew){Text("續約")};if(onEnd!=null)TextButton(onClick=onEnd){Text("退租")}}}}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun NewLeaseDialog(rooms:List<RoomListRow>,onDismiss:()->Unit,onSave:(Tenant,Lease)->Unit) {
    var roomId by remember{mutableLongStateOf(rooms.firstOrNull()?.id?:0)};var expanded by remember{mutableStateOf(false)};var name by remember{mutableStateOf("")};var phone by remember{mutableStateOf("")};var line by remember{mutableStateOf("")};var start by remember{mutableStateOf(LocalDate.now().toString())};var end by remember{mutableStateOf(LocalDate.now().plusYears(1).minusDays(1).toString())};val selected=rooms.firstOrNull{it.id==roomId};var rent by remember(selected?.id){mutableStateOf((selected?.defaultRent?:0).toString())};var due by remember{mutableStateOf("5")};var deposit by remember{mutableStateOf("")};var note by remember{mutableStateOf("")}
    val valid=roomId>0&&name.isNotBlank()&&rent.asLong()>0&&due.toIntOrNull() in 1..31&&runCatching{LocalDate.parse(start)<=LocalDate.parse(end)}.getOrDefault(false)
    AlertDialog(onDismissRequest=onDismiss,title={Text("辦理入住")},text={Column(Modifier.heightIn(max=560.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){if(rooms.isEmpty())Text("目前沒有空房") else ExposedDropdownMenuBox(expanded,{expanded=!expanded}){OutlinedTextField(selected?.let{"${it.venueName} · ${it.name}"}?:"選擇房間",{},readOnly=true,label={Text("房間")},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(expanded)},modifier=Modifier.menuAnchor().fillMaxWidth());ExposedDropdownMenu(expanded,{expanded=false}){rooms.forEach{r->DropdownMenuItem({Text("${r.venueName} · ${r.name}")},{roomId=r.id;rent=r.defaultRent.toString();expanded=false})}}};FormField(name,{name=it},"房客姓名");FormField(phone,{phone=it},"電話");FormField(line,{line=it},"LINE 顯示名稱");Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){DatePickerField("起租日",start,{start=it},Modifier.weight(1f));DatePickerField("到期日",end,{end=it},Modifier.weight(1f))};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FormField(rent,{rent=it},"月租",Modifier.weight(1f),true);FormField(due,{due=it},"繳租日",Modifier.weight(1f),true)};FormField(deposit,{deposit=it},"押金",numeric=true);FormField(note,{note=it},"租約備註")}},dismissButton={TextButton(onClick=onDismiss){Text("取消")}},confirmButton={Button(enabled=valid,onClick={onSave(Tenant(name=name.trim(),phone=phone.trim(),lineName=line.trim()),Lease(roomId=roomId,tenantId=0,startDate=start,endDate=end,monthlyRent=rent.asLong(),dueDay=due.toInt(),deposit=deposit.asLong(),note=note.trim()))}){Text("建立租約")}})
}

@Composable private fun TenantDialog(value:Tenant,onDismiss:()->Unit,onSave:(Tenant)->Unit){var name by remember{mutableStateOf(value.name)};var phone by remember{mutableStateOf(value.phone)};var line by remember{mutableStateOf(value.lineName)};var note by remember{mutableStateOf(value.note)};AlertDialog(onDismissRequest=onDismiss,title={Text("編輯房客")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){FormField(name,{name=it},"姓名");FormField(phone,{phone=it},"電話");FormField(line,{line=it},"LINE 顯示名稱");FormField(note,{note=it},"備註")}},dismissButton={TextButton(onClick=onDismiss){Text("取消")}},confirmButton={Button(enabled=name.isNotBlank(),onClick={onSave(value.copy(name=name.trim(),phone=phone.trim(),lineName=line.trim(),note=note.trim()))}){Text("儲存")}})}
@Composable private fun RenewDialog(value:LeaseListRow,onDismiss:()->Unit,onSave:(String)->Unit){var end by remember{mutableStateOf(LocalDate.parse(value.endDate).plusYears(1).toString())};val valid=runCatching{LocalDate.parse(end)>LocalDate.parse(value.endDate)}.getOrDefault(false);AlertDialog(onDismissRequest=onDismiss,title={Text("續約 ${value.roomName}")},text={Column{Text("目前到期日：${value.endDate}");Spacer(Modifier.height(8.dp));DatePickerField("新到期日",end,{end=it})}},dismissButton={TextButton(onClick=onDismiss){Text("取消")}},confirmButton={Button(enabled=valid,onClick={onSave(end)}){Text("確認續約")}})}

@Composable private fun EditLeaseDialog(value:LeaseListRow,onDismiss:()->Unit,onSave:(Lease)->Unit){var start by remember{mutableStateOf(value.startDate)};var end by remember{mutableStateOf(value.endDate)};var rent by remember{mutableStateOf(value.monthlyRent.toString())};var due by remember{mutableStateOf(value.dueDay.toString())};var deposit by remember{mutableStateOf(value.deposit.toString())};var note by remember{mutableStateOf(value.note)};val valid=rent.asLong()>0&&due.toIntOrNull() in 1..31&&runCatching{LocalDate.parse(start)<=LocalDate.parse(end)}.getOrDefault(false);AlertDialog(onDismissRequest=onDismiss,title={Text("修改租約 · ${value.roomName}")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text("房客：${value.tenantName}");Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){DatePickerField("起租日",start,{start=it},Modifier.weight(1f));DatePickerField("到期日",end,{end=it},Modifier.weight(1f))};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FormField(rent,{rent=it},"月租",Modifier.weight(1f),true);FormField(due,{due=it},"繳租日",Modifier.weight(1f),true)};FormField(deposit,{deposit=it},"押金",numeric=true);FormField(note,{note=it},"租約備註");Text("修改租約不會自動更動已產生的帳單。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}},dismissButton={TextButton(onClick=onDismiss){Text("取消")}},confirmButton={Button(enabled=valid,onClick={onSave(Lease(value.id,value.roomId,value.tenantId,start,end,rent.asLong(),due.toInt(),deposit.asLong(),note.trim(),value.status,value.endedAt))}){Text("儲存修改")}})}
