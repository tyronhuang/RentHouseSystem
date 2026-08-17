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
fun BillsScreen(vm:AppController,onMessage:(String)->Unit,onShare:(String)->Unit) {
    val all by vm.invoices.collectAsState();var month by remember{mutableStateOf(currentMonth())};var selected by remember{mutableStateOf<InvoiceListRow?>(null)};var venue by remember{mutableStateOf("全部")}
    val venues=listOf("全部")+all.map{it.venueName}.distinct();val rows=all.filter{it.billingMonth==month&&(venue=="全部"||it.venueName==venue)};val today=currentDate()
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=24.dp)) {
        item { ScreenTitle("收租帳簿","帳單、加收項目與收款") }
        item { Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){TextButton(onClick={month=shiftMonth(month,-1)}){Text("‹ 上月")};Text(month,fontWeight=FontWeight.Bold);TextButton(onClick={month=shiftMonth(month,1)}){Text("下月 ›")}} }
        item { Row(Modifier.padding(horizontal=16.dp).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){venues.take(4).forEach{v->FilterChip(venue==v,{venue=v},{Text(v)},Modifier.weight(1f))}} }
        item { val valid=rows.filter{it.rawStatus!=InvoiceStatus.VOID};Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){MetricCard("應收",money(valid.sumOf{it.total}),Modifier.weight(1f));MetricCard("已收",money(valid.sumOf{minOf(it.paid,it.total)}),Modifier.weight(1f));MetricCard("未收",money(valid.sumOf{(it.total-it.paid).coerceAtLeast(0)}),Modifier.weight(1f))} }
        if(rows.isEmpty()) item{EmptyHint("這個月份尚無帳單")}
        items(rows,key={it.id}){bill->
            val status=bill.displayStatus(today)
            Card(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp).clickable{selected=bill}) { Column(Modifier.padding(16.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${bill.venueName} · ${bill.roomName}",fontWeight=FontWeight.Bold);Text(status,color=if(status=="逾期")MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)};Text("${bill.tenantName}　到期 ${bill.dueDate}");Spacer(Modifier.height(6.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("應收 ${money(bill.total)}");Text("已收 ${money(bill.paid)}",fontWeight=FontWeight.Bold)}}
            }
        }
    }
    if(selected!=null) InvoiceDialog(vm,selected!!,{selected=null},onMessage,onShare)
}

