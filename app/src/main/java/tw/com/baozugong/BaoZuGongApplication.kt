package tw.com.baozugong

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import tw.com.baozugong.notifications.ReminderWorker
import java.util.concurrent.TimeUnit

class BaoZuGongApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("daily_reminders", ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
