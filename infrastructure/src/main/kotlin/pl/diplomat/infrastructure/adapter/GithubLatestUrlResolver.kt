package pl.diplomat.infrastructure.adapter

import org.json.JSONObject
import pl.diplomat.domain.port.LatestReleaseUrlResolver
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resolves the static download URL of the artifact attached to the newest GitHub release
 * via `GET https://api.github.com/repos/{owner}/{repo}/releases/latest`.
 *
 * The returned URL is stable for a given release tag, so "latest" always points at the
 * most recently published build without requiring a manually pasted URL. Mirrors
 * [pl.diplomat.infrastructure.ota.OtaUpdateManager] in its plain [HttpURLConnection] + org.json
 * approach.
 */
class GithubLatestUrlResolver(
    private val owner: String,
    private val repo: String,
) : LatestReleaseUrlResolver {

    override fun resolveLatestUrl(): Result<String> = runCatching {
        parseStaticDownloadUrl(fetchAssets())
    }

    /**
     * Extracts the static `browser_download_url` of the first `.apk`/`.zip` asset.
     * Accepts already-parsed `(name, url)` pairs so it can be unit-tested without `org.json`.
     */
    internal fun parseStaticDownloadUrl(assets: List<Pair<String, String>>): String {
        val artifactUrl = assets.firstOrNull { (name, _) ->
            name.endsWith(".apk", ignoreCase = true) || name.endsWith(".zip", ignoreCase = true)
        }?.second
        return artifactUrl?.takeIf { it.isNotBlank() }
            ?: error("Latest release has no downloadable APK/ZIP artifact")
    }

    /** Fetches and parses the asset list from the GitHub releases/latest endpoint. */
    private fun fetchAssets(): List<Pair<String, String>> {
        val connection = (
            URL("https://api.github.com/repos/$owner/$repo/releases/latest")
                .openConnection() as HttpURLConnection
            ).apply {
                requestMethod = "GET"
                connectTimeout = 30_000
                readTimeout = 60_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Diplomat")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            }
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                error("GitHub API error: HTTP $code")
            }
            return parseAssets(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun parseAssets(releaseBody: String): List<Pair<String, String>> {
        val release = JSONObject(releaseBody)
        val assets = release.getJSONArray("assets")
        return (0 until assets.length()).map { i ->
            val asset = JSONObject(assets.getString(i))
            asset.optString("name") to asset.optString("browser_download_url")
        }
    }
}
