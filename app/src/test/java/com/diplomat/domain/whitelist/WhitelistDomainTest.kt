package com.diplomat.domain.whitelist

import com.diplomat.usecase.whitelist.AddContactToWhitelistUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberTest {
    @Test fun parsesDigitsAndPlus() = assertEquals("+48123456789", PhoneNumber.parse("+48 123-456-789")?.value)
    @Test fun rejectsShort() = assertNull(PhoneNumber.parse("12"))
    @Test fun rejectsBlank() = assertNull(PhoneNumber.parse("   "))
}

class AddContactToWhitelistUseCaseTest {
    @Test
    fun addsNormalizedContact() = runBlocking {
        val repo = FakeContactRepository()
        val result = AddContactToWhitelistUseCase(repo)("Ada", "+48 600 100 200")
        assertTrue(result.isSuccess)
        assertEquals("+48600100200", result.getOrThrow().phoneNumber.value)
        assertEquals(1, repo.snapshot.size)
    }

    @Test
    fun failsOnInvalidPhone() = runBlocking {
        val result = AddContactToWhitelistUseCase(FakeContactRepository())("x", "ab")
        assertTrue(result.isFailure)
    }
}

private class FakeContactRepository : ContactRepositoryPort {
    val snapshot = mutableListOf<WhitelistedContact>()
    private val flow = MutableStateFlow(snapshot.toList())

    override fun observeAll(): Flow<List<WhitelistedContact>> = flow

    override suspend fun getById(id: Long) = snapshot.find { it.id == id }

    override suspend fun upsert(contact: WhitelistedContact): WhitelistedContact {
        val saved = if (contact.id == 0L) contact.copy(id = snapshot.size + 1L) else contact
        snapshot.removeAll { it.id == saved.id }
        snapshot += saved
        flow.value = snapshot.toList()
        return saved
    }

    override suspend fun delete(id: Long) {
        snapshot.removeAll { it.id == id }
        flow.value = snapshot.toList()
    }
}
