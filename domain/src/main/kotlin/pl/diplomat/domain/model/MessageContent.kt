package pl.diplomat.domain.model

enum class MessageContentType {
    TEXT,
    IMAGE,
    IMAGE_WITH_TEXT,
}

enum class VisualMediaKind {
    PHOTO,
    GIF,
    STICKER,
    VIDEO,
}

sealed interface MessageContent {
    val type: MessageContentType

    data class TextOnly(val body: String) : MessageContent {
        init {
            require(body.isNotBlank()) { "Text body cannot be blank" }
        }

        override val type = MessageContentType.TEXT
    }

    data class VisualOnly(val kind: VisualMediaKind) : MessageContent {
        override val type = MessageContentType.IMAGE
    }

    data class VisualWithText(val kind: VisualMediaKind, val body: String) : MessageContent {
        init {
            require(body.isNotBlank()) { "Caption cannot be blank" }
        }

        override val type = MessageContentType.IMAGE_WITH_TEXT
    }
}

fun MessageContent.bodyText(): String? = when (this) {
    is MessageContent.TextOnly -> body
    is MessageContent.VisualOnly -> null
    is MessageContent.VisualWithText -> body
}

fun MessageContent.visualKind(): VisualMediaKind? = when (this) {
    is MessageContent.TextOnly -> null
    is MessageContent.VisualOnly -> kind
    is MessageContent.VisualWithText -> kind
}
