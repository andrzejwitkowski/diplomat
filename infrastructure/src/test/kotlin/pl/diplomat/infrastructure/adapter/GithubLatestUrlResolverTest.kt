package pl.diplomat.infrastructure.adapter

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class GithubLatestUrlResolverTest {

    private val resolver = GithubLatestUrlResolver("owner", "repo")

    private fun apk(name: String) = name to "https://github.com/owner/repo/releases/download/v1.2.10/$name"

    @Test
    fun resolvesApkDownloadUrl() {
        assertEquals(
            "https://github.com/owner/repo/releases/download/v1.2.10/app-debug.apk",
            resolver.parseStaticDownloadUrl(listOf(apk("app-debug.apk"))),
        )
    }

    @Test
    fun selectsArtifactAmongMixedAssets() {
        assertEquals(
            "https://github.com/owner/repo/releases/download/v1.2.10/app-debug.apk",
            resolver.parseStaticDownloadUrl(
                listOf(
                    "checksums" to "https://github.com/owner/repo/releases/download/v1.2.10/checksums",
                    apk("app-debug.apk"),
                    "notes.md" to "https://github.com/owner/repo/releases/download/v1.2.10/notes.md",
                ),
            ),
        )
    }

    @Test
    fun acceptsZipArtifact() {
        assertEquals(
            "https://github.com/owner/repo/releases/download/v1.2.10/release.zip",
            resolver.parseStaticDownloadUrl(listOf(apk("release.zip"))),
        )
    }

    @Test
    fun failsWhenNoArtifact() {
        try {
            resolver.parseStaticDownloadUrl(listOf("checksums.txt" to "https://example.com/checksums.txt"))
            fail("Expected error when no APK/ZIP asset exists")
        } catch (expected: Exception) {
            assertEquals("Latest release has no downloadable APK/ZIP artifact", expected.message)
        }
    }

    @Test
    fun failsWhenAssetsEmpty() {
        try {
            resolver.parseStaticDownloadUrl(emptyList())
            fail("Expected error when assets array is empty")
        } catch (expected: Exception) {
            assertEquals("Latest release has no downloadable APK/ZIP artifact", expected.message)
        }
    }
}
