package pl.diplomat.usecase

import org.junit.Before

abstract class BaseGetActiveConversationsSpec : BaseSpec() {

    protected lateinit var useCase: GetActiveConversationsUseCase
        private set

    @Before
    fun getActiveConversationsSetUp() {
        useCase = GetActiveConversationsUseCase(messageRepository)
    }
}
