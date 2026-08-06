package pl.diplomat.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.testsupport.TestConstants
import pl.diplomat.usecase.testsupport.InMemoryContactRepository

class AddContactToWhitelistUseCaseTest {

    private val repository = InMemoryContactRepository()
    private val useCase = AddContactToWhitelistUseCase(repository)

    @Test
    fun `merges duplicate phone numbers into one whitelist entry`() = runTest {
        val firstId = useCase(TestConstants.ALICE_NAME, PhoneNumber(TestConstants.ALICE_PHONE_FORMATTED))
        val secondId = useCase("Alice Updated", PhoneNumber("48123456789"))

        assertEquals(firstId, secondId)
        assertEquals(1, repository.observeAll().first().size)
        assertEquals("Alice Updated", repository.observeAll().first().single().displayName)
    }
}
