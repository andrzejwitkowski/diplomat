package pl.diplomat.infrastructure.persistence

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.MessageContentType
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.MessageStatus
import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.model.WhitelistedContact
import pl.diplomat.domain.model.bodyText
import pl.diplomat.infrastructure.adapter.RoomContactRepositoryAdapter
import pl.diplomat.infrastructure.adapter.RoomMessageRepositoryAdapter

@RunWith(AndroidJUnit4::class)
class RoomMessageRepositoryIntegrationTest {

    private lateinit var database: DiplomatDatabase
    private lateinit var contactRepository: RoomContactRepositoryAdapter
    private lateinit var messageRepository: RoomMessageRepositoryAdapter

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DiplomatDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        contactRepository = RoomContactRepositoryAdapter(database.whitelistedContactDao())
        messageRepository = RoomMessageRepositoryAdapter(
            messageDao = database.incomingMessageDao(),
            contactDao = database.whitelistedContactDao(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveAndObserveActiveConversations() = runTest {
        val contactId = contactRepository.add("Alice", PhoneNumber("+48123456789"))
        val messageId = messageRepository.save(
            IncomingMessage(
                id = 0,
                contactId = contactId,
                content = MessageContent.TextOnly("Hello Diplomat"),
                timestamp = 1_700_000_000_000L,
                sourceApp = MessageSourceApp.SMS,
                status = MessageStatus.PENDING,
            ),
        )

        assertTrue(messageId > 0)

        val conversations = messageRepository.observeActiveConversations().first()
        assertEquals(1, conversations.size)
        assertEquals("Alice", conversations.first().contact.displayName)
        assertEquals("Hello Diplomat", conversations.first().lastMessage.content.bodyText())
    }

    @Test
    fun mapperRoundTripPreservesTextMessage() {
        val domain = IncomingMessage(
            id = 42,
            contactId = 7,
            content = MessageContent.TextOnly("Mapped text"),
            timestamp = 99L,
            sourceApp = MessageSourceApp.WHATSAPP,
            status = MessageStatus.IGNORED_CONFIRMATION,
        )

        val roundTripped = domain.toEntity().toDomain()

        assertEquals(domain, roundTripped)
    }

    @Test
    fun mapperRoundTripPreservesImageMessage() {
        val domain = IncomingMessage(
            id = 43,
            contactId = 7,
            content = MessageContent.ImageOnly,
            timestamp = 100L,
            sourceApp = MessageSourceApp.WHATSAPP,
            status = MessageStatus.PENDING,
        )

        val entity = domain.toEntity()
        assertEquals(MessageContentType.IMAGE.name, entity.contentType)
        assertEquals("", entity.text)
        assertEquals(domain, entity.toDomain())
    }

    @Test
    fun mapperRoundTripPreservesImageWithTextMessage() {
        val domain = IncomingMessage(
            id = 44,
            contactId = 7,
            content = MessageContent.ImageWithText("Sunset"),
            timestamp = 101L,
            sourceApp = MessageSourceApp.SMS,
            status = MessageStatus.PENDING,
        )

        val roundTripped = domain.toEntity().toDomain()

        assertEquals(domain, roundTripped)
    }

    @Test
    fun contactMapperRoundTrip() {
        val domain = WhitelistedContact(
            id = 3,
            displayName = "Bob",
            phoneNumber = PhoneNumber("+48 999 888 777"),
            avatarUri = "file:///avatar.jpg",
        )

        val roundTripped = domain.toEntity().toDomain()

        assertEquals(domain, roundTripped)
    }

    @Test
    fun findMessagesByContactIdReturnsHistory() = runTest {
        val contactId = contactRepository.add("Carol", PhoneNumber("5551234"))
        messageRepository.save(
            IncomingMessage(
                0,
                contactId,
                MessageContent.TextOnly("Older"),
                100L,
                MessageSourceApp.SMS,
                MessageStatus.REPLIED,
            ),
        )
        messageRepository.save(
            IncomingMessage(
                0,
                contactId,
                MessageContent.ImageWithText("Newer"),
                200L,
                MessageSourceApp.SMS,
                MessageStatus.PENDING,
            ),
        )

        val history = messageRepository.findMessagesByContactId(contactId)

        assertEquals(2, history.size)
        assertEquals("Newer", history.first().content.bodyText())
        assertEquals(MessageContentType.IMAGE_WITH_TEXT, history.first().content.type)
    }
}
