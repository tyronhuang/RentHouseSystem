package tw.com.baozugong.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import tw.com.baozugong.data.AppDatabase
import tw.com.baozugong.data.AppRepository
import tw.com.baozugong.data.SettingsStore
import tw.com.baozugong.data.displayStatus
import java.time.LocalDate

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val repo = AppRepository(AppDatabase.get(applicationContext))
        repo.generateMissingInvoices()
        val settings = SettingsStore(applicationContext).get()
        val today = LocalDate.now()
        val bills = repo.invoices.first().filter {
            val days = java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.parse(it.dueDate))
            it.displayStatus(today.toString()) == "逾期" || (days in 0..settings.rentReminderDays.toLong() && it.paid < it.total)
        }
        val leases = repo.leases.first().filter { it.status == "ACTIVE" && java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.parse(it.endDate)) in 0..settings.leaseReminderDays.toLong() }
        if (bills.isNotEmpty() || leases.isNotEmpty()) notify("今日有 ${bills.size} 筆租金待處理、${leases.size} 筆租約即將到期")
        Result.success()
    } catch (_: Exception) { Result.retry() }

    private fun notify(message: String) {
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("reminders", "租務提醒", NotificationManager.IMPORTANCE_DEFAULT))
        manager.notify(1001, NotificationCompat.Builder(applicationContext, "reminders").setSmallIcon(tw.com.baozugong.R.drawable.ic_launcher_foreground).setContentTitle("包租公提醒").setContentText(message).setAutoCancel(true).build())
    }
}
