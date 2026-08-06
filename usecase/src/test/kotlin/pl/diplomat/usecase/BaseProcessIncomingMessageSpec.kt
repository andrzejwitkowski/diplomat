package pl.diplomat.usecase

import org.junit.Before
import pl.diplomat.usecase.testsupport.InMemorySystemContactsAdapter

abstract class BaseProcessIncomingMessageSpec : BaseSpec() {

    protected lateinit var systemContacts: InMemorySystemContactsAdapter
        private set

    protected lateinit var useCase: ProcessIncomingMessageUseCase
        private set

    @Before
    fun processIncomingMessageSetUp() {
        systemContacts = InMemorySystemContactsAdapter()
        useCase = ProcessIncomingMessageUseCase(contactRepository, messageRepository, systemContacts)
    }
}
