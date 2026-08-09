package pl.diplomat.infrastructure.notification

object WhatsAppSystemTextFilter {

    private val STANDALONE_CLOCK = Regex("""^\d{1,2}:\d{2}$""")
    private val CALL_DURATION = Regex(
        """^\d+\s*(sek\.?|sec\.?|s|min\.?|minutes?|mins?)$""",
        RegexOption.IGNORE_CASE,
    )
    private val UNREAD_COUNT = Regex(
        """^\d+\s+(nieprzeczytane?\s+wiadomo[sś]ci|unread\s+messages?)$""",
        RegexOption.IGNORE_CASE,
    )
    private val LAST_SEEN = Regex(
        """^(widziano|last\s+seen|ostatnio\s+widziano)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val ACTIVE_STATUS = Regex("""^aktywny\(-a\)$""", RegexOption.IGNORE_CASE)

    private val EXACT_JUNK = setOf(
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
        "wiadomość",
        "search",
        "szukaj",
        "rozmowa głosowa",
        "rozmowa glosowa",
        "voice call",
        "missed voice call",
        "nieodebrana rozmowa głosowa",
        "połączenie wideo",
        "video call",
        "missed video call",
        "nieodebrana rozmowa wideo",
        "wiadomość głosowa",
        "voice message",
        "deleted this message",
        "usunięto tę wiadomość",
        "usunieto te wiadomosc",
        "this message was deleted",
        "forwarded",
        "przekazano",
        "end-to-end encrypted",
        "szyfrowane end-to-end",
    )

    fun isJunk(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return true
        val lower = trimmed.lowercase()
        if (lower in EXACT_JUNK) return true
        if (ACTIVE_STATUS.matches(trimmed)) return true
        if (STANDALONE_CLOCK.matches(trimmed)) return true
        if (CALL_DURATION.matches(trimmed)) return true
        if (UNREAD_COUNT.matches(trimmed)) return true
        if (LAST_SEEN.containsMatchIn(trimmed)) return true
        if (lower.startsWith("widziano dzisiaj o ")) return true
        if (lower.startsWith("last seen today at ")) return true
        return false
    }

    fun isTimestampNode(node: WhatsAppNodeMessageExtractor.NodeTextSnapshot): Boolean {
        val id = node.viewId.orEmpty()
        if (id.endsWith("/date") ||
            id.contains("message_date", ignoreCase = true) ||
            id.contains("timestamp", ignoreCase = true)
        ) {
            return true
        }
        val text = node.text.trim()
        if (!STANDALONE_CLOCK.matches(text)) return false
        val height = (node.bottom - node.top).coerceAtLeast(0)
        return height in 1..36
    }
}