@Composable
private fun InvoiceDialog(vm:AppController,bill:InvoiceListRow,onDismiss:()->Unit,onMessage:(String)->Unit,onShare:(String)->Unit) {
    val itemsFlow=remember(bill.id){vm.repository.invoiceItems(bill.id)}
    val paymentsFlow=remember(bill.id){vm.repository.payments(bill.id)}
    val chargeItems by itemsFlow.collectAsState(emptyList())
    val payments by paymentsFlow.collectAsState(emptyList())
    var addCharge by remember{mutableStateOf(false)}
    var addPayment by remember{mutableStateOf(false)}
    var editItem by remember{mutableStateOf<InvoiceItem?>(null)}
    var editPayment by remember{mutableStateOf<Payment?>(null)}
    var cancelPayment by remember{mutableStateOf<Payment?>(null)}
    var confirmVoid by remember{mutableStateOf(false)}
    val total=chargeItems.sumOf{it.amount}
    val paid=payments.filterNot{it.voided}.sumOf{it.amount}
    val remaining=(total-paid).coerceAtLeast(0)

    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("${bill.billingMonth} · ${bill.roomName}")},
        text={LazyColumn(Modifier.heightIn(max=520.dp)){
            item{Text("${bill.venueName}　${bill.tenantName}");Text("到期日 ${bill.dueDate}");Spacer(Modifier.height(12.dp));Text("帳單項目",fontWeight=FontWeight.Bold)}
            items(chargeItems,key={it.id}){item->ListItem(headlineContent={Text(item.title)},trailingContent={Text(money(item.amount))},modifier=Modifier.clickable(enabled=paid==0L&&bill.rawStatus!=InvoiceStatus.VOID){editItem=item})}
            item{HorizontalDivider();Text("收款紀錄",Modifier.padding(top=12.dp),fontWeight=FontWeight.Bold)}
            if(payments.isEmpty()) item{Text("尚無收款",Modifier.padding(vertical=12.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)}
            else items(payments,key={it.id}){payment->
                ListItem(
                    headlineContent={Text("${payment.paidDate} · ${payment.method}")},
                    supportingContent={Text(if(payment.voided) "已取消${payment.voidedAt?.let{" · ${it.take(16).replace('T',' ')}"}?:""}" else payment.note.ifBlank{"點選可更正或取消"})},
                    trailingContent={Column(horizontalAlignment=androidx.compose.ui.Alignment.End){Text(money(payment.amount));if(payment.voided)Text("已取消",color=MaterialTheme.colorScheme.error)}},
                    modifier=Modifier.clickable(enabled=!payment.voided){editPayment=payment},
                )
            }
            item{HorizontalDivider();Row(Modifier.fillMaxWidth().padding(vertical=12.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("尚未收",fontWeight=FontWeight.Bold);Text(money(remaining),fontWeight=FontWeight.Bold,color=if(remaining>0)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)}}
        }},
        dismissButton={TextButton(onClick=onDismiss){Text("關閉")}},
        confirmButton={Column(horizontalAlignment=androidx.compose.ui.Alignment.End){Row{TextButton(enabled=bill.rawStatus!=InvoiceStatus.VOID,onClick={onShare("您好，${bill.billingMonth} ${bill.venueName} ${bill.roomName} 應繳 ${money(total)}，目前尚有 ${money(remaining)} 未繳，繳款期限為 ${bill.dueDate}，再請協助確認，謝謝。")}){Text("分享催繳")};TextButton(enabled=paid==0L&&bill.rawStatus!=InvoiceStatus.VOID,onClick={addCharge=true}){Text("＋費用")}};Row{TextButton(enabled=paid==0L&&bill.rawStatus!=InvoiceStatus.VOID,onClick={confirmVoid=true}){Text("作廢")};Button(enabled=remaining>0&&bill.rawStatus!=InvoiceStatus.VOID,onClick={addPayment=true}){Text("登記收款")}}}},
    )
    if(addCharge) ChargeDialog(null,{addCharge=false}){title,amount->vm.run{addCharge(bill.id,title,amount)};addCharge=false}
    if(editItem!=null) ChargeDialog(editItem,{editItem=null}){title,amount->vm.run{updateCharge(editItem!!.copy(title=title,amount=amount))};editItem=null}
    if(addPayment) PaymentDialog(null,remaining,{addPayment=false},null){amount,date,method,note->vm.run{addPayment(bill.id,amount,date,method,note)};addPayment=false}
    if(editPayment!=null) PaymentDialog(editPayment,remaining+editPayment!!.amount,{editPayment=null},{cancelPayment=editPayment;editPayment=null}){amount,date,method,note->vm.run{updatePayment(editPayment!!.copy(amount=amount,paidDate=date,method=method,note=note))};editPayment=null}
    if(cancelPayment!=null) AlertDialog(onDismissRequest={cancelPayment=null},title={Text("取消這筆收款？")},text={Text("${cancelPayment!!.paidDate} 的 ${money(cancelPayment!!.amount)} 將不再計入已收金額，原紀錄仍會保留並標示為已取消。")},dismissButton={TextButton(onClick={cancelPayment=null}){Text("返回")}},confirmButton={Button(onClick={vm.run{voidPayment(cancelPayment!!)};cancelPayment=null;onMessage("已取消收款")}){Text("確認取消收款")}})
    if(confirmVoid) AlertDialog(onDismissRequest={confirmVoid=false},title={Text("作廢帳單？")},text={Text("帳單不會刪除，將保留為已作廢紀錄。")},dismissButton={TextButton(onClick={confirmVoid=false}){Text("取消")}},confirmButton={Button(onClick={vm.run{voidInvoice(bill.id)};confirmVoid=false;onDismiss()}){Text("確認作廢")}})
}

@Composable private fun ChargeDialog(value:InvoiceItem?,onDismiss:()->Unit,onSave:(String,Long)->Unit){var title by remember{mutableStateOf(value?.title?:"水費")};var amount by remember{mutableStateOf(value?.amount?.toString()?:"")};AlertDialog(onDismissRequest=onDismiss,title={Text(if(value==null)"新增費用" else "調整費用")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){FormField(title,{title=it},"項目（水費、電費等）");FormField(amount,{amount=it},"金額",numeric=true)}},dismissButton={TextButton(onClick=onDismiss){Text("取消")}},confirmButton={Button(enabled=title.isNotBlank()&&amount.asLong()>=0,onClick={onSave(title.trim(),amount.asLong())}){Text("儲存")}})}
@Composable
private fun PaymentDialog(value:Payment?,maximum:Long,onDismiss:()->Unit,onCancel:(()->Unit)?,onSave:(Long,String,String,String)->Unit){
    var amount by remember{mutableStateOf(value?.amount?.toString()?:maximum.toString())}
    var date by remember{mutableStateOf(value?.paidDate?:currentDate())}
    var method by remember{mutableStateOf(value?.method?:"轉帳")}
    var note by remember{mutableStateOf(value?.note?:"")}
    val valid=amount.asLong() in 1..maximum&&isValidDate(date)
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text(if(value==null)"登記收款" else "更正收款")},
        text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){FormField(amount,{amount=it},"金額（最多 ${money(maximum)}）",numeric=true);DatePickerField("收款日",date,{date=it});Text("方式");Row{listOf("轉帳","現金","其他").forEach{FilterChip(method==it,{method=it},{Text(it)},Modifier.padding(end=6.dp))}};FormField(note,{note=it},"備註")}},
        dismissButton={Row{if(onCancel!=null)TextButton(onClick=onCancel){Text("取消這筆收款",color=MaterialTheme.colorScheme.error)};TextButton(onClick=onDismiss){Text("返回")}}},
        confirmButton={Button(enabled=valid,onClick={onSave(amount.asLong(),date,method,note.trim())}){Text("儲存")}},
    )
}
