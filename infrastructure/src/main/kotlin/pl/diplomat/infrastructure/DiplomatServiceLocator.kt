package pl.diplomat.infrastructure

import kotlinx.coroutines.CoroutineScope
import pl.diplomat.infrastructure.notification.NotificationParser
import pl.diplomat.usecase.ProcessIncomingMessageResult
import pl.diplomat.usecase.RawIncomingMessage

interface DiplomatServiceLocator {
    val notificationParser: NotificationParser
    val applicationScope: CoroutineScope

    suspend fun processIncomingMessage(raw: RawIncomingMessage): ProcessIncomingMessageResult
}
