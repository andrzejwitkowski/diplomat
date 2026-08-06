package pl.diplomat.app

import android.app.Application
import androidx.room.Room
import pl.diplomat.infrastructure.DiplomatServiceLocator
import pl.diplomat.infrastructure.adapter.AndroidSystemContactsAdapter
import pl.diplomat.infrastructure.adapter.LocalAvatarStorageAdapter
import pl.diplomat.infrastructure.adapter.RoomContactRepositoryAdapter
import pl.diplomat.infrastructure.adapter.RoomMessageRepositoryAdapter
import pl.diplomat.infrastructure.dashboard.DashboardViewModel
import pl.diplomat.infrastructure.persistence.DiplomatDatabase
import pl.diplomat.infrastructure.persistence.MIGRATION_1_2
import pl.diplomat.infrastructure.persistence.MIGRATION_2_3
import pl.diplomat.infrastructure.persistence.MIGRATION_3_4
import pl.diplomat.infrastructure.persistence.MIGRATION_4_5
import pl.diplomat.infrastructure.whitelist.WhitelistViewModel
import pl.diplomat.usecase.AddContactToWhitelistUseCase
import pl.diplomat.usecase.GetActiveConversationsUseCase
import pl.diplomat.usecase.GetWhitelistedContactsUseCase
import pl.diplomat.usecase.ProcessIncomingMessageResult
import pl.diplomat.usecase.ProcessIncomingMessageUseCase
import pl.diplomat.usecase.RawIncomingMessage
import pl.diplomat.usecase.RemoveContactFromWhitelistUseCase
import pl.diplomat.usecase.UpdateWhitelistedContactUseCase

class DiplomatApplication : Application(), DiplomatServiceLocator {

    lateinit var dashboardViewModel: DashboardViewModel
        private set

    lateinit var whitelistViewModel: WhitelistViewModel
        private set

    private lateinit var processIncomingMessage: ProcessIncomingMessageUseCase

    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(this, DiplomatDatabase::class.java, "diplomat.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()

        val contactRepository = RoomContactRepositoryAdapter(database.whitelistedContactDao())
        val messageRepository = RoomMessageRepositoryAdapter(
            messageDao = database.incomingMessageDao(),
            contactDao = database.whitelistedContactDao(),
        )

        val systemContacts = AndroidSystemContactsAdapter(contentResolver)
        val avatarStorage = LocalAvatarStorageAdapter(this)

        processIncomingMessage = ProcessIncomingMessageUseCase(contactRepository, messageRepository)

        dashboardViewModel = DashboardViewModel(
            getActiveConversations = GetActiveConversationsUseCase(messageRepository),
        )

        whitelistViewModel = WhitelistViewModel(
            getWhitelistedContacts = GetWhitelistedContactsUseCase(contactRepository),
            addContact = AddContactToWhitelistUseCase(contactRepository),
            updateContact = UpdateWhitelistedContactUseCase(contactRepository),
            removeContactFromWhitelist = RemoveContactFromWhitelistUseCase(contactRepository),
            systemContacts = systemContacts,
            avatarStorage = avatarStorage,
        )
    }

    override suspend fun processIncomingMessage(raw: RawIncomingMessage): ProcessIncomingMessageResult =
        processIncomingMessage.invoke(raw)
}
