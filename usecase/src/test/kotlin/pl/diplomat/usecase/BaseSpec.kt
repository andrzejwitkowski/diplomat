package pl.diplomat.usecase

import org.junit.Before
import pl.diplomat.usecase.testsupport.InMemoryContactRepository
import pl.diplomat.usecase.testsupport.InMemoryMessageRepository

abstract class BaseSpec {

    protected lateinit var contactRepository: InMemoryContactRepository
        private set

    protected lateinit var messageRepository: InMemoryMessageRepository
        private set

    @Before
    fun baseSetUp() {
        contactRepository = InMemoryContactRepository()
        messageRepository = InMemoryMessageRepository(contactRepository)
    }
}
