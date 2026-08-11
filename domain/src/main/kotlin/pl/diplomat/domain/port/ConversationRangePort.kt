package pl.diplomat.domain.port

import pl.diplomat.domain.model.ConversationRange
import kotlinx.coroutines.flow.Flow

interface ConversationRangePort {
    fun observe(contactId: Long): Flow<ConversationRange?>
    fun get(contactId: Long): ConversationRange?
    fun set(range: ConversationRange)
    fun clear(contactId: Long)
}
