package tw.com.baozugong.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tw.com.baozugong.AppController
import tw.com.baozugong.data.*

@Composable
fun RoomsScreen(vm:AppController,onMessage:(String)->Unit) {
    val venues by vm.venues.collectAsState();val rooms by vm.rooms.collectAsState()
    var venueDialog by remember { mutableStateOf<Venue?>(null) };var showNewVenue by remember { mutableStateOf(false) }
    var roomDialog by remember { mutableStateOf<RoomListRow?>(null) };var showNewRoom by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=24.dp)) {
        item { ScreenTitle("場館與房間","點選項目可編輯",action={Row{TextButton(onClick={showNewVenue=true}){Text("＋場館")};Button(onClick={showNewRoom=true}){Text("＋房間")}}}) }
        venues.forEach { venue ->
            item(key="v${venue.id}") { ListItem(headlineContent={Text(venue.name,fontWeight=FontWeight.Bold)},supportingContent={Text(if(venue.active)"使用中 · ${rooms.count{it.venueId==venue.id}} 間" else "已停用")},trailingContent={TextButton(onClick={venueDialog=venue}){Text("編輯")}});HorizontalDivider() }
            items(rooms.filter{it.venueId==venue.id},key={"r${it.id}"}) { room ->
                ListItem(headlineContent={Text(room.name)},supportingContent={Text("${money(room.defaultRent)} · ${roomStatus(room.status)}${if(room.note.isNotBlank())" · ${room.note}" else ""}")},trailingContent={Text(roomStatus(room.status),color=if(room.status==RoomStatus.RENTED)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)},modifier=Modifier.clickable{roomDialog=room})
            }
        }
    }
    if(showNewVenue) VenueDialog(null,{showNewVenue=false}) { vm.run { saveVenue(it) };showNewVenue=false }
    if(venueDialog!=null) VenueDialog(venueDialog,{venueDialog=null}) { vm.run { saveVenue(it) };venueDialog=null }
    if(showNewRoom) RoomDialog(null,venues,{showNewRoom=false}) { vm.run { saveRoom(it) };showNewRoom=false }
    if(roomDialog!=null) RoomDialog(roomDialog,venues,{roomDialog=null}) { value ->
        val original=roomDialog!!
        if(original.status==RoomStatus.RENTED && value.status!=RoomStatus.RENTED) onMessage("出租中的房間請先從租務頁辦理退租") else { vm.run { saveRoom(value) };roomDialog=null }
    }
}

@Composable private fun VenueDialog(value:Venue?,onDismiss:()->Unit,onSave:(Venue)->Unit) {
    var name by remember { mutableStateOf(value?.name?:"") };var note by remember { mutableStateOf(value?.note?:"") };var active by remember { mutableStateOf(value?.active?:true) }
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(value==null)"新增場館" else "編輯場館")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){FormField(name,{name=it},"場館名稱");FormField(note,{note=it},"備註");Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Switch(active,{active=it});Text("　使用中")}}},dismissButton={TextButton(onClick=onDismiss){Text("取消")}},confirmButton={Button(enabled=name.isNotBlank(),onClick={onSave(Venue(value?.id?:0,name.trim(),active,note.trim()))}){Text("儲存")}})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun RoomDialog(value:RoomListRow?,venues:List<Venue>,onDismiss:()->Unit,onSave:(RentalRoom)->Unit) {
    var name by remember { mutableStateOf(value?.name?:"") };var rent by remember { mutableStateOf(value?.defaultRent?.toString()?:"") };var note by remember { mutableStateOf(value?.note?:"") };var venueId by remember { mutableLongStateOf(value?.venueId?:venues.firstOrNull()?.id?:0) };var status by remember { mutableStateOf(value?.status?:RoomStatus.VACANT) };var expanded by remember{mutableStateOf(false)}
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(value==null)"新增房間" else "編輯房間")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){ExposedDropdownMenuBox(expanded,{expanded=!expanded}){OutlinedTextField(venues.firstOrNull{it.id==venueId}?.name?:"選擇場館",{},readOnly=true,label={Text("場館")},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(expanded)},modifier=Modifier.menuAnchor().fillMaxWidth());ExposedDropdownMenu(expanded,{expanded=false}){venues.filter{it.active}.forEach{DropdownMenuItem({Text(it.name)},{venueId=it.id;expanded=false})}}};FormField(name,{name=it},"房號或名稱");FormField(rent,{rent=it},"預設月租",numeric=true);FormField(note,{note=it},"備註");Text("狀態");Row{listOf(RoomStatus.VACANT to "空房",RoomStatus.DISABLED to "停用").forEach{(s,l)->FilterChip(status==s,{status=s},{Text(l)},Modifier.padding(end=8.dp))}}}},dismissButton={TextButton(onClick=onDismiss){Text("取消")}},confirmButton={Button(enabled=name.isNotBlank()&&venueId>0,onClick={onSave(RentalRoom(value?.id?:0,venueId,name.trim(),rent.asLong(),status,note.trim()))}){Text("儲存")}})
}
