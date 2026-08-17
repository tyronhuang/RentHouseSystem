package tw.com.baozugong.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction

@Database(entities = [Venue::class, RentalRoom::class, Tenant::class, Lease::class, Invoice::class, InvoiceItem::class, Payment::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "baozugong.db")
                .addCallback(SeedCallback(context.applicationContext)).build().also { instance = it }
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
