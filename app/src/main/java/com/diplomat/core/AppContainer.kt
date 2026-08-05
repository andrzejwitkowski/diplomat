package com.diplomat.core

import android.content.Context
import com.diplomat.BuildConfig
import com.diplomat.data.local.DiplomatDatabase
import com.diplomat.data.remote.DiplomatApi
import com.diplomat.data.remote.HttpClientFactory
import com.diplomat.data.remote.KtorDiplomatApi
import com.diplomat.data.repository.DiplomatMessageRepository
import com.diplomat.data.repository.MessageRepository
import com.diplomat.domain.whitelist.ContactRepositoryPort
import com.diplomat.infrastructure.contacts.DeviceContactsGateway
import com.diplomat.infrastructure.persistence.RoomContactRepository
import com.diplomat.usecase.whitelist.AddContactToWhitelistUseCase
import com.diplomat.usecase.whitelist.GetWhitelistedContactsUseCase
import com.diplomat.usecase.whitelist.RemoveContactFromWhitelistUseCase
import com.diplomat.usecase.whitelist.UpdateWhitelistedContactUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Manual DI: message capture stack + hexagonal whitelist ports/use cases.
 */
class AppContainer(context: Context) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database: DiplomatDatabase = DiplomatDatabase.get(context)

    private val api: DiplomatApi = KtorDiplomatApi(
        client = HttpClientFactory.create(),
        baseUrl = BuildConfig.ANALYSIS_BASE_URL,
    )

    val messageRepository: MessageRepository =
        DiplomatMessageRepository(database.messageDao(), api)

    val contactRepository: ContactRepositoryPort =
        RoomContactRepository(database.whitelistedContactDao())

    val deviceContacts = DeviceContactsGateway(context.contentResolver)

    val getWhitelistedContacts = GetWhitelistedContactsUseCase(contactRepository)
    val addContactToWhitelist = AddContactToWhitelistUseCase(contactRepository)
    val updateWhitelistedContact = UpdateWhitelistedContactUseCase(contactRepository)
    val removeContactFromWhitelist = RemoveContactFromWhitelistUseCase(contactRepository)

    init {
        contactRepository.observeAll()
            .onEach { contacts ->
                ContactWhitelist.setSenders(
                    contacts.flatMap { listOf(it.phoneNumber.value, it.displayName) },
                )
            }
            .launchIn(appScope)
    }
}
