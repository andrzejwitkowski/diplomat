package com.diplomat.ui

import com.diplomat.domain.model.MessageSource
import com.diplomat.domain.model.MessageStatus
import java.text.DateFormat
import java.util.Date

/** Human-readable label for a message status. */
fun MessageStatus.label(): String = when (this) {
    MessageStatus.PENDING_DECISION -> "Awaiting decision"
    MessageStatus.DRAFTING -> "Drafting…"
    MessageStatus.DRAFTED -> "Draft ready"
    MessageStatus.SENT -> "Sent"
    MessageStatus.IGNORED_ACK -> "Ignored (acknowledged)"
    MessageStatus.ERROR -> "Error"
}

/** Short label for the source app. */
fun MessageSource.label(): String = when (this) {
    MessageSource.SMS -> "SMS"
    MessageSource.WHATSAPP -> "WhatsApp"
    MessageSource.UNKNOWN -> "Other"
}

fun formatTimestamp(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(epochMillis))
