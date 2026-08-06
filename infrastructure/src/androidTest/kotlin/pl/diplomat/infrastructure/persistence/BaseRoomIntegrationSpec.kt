package pl.diplomat.infrastructure.persistence

import androidx.room.Room
import org.junit.After
import org.junit.Before
import pl.diplomat.infrastructure.BaseSpec
import pl.diplomat.infrastructure.adapter.RoomContactRepositoryAdapter
import pl.diplomat.infrastructure.adapter.RoomMessageRepositoryAdapter

abstract class BaseRoomIntegrationSpec : BaseSpec() {

    protected lateinit var database: DiplomatDatabase
        private set

    protected lateinit var contactRepository: RoomContactRepositoryAdapter
        private set

    protected lateinit var messageRepository: RoomMessageRepositoryAdapter
        private set

    @Before
    fun roomSetUp() {
        database = Room.inMemoryDatabaseBuilder(context, DiplomatDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val contactDao = database.whitelistedContactDao()
        contactRepository = RoomContactRepositoryAdapter(contactDao)
        messageRepository = RoomMessageRepositoryAdapter(
            messageDao = database.incomingMessageDao(),
            contactDao = contactDao,
        )
    }

    @After
    fun roomTearDown() {
        database.close()
    }
}
