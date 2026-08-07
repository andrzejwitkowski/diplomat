package pl.diplomat.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.testsupport.TestConstants
import pl.diplomat.domain.testsupport.anIncomingMessage
import pl.diplomat.usecase.testsupport.InMemoryContactRepository
import pl.diplomat.usecase.testsupport.InMemoryMessageRepository

class GroupMessagesByChannelTest {

  @Test
  fun groupsMessagesBySourceApp() {
    val sms = anIncomingMessage()
      .withId(1)
      .withSourceApp(MessageSourceApp.SMS)
      .withText("sms message")
      .withTimestamp(TestConstants.TIMESTAMP_100)
      .build()
    val whatsapp = anIncomingMessage()
      .withId(2)
      .withSourceApp(MessageSourceApp.WHATSAPP)
      .withText("wa message")
      .withTimestamp(TestConstants.TIMESTAMP_200)
      .build()

    val groups = groupMessagesByChannel(listOf(sms, whatsapp))

    assertEquals(2, groups.size)
    assertEquals(MessageSourceApp.WHATSAPP, groups[0].sourceApp)
    assertEquals(MessageSourceApp.SMS, groups[1].sourceApp)
  }
}

class MarkConversationAsReadUseCaseTest {

  @Test
  fun marksAllMessagesForContactAsRead() = runTest {
    val contactRepository = InMemoryContactRepository()
    val messageRepository = InMemoryMessageRepository(contactRepository)
    val contactId = contactRepository.add(
      TestConstants.ALICE_NAME,
      pl.diplomat.domain.model.PhoneNumber(TestConstants.ALICE_PHONE_FORMATTED),
    )
    messageRepository.save(
      anIncomingMessage()
        .withContactId(contactId)
        .withText("one")
        .build(),
    )
    messageRepository.save(
      anIncomingMessage()
        .withContactId(contactId)
        .withText("two")
        .build(),
    )

    MarkConversationAsReadUseCase(messageRepository).invoke(contactId)

    val messages = messageRepository.findMessagesByContactId(contactId)
    assertTrue(messages.all { it.isRead })
  }
}
