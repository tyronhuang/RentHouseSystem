package tw.com.baozugong.backup

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class CloudBackupState(
    val folderUri: String? = null,
    val autoBackup: Boolean = false,
    val lastBackupAt: String? = null,
    val lastError: String? = null
) {
    val connected: Boolean get() = !folderUri.isNullOrBlank()
}

class CloudBackupStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("cloud_backup", Context.MODE_PRIVATE)

    fun state() = CloudBackupState(
        folderUri = prefs.getString("folder_uri", null),
        autoBackup = prefs.getBoolean("auto_backup", false),
        lastBackupAt = prefs.getString("last_backup_at", null),
        lastError = prefs.getString("last_error", null)
    )

    fun connect(uri: Uri, password: String) {
        savePassword(password)
        prefs.edit().putString("folder_uri", uri.toString()).putString("last_error", null).apply()
    }

    fun setAutoBackup(enabled: Boolean) {
        prefs.edit().putBoolean("auto_backup", enabled).apply()
        schedule(context, enabled)
    }

    fun markSuccess(dateTime: String) = prefs.edit().putString("last_backup_at", dateTime).remove("last_error").apply()
    fun markError(message: String) = prefs.edit().putString("last_error", message).apply()

    fun password(): String? = runCatching {
        val encrypted = Base64.decode(prefs.getString("password", null), Base64.NO_WRAP)
        val iv = Base64.decode(prefs.getString("password_iv", null), Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }.getOrNull()

    fun disconnect() {
        setAutoBackup(false)
        prefs.edit().clear().apply()
    }

    private fun savePassword(password: String) {
        require(password.length >= 6) { "備份密碼至少需要 6 個字元" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(password.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString("password", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString("password_iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
            generateKey()
        }
    }

    companion object {
        private const val KEY_ALIAS = "baozugong_cloud_backup_password"
        private const val WORK_NAME = "weekly_cloud_backup"

        fun schedule(context: Context, enabled: Boolean) {
            val work = WorkManager.getInstance(context)
            if (!enabled) {
                work.cancelUniqueWork(WORK_NAME)
                return
            }
            work.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<CloudBackupWorker>(7, TimeUnit.DAYS).build()
            )
        }
    }
}
