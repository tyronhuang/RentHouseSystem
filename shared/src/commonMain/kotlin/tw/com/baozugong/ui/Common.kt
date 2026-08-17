package tw.com.baozugong.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.*
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import tw.com.baozugong.domain.PhoneRules

fun money(value: Long): String {
    val absolute = if (value == Long.MIN_VALUE) "9223372036854775808" else kotlin.math.abs(value).toString()
    val grouped = absolute.reversed().chunked(3).joinToString(",").reversed()
    return "NT$ ${if (value < 0) "-" else ""}$grouped"
}
fun roomStatus(value:String)=when(value){"VACANT"->"空房";"RENTED"->"出租中";else->"停用"}

fun currentDate(): String = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
fun currentMonth(): String = currentDate().take(7)
fun isValidDate(value: String): Boolean = runCatching { LocalDate.parse(value) }.isSuccess
fun isValidDateRange(start: String, end: String): Boolean = runCatching { LocalDate.parse(start) <= LocalDate.parse(end) }.getOrDefault(false)
fun daysBetween(start: String, end: String): Int = runCatching { LocalDate.parse(end).toEpochDays() - LocalDate.parse(start).toEpochDays() }.getOrDefault(Int.MIN_VALUE)
fun datePlusYears(value: String, years: Int, minusOneDay: Boolean = false): String = runCatching {
    val shifted = LocalDate.parse(value).plus(DatePeriod(years = years))
    (if (minusOneDay) shifted.minus(DatePeriod(days = 1)) else shifted).toString()
}.getOrElse { value }
fun shiftMonth(value: String, delta: Int): String {
    val parts = value.split('-')
    val total = parts[0].toInt() * 12 + parts[1].toInt() - 1 + delta
    val year = total.floorDiv(12)
    val month = total.mod(12) + 1
    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}"
}

@Composable fun ScreenTitle(title:String, subtitle:String?=null, action:(@Composable ()->Unit)?=null) {
    Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);if(subtitle!=null)Text(subtitle,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        if(action!=null) action()
    }
}

@Composable fun EmptyHint(text:String)=Box(Modifier.fillMaxWidth().padding(32.dp)){Text(text,color=MaterialTheme.colorScheme.onSurfaceVariant)}

@Composable fun MetricCard(label:String,value:String,modifier:Modifier=Modifier) {
    Card(modifier,shape=CutCornerShape(topEnd=14.dp,bottomStart=14.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline.copy(alpha=0.7f))){Column(Modifier.padding(14.dp)){Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary);Spacer(Modifier.height(4.dp));Text(value,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}}
}

@Composable fun FormField(value:String,onChange:(String)->Unit,label:String,modifier:Modifier=Modifier,numeric:Boolean=false) {
    OutlinedTextField(value,onChange,label={Text(label)},modifier=modifier.fillMaxWidth(),singleLine=true,keyboardOptions=if(numeric) androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Number) else androidx.compose.foundation.text.KeyboardOptions.Default)
}

fun String.asLong():Long=trim().toLongOrNull()?:0

@Composable
fun MobilePhoneField(tail: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = PhoneRules.formatTail(tail),
        onValueChange = { onChange(PhoneRules.tailFromInput(it)) },
        label = { Text("手機號碼") },
        prefix = { Text("09") },
        supportingText = { Text(if (PhoneRules.isCompleteTail(tail)) "格式：09xx-xxxxxx" else "請輸入後 8 碼") },
        isError = tail.isNotEmpty() && !PhoneRules.isCompleteTail(tail),
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun DatePickerField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    val date = remember(value) { runCatching { LocalDate.parse(value) }.getOrElse { LocalDate.parse(currentDate()) } }
    val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds())
    OutlinedButton(
        onClick = { visible = true },
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
    if (visible) {
        DatePickerDialog(
            onDismissRequest = { visible = false },
            dismissButton = { TextButton(onClick = { visible = false }) { Text("取消") } },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onChange(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date.toString())
                    }
                    visible = false
                }) { Text("確定") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}
