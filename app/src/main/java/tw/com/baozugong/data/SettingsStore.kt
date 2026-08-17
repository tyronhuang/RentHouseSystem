package tw.com.baozugong.data

import android.content.Context

data class AppSettings(val landlordName: String = "房東", val rentReminderDays: Int = 3, val leaseReminderDays: Int = 30)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    fun get() = AppSettings(
        prefs.getString("landlord", "房東") ?: "房東",
        prefs.getInt("rentDays", 3),
        prefs.getInt("leaseDays", 30)
    )
    fun save(value: AppSettings) = prefs.edit().putString("landlord", value.landlordName)
        .putInt("rentDays", value.rentReminderDays).putInt("leaseDays", value.leaseReminderDays).apply()
    fun clear() = prefs.edit().clear().apply()
}
