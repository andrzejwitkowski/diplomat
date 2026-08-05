package pl.diplomat.infrastructure

import pl.diplomat.usecase.ProcessIncomingMessageResult
import pl.diplomat.usecase.RawIncomingMessage

interface DiplomatServiceLocator {
    suspend fun processIncomingMessage(raw: RawIncomingMessage): ProcessIncomingMessageResult
}
