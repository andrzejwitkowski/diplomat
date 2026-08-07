package pl.diplomat.infrastructure.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pl.diplomat.infrastructure.appinfo.AppBuildInfo

class DevLogTest {

    @Before
    fun clear() {
        DevLog.clear()
    }

    @Test
    fun dumpReturnsPlaceholderWhenEmpty() {
        assertEquals("No debug log entries yet.", DevLog.dump())
    }

    @Test
    fun logAppendsEntriesAndCapsAtMaxEntries() {
        repeat(DevLog.MAX_ENTRIES + 5) { index ->
            DevLog.log("TEST", "entry-$index")
        }
        val dump = DevLog.dump()
        val lines = dump.lines()
        assertEquals(DevLog.MAX_ENTRIES, lines.size)
        assertTrue(lines.first().contains("entry-5"))
        assertTrue(lines.last().contains("entry-${DevLog.MAX_ENTRIES + 4}"))
    }

    @Test
    fun dumpForExportIncludesBuildInfoHeader() {
        DevLog.log("TEST", "hello")
        val exported = DevLog.dumpForExport(
            AppBuildInfo(
                versionName = "1.1.0",
                gitCommitHash = "abc123",
                apkBuiltAt = "2026-08-07T05:00:00Z",
            ),
        )
        assertTrue(exported.contains("version=1.1.0"))
        assertTrue(exported.contains("commit=abc123"))
        assertTrue(exported.contains("[TEST] hello"))
    }
}
