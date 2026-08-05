package pl.diplomat.app

import android.app.Application
import androidx.room.Room
import pl.diplomat.infrastructure.adapter.AndroidSystemContactsAdapter
import pl.diplomat.infrastructure.adapter.RoomContactRepositoryAdapter
import pl.diplomat.infrastructure.persistence.DiplomatDatabase
import pl.diplomat.infrastructure.whitelist.WhitelistViewModel
import pl.diplomat.usecase.AddContactToWhitelistUseCase
import pl.diplomat.usecase.GetWhitelistedContactsUseCase
import pl.diplomat.usecase.RemoveContactFromWhitelistUseCase
import pl.diplomat.usecase.UpdateWhitelistedContactUseCase

class DiplomatApplication : Application() {

    lateinit var whitelistViewModel: WhitelistViewModel
        private set

    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(this, DiplomatDatabase::class.java, "diplomat.db").build()
        val repository = RoomContactRepositoryAdapter(database.whitelistedContactDao())
        val systemContacts = AndroidSystemContactsAdapter(contentResolver)

        whitelistViewModel = WhitelistViewModel(
            getWhitelistedContacts = GetWhitelistedContactsUseCase(repository),
            addContact = AddContactToWhitelistUseCase(repository),
            updateContact = UpdateWhitelistedContactUseCase(repository),
            removeContactFromWhitelist = RemoveContactFromWhitelistUseCase(repository),
            systemContacts = systemContacts,
        )
    }
}
