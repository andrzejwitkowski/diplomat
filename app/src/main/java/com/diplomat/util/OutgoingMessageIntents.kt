package com.diplomat.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.diplomat.domain.model.MessageSource

/**
 * Builds the system intents used to actually dispatch an approved reply. We use
 * the platform's own SMS / WhatsApp UI (an Intent) rather than sending
 * silently, keeping the user in the loop.
 */
object OutgoingMessageIntents {

    fun send(context: Context, source: MessageSource, recipient: String, body: String) {
        when (source) {
            MessageSource.WHATSAPP -> sendWhatsApp(context, recipient, body)
            MessageSource.SMS, MessageSource.UNKNOWN -> sendSms(context, recipient, body)
        }
    }

    /** Opens the default SMS app pre-filled with the reply. */
    fun sendSms(context: Context, address: String, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(address)}")).apply {
            putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Opens WhatsApp's share sheet with the reply text. A phone number is
     * accepted but WhatsApp deep links reliably support prefilled text via the
     * generic send intent.
     */
    fun sendWhatsApp(context: Context, recipient: String, body: String) {
        val uri = Uri.parse("https://wa.me/?text=${Uri.encode(body)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(MessageSource.WHATSAPP.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Fall back to a plain view intent if WhatsApp is not installed.
        val resolved = intent.resolveActivity(context.packageManager)
        context.startActivity(
            if (resolved != null) intent
            else Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
