package tw.com.baozugong.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Venue::class, RentalRoom::class, Tenant::class, Lease::class, Invoice::class, InvoiceItem::class, Payment::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "baozugong.db")
                .addMigrations(MIGRATION_1_2)
                .addCallback(SeedCallback(context.applicationContext)).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE leases ADD COLUMN waterFee INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE leases ADD COLUMN managementFee INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE leases ADD COLUMN electricityFee INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}

private class SeedCallback(private val context: Context) : RoomDatabase.Callback() {
    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        super.onCreate(db)
        db.execSQL("INSERT INTO venues(id,name,active,note) VALUES(1,'第一場館',1,''),(2,'第二場館',1,'')")
        for (i in 1..8) db.execSQL("INSERT INTO rooms(venueId,name,defaultRent,status,note) VALUES(1,'房間 $i',0,'VACANT','')")
        for (i in 1..4) db.execSQL("INSERT INTO rooms(venueId,name,defaultRent,status,note) VALUES(2,'房間 $i',0,'VACANT','')")
    }
}
