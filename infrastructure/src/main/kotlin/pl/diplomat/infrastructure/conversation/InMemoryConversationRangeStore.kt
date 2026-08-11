package pl.diplomat.infrastructure.conversation

import pl.diplomat.domain.model.ConversationRange
import pl.diplomat.domain.port.ConversationRangePort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// ponytail: Room ConversationRangePort when markers must survive process death
class InMemoryConversationRangeStore : ConversationRangePort {
    private val ranges = MutableStateFlow<Map<Long, ConversationRange>>(emptyMap())

    override fun observe(contactId: Long): Flow<ConversationRange?> =
        ranges.map { it[contactId] }

    override fun get(contactId: Long): ConversationRange? =
        ranges.value[contactId]

    override fun set(range: ConversationRange) {
        ranges.update { it + (range.contactId to range) }
    }

    override fun clear(contactId: Long) {
        ranges.update { it - contactId }
    }
}
