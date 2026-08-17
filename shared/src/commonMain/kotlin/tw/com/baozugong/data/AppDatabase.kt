package tw.com.baozugong.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [Venue::class, RentalRoom::class, Tenant::class, Lease::class, Invoice::class, InvoiceItem::class, Payment::class],
    version = 2,
    exportSchema = true,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE leases ADD COLUMN waterFee INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE leases ADD COLUMN managementFee INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE leases ADD COLUMN electricityFee INTEGER NOT NULL DEFAULT 0")
    }
}

internal val SEED_CALLBACK = object : RoomDatabase.Callback() {
    override fun onCreate(connection: SQLiteConnection) {
        connection.execSQL("INSERT INTO venues(id,name,active,note) VALUES(1,'第一場館',1,''),(2,'第二場館',1,'')")
        for (index in 1..8) {
            connection.execSQL("INSERT INTO rooms(venueId,name,defaultRent,status,note) VALUES(1,'房間 $index',0,'VACANT','')")
        }
        for (index in 1..4) {
            connection.execSQL("INSERT INTO rooms(venueId,name,defaultRent,status,note) VALUES(2,'房間 $index',0,'VACANT','')")
        }
    }
}
