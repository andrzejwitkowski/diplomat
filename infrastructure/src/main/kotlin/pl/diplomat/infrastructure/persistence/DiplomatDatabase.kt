package pl.diplomat.infrastructure.persistence

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WhitelistedContactEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class DiplomatDatabase : RoomDatabase() {
    abstract fun whitelistedContactDao(): WhitelistedContactDao
}
