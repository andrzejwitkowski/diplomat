package pl.diplomat.app

import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import pl.diplomat.infrastructure.DiplomatServiceLocator
import pl.diplomat.infrastructure.appinfo.AppBuildInfo
import pl.diplomat.infrastructure.notification.IncomingMessageNotifier
import pl.diplomat.infrastructure.notification.NotificationParser
import pl.diplomat.infrastructure.notification.VisualPlaceholderCatalog
import pl.diplomat.infrastructure.adapter.AndroidSystemContactsAdapter
import pl.diplomat.infrastructure.adapter.LocalAvatarStorageAdapter
import pl.diplomat.infrastructure.adapter.RoomContactRepositoryAdapter
import pl.diplomat.infrastructure.adapter.RoomMessageRepositoryAdapter
import pl.diplomat.infrastructure.debug.DevLog
import pl.diplomat.infrastructure.dashboard.DashboardViewModel
import pl.diplomat.infrastructure.ota.OtaUpdateManager
import pl.diplomat.infrastructure.ota.OtaUpdateViewModel
import pl.diplomat.infrastructure.sms.SmsInboxObserver
import pl.diplomat.infrastructure.persistence.DiplomatDatabase
import pl.diplomat.infrastructure.persistence.MIGRATION_1_2
import pl.diplomat.infrastructure.persistence.MIGRATION_2_3
import pl.diplomat.infrastructure.persistence.MIGRATION_3_4
import pl.diplomat.infrastructure.persistence.MIGRATION_4_5
import pl.diplomat.infrastructure.persistence.MIGRATION_5_6
import pl.diplomat.infrastructure.persistence.MIGRATION_6_7
import pl.diplomat.infrastructure.persistence.MIGRATION_7_8
import pl.diplomat.infrastructure.persistence.MIGRATION_8_9
import pl.diplomat.infrastructure.persistence.MIGRATION_10_11
import pl.diplomat.infrastructure.persistence.MIGRATION_9_10
import pl.diplomat.domain.normalization.NormalizationService
import pl.diplomat.infrastructure.whitelist.WhitelistViewModel
import pl.diplomat.domain.model.WhitelistedContact
import pl.diplomat.infrastructure.conversation.ConversationDetailViewModel
import pl.diplomat.usecase.MarkConversationAsReadUseCase
import pl.diplomat.usecase.ObserveContactMessagesUseCase
import pl.diplomat.usecase.AddContactToWhitelistUseCase
import pl.diplomat.usecase.GetActiveConversationsUseCase
import pl.diplomat.usecase.GetWhitelistedContactsUseCase
import pl.diplomat.usecase.ProcessIncomingMessageResult
import pl.diplomat.usecase.ProcessIncomingMessageUseCase
import pl.diplomat.usecase.RawIncomingMessage
import pl.diplomat.usecase.RemoveContactFromWhitelistUseCase
import pl.diplomat.usecase.UpdateWhitelistedContactUseCase

class DiplomatApplication : Application(), DiplomatServiceLocator {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val applicationScope: CoroutineScope
        get() = appScope

    override lateinit var notificationParser: NotificationParser
        private set

    override lateinit var incomingMessageNotifier: IncomingMessageNotifier
        private set

    val buildInfo: AppBuildInfo by lazy {
        AppBuildInfo(
            versionName = BuildConfig.VERSION_NAME,
            gitCommitHash = BuildConfig.GIT_COMMIT_HASH,
            apkBuiltAt = BuildConfig.APK_BUILT_AT,
        )
    }

    lateinit var dashboardViewModel: DashboardViewModel
        private set

    lateinit var otaUpdateViewModel: OtaUpdateViewModel
        private set

    lateinit var whitelistViewModel: WhitelistViewModel
        private set

    lateinit var conversationDetailViewModelFactory: (WhitelistedContact) -> ConversationDetailViewModel
        private set

    private lateinit var smsInboxObserver: SmsInboxObserver

    private lateinit var processIncomingMessage: ProcessIncomingMessageUseCase

    fun startSmsInboxObserver() {
        smsInboxObserver.start()
    }

    override fun onCreate() {
        super.onCreate()
        notificationParser = NotificationParser(VisualPlaceholderCatalog.fromContext(this))
        IncomingMessageNotifier.ensureChannel(this)
        incomingMessageNotifier = IncomingMessageNotifier(this)
        DevLog.log("APP", "started version=${buildInfo.versionName} commit=${buildInfo.gitCommitHash}")

        val normalization = NormalizationService.default

        val database = Room.databaseBuilder(this, DiplomatDatabase::class.java, "diplomat.db")
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
            )
            .build()

        val contactRepository = RoomContactRepositoryAdapter(database.whitelistedContactDao(), normalization)
        val messageRepository = RoomMessageRepositoryAdapter(
            messageDao = database.incomingMessageDao(),
            contactDao = database.whitelistedContactDao(),
        )

        val systemContacts = AndroidSystemContactsAdapter(contentResolver, normalization)
        val avatarStorage = LocalAvatarStorageAdapter(this)

        processIncomingMessage = ProcessIncomingMessageUseCase(
            contactRepository,
            messageRepository,
            systemContacts,
        )

        smsInboxObserver = SmsInboxObserver(this, appScope, processIncomingMessage::invoke)
        smsInboxObserver.start()

        val observeContactMessages = ObserveContactMessagesUseCase(messageRepository)
        val markConversationAsRead = MarkConversationAsReadUseCase(messageRepository)

        dashboardViewModel = DashboardViewModel(
            getActiveConversations = GetActiveConversationsUseCase(messageRepository),
            buildInfo = buildInfo,
        )

        otaUpdateViewModel = OtaUpdateViewModel(OtaUpdateManager(this))

        conversationDetailViewModelFactory = { contact ->
            ConversationDetailViewModel(
                contact = contact,
                observeContactMessages = observeContactMessages,
                markConversationAsRead = markConversationAsRead,
            )
        }

        whitelistViewModel = WhitelistViewModel(
            getWhitelistedContacts = GetWhitelistedContactsUseCase(contactRepository),
            addContact = AddContactToWhitelistUseCase(contactRepository, normalization),
            updateContact = UpdateWhitelistedContactUseCase(contactRepository),
            removeContactFromWhitelist = RemoveContactFromWhitelistUseCase(contactRepository),
            systemContacts = systemContacts,
            avatarStorage = avatarStorage,
            onWhitelistChanged = smsInboxObserver::resyncToday,
        )
    }

    override suspend fun processIncomingMessage(raw: RawIncomingMessage): ProcessIncomingMessageResult =
        processIncomingMessage.invoke(raw)
}
