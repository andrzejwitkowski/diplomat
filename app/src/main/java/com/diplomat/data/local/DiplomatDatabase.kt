package com.diplomat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.diplomat.infrastructure.persistence.WhitelistedContactDao
import com.diplomat.infrastructure.persistence.WhitelistedContactEntity

@Database(
    entities = [MessageEntity::class, WhitelistedContactEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class DiplomatDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun whitelistedContactDao(): WhitelistedContactDao

    companion object {
        @Volatile
        private var instance: DiplomatDatabase? = null

        fun get(context: Context): DiplomatDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DiplomatDatabase::class.java,
                    "diplomat.db",
                )
                    // ponytail: wipe on schema bump until we ship migrations
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
