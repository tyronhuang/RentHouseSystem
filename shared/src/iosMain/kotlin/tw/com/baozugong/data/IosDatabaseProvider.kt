package tw.com.baozugong.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

object IosDatabaseProvider {
    @OptIn(ExperimentalForeignApi::class)
    fun create(): AppDatabase {
        val documentsDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        ) ?: error("無法取得 iOS 文件目錄")
        val databasePath = documentsDirectory.URLByAppendingPathComponent("baozugong.db")?.path
            ?: error("無法建立 iOS 資料庫路徑")

        return Room.databaseBuilder<AppDatabase>(name = databasePath)
            .addMigrations(MIGRATION_1_2)
            .addCallback(SEED_CALLBACK)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}
