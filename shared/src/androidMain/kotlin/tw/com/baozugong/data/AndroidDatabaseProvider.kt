package tw.com.baozugong.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

object AndroidDatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder<AppDatabase>(
            context = context.applicationContext,
            name = context.getDatabasePath("baozugong.db").absolutePath,
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .addCallback(SEED_CALLBACK)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
            .also { instance = it }
    }
}
