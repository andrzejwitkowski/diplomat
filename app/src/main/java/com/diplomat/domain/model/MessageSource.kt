package com.diplomat.domain.model

/**
 * Messaging app a notification was captured from, resolved from its package
 * name. Used for whitelisting and for choosing how to send a reply.
 */
enum class MessageSource(val packageName: String) {
    SMS("com.google.android.apps.messaging"),
    WHATSAPP("com.whatsapp"),
    UNKNOWN("");

    companion object {
        fun fromPackage(pkg: String): MessageSource =
            entries.firstOrNull { it.packageName == pkg } ?: UNKNOWN
    }
}
