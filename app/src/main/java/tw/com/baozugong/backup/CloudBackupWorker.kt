package tw.com.baozugong.backup

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import tw.com.baozugong.data.AndroidDatabaseProvider
import java.time.LocalDateTime

class CloudBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val store = CloudBackupStore(applicationContext)
        val state = store.state()
        val password = store.password()
        if (!state.connected || password == null || !state.autoBackup) return Result.success()
        return runCatching {
            BackupManager(AndroidDatabaseProvider.get(applicationContext)).exportToFolder(
                applicationContext.contentResolver,
                Uri.parse(state.folderUri),
                password,
                automatic = true
            )
            store.markSuccess(LocalDateTime.now().toString())
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { store.markError(it.message ?: "雲端備份失敗"); Result.retry() }
        )
    }
}
