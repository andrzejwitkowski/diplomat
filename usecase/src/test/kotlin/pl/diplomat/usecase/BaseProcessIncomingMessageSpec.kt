package pl.diplomat.usecase

import org.junit.Before

abstract class BaseProcessIncomingMessageSpec : BaseSpec() {

    protected lateinit var useCase: ProcessIncomingMessageUseCase
        private set

    @Before
    fun processIncomingMessageSetUp() {
        useCase = ProcessIncomingMessageUseCase(contactRepository, messageRepository)
    }
}
