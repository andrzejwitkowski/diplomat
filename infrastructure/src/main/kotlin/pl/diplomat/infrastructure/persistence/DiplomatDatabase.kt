package pl.diplomat.infrastructure.persistence

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WhitelistedContactEntity::class, IncomingMessageEntity::class],
    version = 6,
    exportSchema = false,
)
abstract class DiplomatDatabase : RoomDatabase() {
    abstract fun whitelistedContactDao(): WhitelistedContactDao
    abstract fun incomingMessageDao(): IncomingMessageDao
}
