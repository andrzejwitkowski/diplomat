package pl.diplomat.infrastructure.debug

import pl.diplomat.infrastructure.appinfo.AppBuildInfo
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object DevLog {
    internal const val MAX_ENTRIES = 300

    private val lock = Any()
    private val entries = ArrayDeque<String>(MAX_ENTRIES)
    private val timestampFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC)

    fun log(tag: String, message: String) {
        val line = "${timestampFormat.format(Instant.now())} [$tag] $message"
        synchronized(lock) {
            if (entries.size >= MAX_ENTRIES) {
                entries.removeFirst()
            }
            entries.addLast(line)
        }
    }

    fun dump(): String = synchronized(lock) {
        if (entries.isEmpty()) return "No debug log entries yet."
        entries.joinToString("\n")
    }

    fun dumpForExport(buildInfo: AppBuildInfo): String = buildString {
        appendLine("Diplomat debug log")
        appendLine("version=${buildInfo.versionName}")
        appendLine("commit=${buildInfo.gitCommitHash}")
        appendLine("apkBuiltAt=${buildInfo.apkBuiltAt}")
        appendLine("---")
        append(dump())
    }

    fun clear() = synchronized(lock) { entries.clear() }
}
