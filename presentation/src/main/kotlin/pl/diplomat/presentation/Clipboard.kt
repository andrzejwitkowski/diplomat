package pl.diplomat.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

fun copyPlainTextToClipboard(
    context: Context,
    label: String,
    text: String,
    @StringRes copiedMessageRes: Int,
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, context.getString(copiedMessageRes), Toast.LENGTH_SHORT).show()
}
