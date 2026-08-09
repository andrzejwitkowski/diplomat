package pl.diplomat.infrastructure.notification

object WhatsAppNodeMessageExtractor {

    data class NodeTextSnapshot(
        val text: String,
        val className: String? = null,
        val viewId: String? = null,
        val centerX: Int = 0,
        val top: Int = 0,
        val bottom: Int = 0,
        val isEditable: Boolean = false,
    )

    data class MessageCandidate(
        val text: String,
        val isOutgoing: Boolean,
        val occurrence: Int = 0,
        val top: Int = 0,
        val timestampMillis: Long? = null,
        val isMediaOnly: Boolean = false,
    ) {
        fun fingerprint(contactKey: String): String =
            "$contactKey\u0000$text\u0000$isOutgoing\u0000$occurrence\u0000$isMediaOnly"
    }

    fun extractConversationTitle(nodes: List<NodeTextSnapshot>): String? {
        nodes.firstOrNull { node ->
            val id = node.viewId.orEmpty()
            id.endsWith("id/conversation_contact_name") ||
                id.endsWith("id/contact_name") ||
                id.endsWith("id/conversation_contact")
        }?.text?.trim()?.takeIf { it.isNotBlank() }?.let { return it }

        return nodes
            .asSequence()
            .filter { !it.isEditable && it.text.isNotBlank() }
            .filter { it.top in 0..280 }
            .filterNot { WhatsAppSystemTextFilter.isJunk(it.text) }
            .minByOrNull { it.top }
            ?.text
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun extractComposeText(nodes: List<NodeTextSnapshot>): String? =
        nodes.firstOrNull { node ->
            node.isEditable ||
                node.viewId.orEmpty().endsWith("id/entry") ||
                node.className.orEmpty().contains("EditText", ignoreCase = true)
        }?.text?.trim()?.takeIf { it.isNotBlank() }

    fun extractMessages(
        nodes: List<NodeTextSnapshot>,
        screenWidth: Int,
        conversationTitle: String?,
        referenceMillis: Long = System.currentTimeMillis(),
    ): List<MessageCandidate> {
        if (screenWidth <= 0) return emptyList()
        val midX = screenWidth / 2
        val title = conversationTitle?.trim().orEmpty()
        val occurrenceByKey = mutableMapOf<String, Int>()
        val textMessages = nodes
            .asSequence()
            .filter { !it.isEditable }
            .filter { it.text.isNotBlank() }
            .filterNot { WhatsAppSystemTextFilter.isJunk(it.text) }
            .filterNot { title.isNotBlank() && it.text.trim().equals(title, ignoreCase = true) }
            .filter { isMessageTextNode(it) }
            .sortedWith(compareBy({ it.top }, { it.centerX }))
            .map { node ->
                val text = node.text.trim()
                val isOutgoing = node.centerX > midX
                val key = "$text\u0000$isOutgoing"
                val occurrence = occurrenceByKey[key] ?: 0
                occurrenceByKey[key] = occurrence + 1
                MessageCandidate(
                    text = text,
                    isOutgoing = isOutgoing,
                    occurrence = occurrence,
                    top = node.top,
                )
            }
            .toList()

        val coveredTops = textMessages.map { it.top }.toSet()
        val mediaOnly = nodes
            .asSequence()
            .filter { !it.isEditable }
            .filter { isMediaThumbNode(it) }
            .filter { thumb -> coveredTops.none { kotlin.math.abs(it - thumb.top) < 160 } }
            .sortedBy { it.top }
            .map { node ->
                val isOutgoing = node.centerX > midX
                val key = "\u0000media\u0000$isOutgoing\u0000${node.top}"
                val occurrence = occurrenceByKey[key] ?: 0
                occurrenceByKey[key] = occurrence + 1
                MessageCandidate(
                    text = "",
                    isOutgoing = isOutgoing,
                    occurrence = occurrence,
                    top = node.top,
                    isMediaOnly = true,
                )
            }
            .toList()

        val merged = (textMessages + mediaOnly).sortedBy { it.top }
        return WhatsAppBubbleTimeParser.attachBubbleTimestamps(
            candidates = merged,
            nodes = nodes,
            referenceMillis = referenceMillis,
        )
    }

    private fun isMessageTextNode(node: NodeTextSnapshot): Boolean {
        val id = node.viewId.orEmpty()
        return id.endsWith("id/message_text") ||
            id.endsWith("id/conversation_text") ||
            id.contains("message_text", ignoreCase = true)
    }

    private fun isMediaThumbNode(node: NodeTextSnapshot): Boolean {
        val id = node.viewId.orEmpty()
        if (id.endsWith("/thumb") ||
            id.contains("media_thumb", ignoreCase = true) ||
            id.endsWith("/image") ||
            id.contains("/thumbnail", ignoreCase = true)
        ) {
            return true
        }
        val className = node.className.orEmpty()
        return className.contains("ImageView", ignoreCase = true) &&
            node.top > 280 &&
            node.centerX > 0
    }
}
