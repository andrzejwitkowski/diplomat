package pl.diplomat.infrastructure.persistence

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE whitelisted_contacts ADD COLUMN avatarUri TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS incoming_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                contactId INTEGER NOT NULL,
                text TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                sourceApp TEXT NOT NULL,
                status TEXT NOT NULL,
                FOREIGN KEY(contactId) REFERENCES whitelisted_contacts(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_incoming_messages_contactId ON incoming_messages(contactId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_incoming_messages_timestamp ON incoming_messages(timestamp)")
    }
}
