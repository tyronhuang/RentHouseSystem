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
import tw.com.baozugong.AppViewModel
import tw.com.baozugong.data.InvoiceStatus
import tw.com.baozugong.data.displayStatus
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun DashboardScreen(vm: AppViewModel,onOpenBills:()->Unit) {
    val summary by vm.dashboard.collectAsState();val invoices by vm.invoices.collectAsState();val leases by vm.leases.collectAsState()
    val today=LocalDate.now();val current=invoices.filter { it.billingMonth==YearMonth.now().toString() && it.rawStatus!=InvoiceStatus.VOID && it.paid<it.total }
    val expiring=leases.filter { it.status=="ACTIVE" && runCatching { java.time.temporal.ChronoUnit.DAYS.between(today,LocalDate.parse(it.endDate)) in 0..30 }.getOrDefault(false) }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=24.dp)) {
        item { ScreenTitle("本月總覽",YearMonth.now().toString()) }
        item { Row(Modifier.padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){MetricCard("本月應收",money(summary.expected),Modifier.weight(1f));MetricCard("已收",money(summary.paid),Modifier.weight(1f))} }
        item { Spacer(Modifier.height(10.dp));Row(Modifier.padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){MetricCard("尚未收",money(summary.unpaid),Modifier.weight(1f));MetricCard("逾期",money(summary.overdue),Modifier.weight(1f))} }
        item { Card(Modifier.fillMaxWidth().padding(16.dp)){Row(Modifier.padding(16.dp).fillMaxWidth(),horizontalArrangement=Arrangement.SpaceAround){RoomMetric("出租中",summary.rentedRooms);RoomMetric("空房",summary.vacantRooms);RoomMetric("總房數",summary.totalRooms)}} }
        item { Text("待收與逾期",Modifier.padding(horizontal=16.dp,vertical=8.dp),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold) }
        if(current.isEmpty()) item { EmptyHint("本月沒有待收帳款") } else items(current.take(10),key={it.id}) { bill ->
            ListItem(headlineContent={Text("${bill.venueName} · ${bill.roomName}")},supportingContent={Text("${bill.tenantName}　到期 ${bill.dueDate}")},trailingContent={Column{Text(money((bill.total-bill.paid).coerceAtLeast(0)),fontWeight=FontWeight.Bold);Text(bill.displayStatus(today.toString()),color=if(bill.dueDate<today.toString())MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)}},modifier=Modifier.clickable(onClick=onOpenBills));HorizontalDivider()
        }
        if(expiring.isNotEmpty()) { item { Text("30 天內到期租約",Modifier.padding(16.dp),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold) };items(expiring,key={it.id}){ListItem(headlineContent={Text("${it.venueName} · ${it.roomName}")},supportingContent={Text("${it.tenantName}　${it.endDate} 到期")})} }
    }
}

@Composable private fun RoomMetric(label:String,value:Int)=Column(horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally){Text(value.toString(),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text(label)}
