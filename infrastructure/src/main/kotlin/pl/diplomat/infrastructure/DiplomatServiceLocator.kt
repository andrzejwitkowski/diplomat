package pl.diplomat.infrastructure

import pl.diplomat.infrastructure.notification.NotificationParser
import pl.diplomat.usecase.ProcessIncomingMessageResult
import pl.diplomat.usecase.RawIncomingMessage

interface DiplomatServiceLocator {
    val notificationParser: NotificationParser

    suspend fun processIncomingMessage(raw: RawIncomingMessage): ProcessIncomingMessageResult
}
