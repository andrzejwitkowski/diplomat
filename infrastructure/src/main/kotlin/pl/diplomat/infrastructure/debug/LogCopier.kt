package pl.diplomat.infrastructure.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object LogCopier {
    
    fun copyLogsToClipboard(context: Context, tagFilter: String? = null): Boolean {
        return try {
            val logcatCommand = mutableListOf("logcat", "-d")
            if (tagFilter != null) {
                logcatCommand.addAll(arrayOf("-s", tagFilter))
            }
            logcatCommand.addAll(arrayOf("*:E", "*:W"))
            
            val process = Runtime.getRuntime().exec(logcatCommand.toTypedArray())
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val logBuilder = StringBuilder()
            
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                logBuilder.appendLine(line)
            }
            
            reader.close()
            process.waitFor(10, TimeUnit.SECONDS)
            
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Diplomat Logs", logBuilder.toString())
            clipboard.setPrimaryClip(clip)
            
            true
        } catch (e: Exception) {
            Log.e("LogCopier", "Failed to copy logs", e)
            false
        }
    }
    
    fun copyAllLogsToClipboard(context: Context): Boolean {
        return copyLogsToClipboard(context, null)
    }
}