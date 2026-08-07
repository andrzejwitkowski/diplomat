package pl.diplomat.infrastructure.persistence

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import pl.diplomat.domain.normalization.NormalizationService

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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE incoming_messages ADD COLUMN contentType TEXT NOT NULL DEFAULT 'TEXT'",
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE incoming_messages ADD COLUMN mediaKind TEXT NOT NULL DEFAULT 'PHOTO'",
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE incoming_messages ADD COLUMN notificationKey TEXT")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_incoming_messages_notificationKey " +
                "ON incoming_messages(notificationKey)",
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS index_incoming_messages_notificationKey")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_incoming_messages_notification_dedup " +
                "ON incoming_messages(notificationKey, timestamp, text, contentType, mediaKind)",
        )

        db.execSQL(
            "ALTER TABLE whitelisted_contacts ADD COLUMN normalizedPhoneNumber TEXT NOT NULL DEFAULT ''",
        )
        db.query("SELECT id, phoneNumber FROM whitelisted_contacts").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val phone = cursor.getString(1)
                val normalized = phone.filter { it.isDigit() || it == '+' }
                db.execSQL(
                    "UPDATE whitelisted_contacts SET normalizedPhoneNumber = ? WHERE id = ?",
                    arrayOf(normalized, id),
                )
            }
        }
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_whitelisted_contacts_normalizedPhoneNumber " +
                "ON whitelisted_contacts(normalizedPhoneNumber)",
        )
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE incoming_messages ADD COLUMN isRead INTEGER NOT NULL DEFAULT 1",
        )
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            DELETE FROM incoming_messages
            WHERE notificationKey IS NOT NULL
              AND id NOT IN (
                  SELECT MIN(id)
                  FROM incoming_messages
                  WHERE notificationKey IS NOT NULL
                  GROUP BY notificationKey, text, contentType, mediaKind
              )
            """.trimIndent(),
        )
        db.execSQL(
            """
            DELETE FROM incoming_messages
            WHERE notificationKey IS NULL
              AND id NOT IN (
                  SELECT MIN(id)
                  FROM incoming_messages
                  WHERE notificationKey IS NULL
                  GROUP BY contactId, text, contentType, mediaKind
              )
            """.trimIndent(),
        )
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE whitelisted_contacts ADD COLUMN phoneMatchKey TEXT NOT NULL DEFAULT ''",
        )
        db.query("SELECT id, phoneNumber FROM whitelisted_contacts").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val phone = cursor.getString(1)
                val matchKey = NormalizationService.default.normalizePhone(phone).matchKey
                db.execSQL(
                    "UPDATE whitelisted_contacts SET phoneMatchKey = ? WHERE id = ?",
                    arrayOf(matchKey, id),
                )
            }
        }
        db.execSQL("DROP INDEX IF EXISTS index_whitelisted_contacts_normalizedPhoneNumber")
        db.execSQL(
            """
            DELETE FROM whitelisted_contacts
            WHERE id NOT IN (
                SELECT MIN(id) FROM whitelisted_contacts GROUP BY phoneMatchKey
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_whitelisted_contacts_phoneMatchKey " +
                "ON whitelisted_contacts(phoneMatchKey)",
        )
    }
}
