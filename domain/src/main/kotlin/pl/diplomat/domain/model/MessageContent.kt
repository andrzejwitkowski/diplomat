package pl.diplomat.domain.model

enum class MessageContentType {
    TEXT,
    IMAGE,
    IMAGE_WITH_TEXT,
}

sealed interface MessageContent {
    val type: MessageContentType

    data class TextOnly(val body: String) : MessageContent {
        init {
            require(body.isNotBlank()) { "Text body cannot be blank" }
        }

        override val type = MessageContentType.TEXT
    }

    data object ImageOnly : MessageContent {
        override val type = MessageContentType.IMAGE
    }

    data class ImageWithText(val body: String) : MessageContent {
        init {
            require(body.isNotBlank()) { "Caption cannot be blank" }
        }

        override val type = MessageContentType.IMAGE_WITH_TEXT
    }
}

fun MessageContent.bodyText(): String? = when (this) {
    is MessageContent.TextOnly -> body
    is MessageContent.ImageOnly -> null
    is MessageContent.ImageWithText -> body
}
