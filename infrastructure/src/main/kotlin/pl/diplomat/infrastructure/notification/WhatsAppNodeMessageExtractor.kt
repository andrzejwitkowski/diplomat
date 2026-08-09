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
    ) {
        fun fingerprint(contactKey: String): String =
            "$contactKey\u0000$text\u0000$isOutgoing\u0000$occurrence"
    }

    private val CHROME = setOf(
        "whatsapp",
        "online",
        "offline",
        "typing…",
        "typing...",
        "pisze…",
        "pisze...",
        "last seen",
        "ostatnio widziano",
        "today",
        "yesterday",
        "dzisiaj",
        "wczoraj",
        "message",
        "type a message",
        "wpisz wiadomość",
        "search",
        "szukaj",
    )

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
            .filterNot { isChrome(it.text) }
            .filterNot { looksLikeMessageBody(it) }
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
    ): List<MessageCandidate> {
        if (screenWidth <= 0) return emptyList()
        val midX = screenWidth / 2
        val title = conversationTitle?.trim().orEmpty()
        val occurrenceByKey = mutableMapOf<String, Int>()

        return nodes
            .asSequence()
            .filter { !it.isEditable }
            .filter { it.text.isNotBlank() }
            .filterNot { isChrome(it.text) }
            .filterNot { title.isNotBlank() && it.text.trim().equals(title, ignoreCase = true) }
            .filter { isLikelyMessageNode(it) }
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
                )
            }
            .toList()
    }

    private fun isLikelyMessageNode(node: NodeTextSnapshot): Boolean {
        val id = node.viewId.orEmpty()
        if (id.endsWith("id/message_text") ||
            id.endsWith("id/conversation_text") ||
            id.contains("message_text", ignoreCase = true)
        ) {
            return true
        }
        if (id.endsWith("id/conversation_contact_name") ||
            id.endsWith("id/contact_name") ||
            id.endsWith("id/entry")
        ) {
            return false
        }
        val className = node.className.orEmpty()
        if (className.isNotBlank() &&
            !className.contains("TextView", ignoreCase = true) &&
            !className.contains("AppCompatTextView", ignoreCase = true)
        ) {
            return false
        }
        return looksLikeMessageBody(node)
    }

    private fun looksLikeMessageBody(node: NodeTextSnapshot): Boolean {
        val text = node.text.trim()
        if (text.isEmpty()) return false
        if (text.length <= 2 && text.all { it.isDigit() || it == ':' }) return false
        val height = (node.bottom - node.top).coerceAtLeast(0)
        if (height in 1..28 && text.length <= 8) return false
        return true
    }

    private fun isChrome(text: String): Boolean =
        text.trim().lowercase() in CHROME
}
