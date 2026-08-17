package tw.com.baozugong.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.text.NumberFormat
import java.util.Locale

fun money(value: Long): String = "NT$ ${NumberFormat.getIntegerInstance(Locale.TAIWAN).format(value)}"
fun roomStatus(value:String)=when(value){"VACANT"->"空房";"RENTED"->"出租中";else->"停用"}

@Composable fun ScreenTitle(title:String, subtitle:String?=null, action:(@Composable ()->Unit)?=null) {
    Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);if(subtitle!=null)Text(subtitle,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        if(action!=null) action()
    }
}

@Composable fun EmptyHint(text:String)=Box(Modifier.fillMaxWidth().padding(32.dp)){Text(text,color=MaterialTheme.colorScheme.onSurfaceVariant)}

@Composable fun MetricCard(label:String,value:String,modifier:Modifier=Modifier) {
    Card(modifier){Column(Modifier.padding(14.dp)){Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(4.dp));Text(value,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}}
}

@Composable fun FormField(value:String,onChange:(String)->Unit,label:String,modifier:Modifier=Modifier,numeric:Boolean=false) {
    OutlinedTextField(value,onChange,label={Text(label)},modifier=modifier.fillMaxWidth(),singleLine=true,keyboardOptions=if(numeric) androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Number) else androidx.compose.foundation.text.KeyboardOptions.Default)
}

fun String.asLong():Long=trim().toLongOrNull()?:0

@Composable
fun DatePickerField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val date = remember(value) { runCatching { LocalDate.parse(value) }.getOrElse { LocalDate.now() } }
    OutlinedButton(
        onClick = {
            android.app.DatePickerDialog(context, { _, year, month, day ->
                onChange(LocalDate.of(year, month + 1, day).toString())
            }, date.year, date.monthValue - 1, date.dayOfMonth).show()
        },
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
