package tech.hyperjump.hypertrace.streetpass.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import tech.hyperjump.hypertrace.status.persistence.StatusRecord
import tech.hyperjump.hypertrace.status.persistence.StatusRecordDao


@Database(
    entities = arrayOf(StreetPassRecord::class, StatusRecord::class),
    version = 1,
    exportSchema = true
)
abstract class StreetPassRecordDatabase : RoomDatabase() {

    abstract fun recordDao(): StreetPassRecordDao
    abstract fun statusDao(): StatusRecordDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: StreetPassRecordDatabase? = null

        internal fun getDatabase(context: Context): StreetPassRecordDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    StreetPassRecordDatabase::class.java,
                    "record_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
