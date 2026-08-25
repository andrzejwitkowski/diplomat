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
        Log.d("GithubLatestUrlResolver", "Resolving latest URL for $owner/$repo")
        parseStaticDownloadUrl(fetchAssets())
    }

    /**
     * Extracts the static `browser_download_url` of the first `.apk`/`.zip` asset.
     * Accepts already-parsed `(name, url)` pairs so it can be unit-tested without `org.json`.
     */
    internal fun parseStaticDownloadUrl(assets: List<Pair<String, String>>): String {
        Log.d("GithubLatestUrlResolver", "Parsing static download URL from ${assets.size} assets")
        val artifactUrl = assets.firstOrNull { (name, _) ->
            name.endsWith(".apk", ignoreCase = true) || name.endsWith(".zip", ignoreCase = true)
        }?.second
        Log.d("GithubLatestUrlResolver", "Found artifact URL: ${artifactUrl ?: "none"}")
        return artifactUrl?.takeIf { it.isNotBlank() }
            ?: error("Latest release has no downloadable APK/ZIP artifact")
    }

    /** Fetches and parses the asset list from the GitHub releases/latest endpoint. */
    private fun fetchAssets(): List<Pair<String, String>> {
        Log.d("GithubLatestUrlResolver", "Fetching assets from GitHub API")
        val connection = (
            URL("$RELEASES_BASE/$owner/$repo/releases/latest").openConnection() as HttpURLConnection
            ).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", ACCEPT_HEADER)
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("X-GitHub-Api-Version", API_VERSION)
            }
        try {
            val code = connection.responseCode
            Log.d("GithubLatestUrlResolver", "GitHub API response code: $code")
            if (code != HttpURLConnection.HTTP_OK) {
                error("GitHub API error: HTTP $code")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            Log.d("GithubLatestUrlResolver", "GitHub API response body preview: ${body.take(200)}")
            return parseAssets(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseAssets(releaseBody: String): List<Pair<String, String>> {
        Log.d("GithubLatestUrlResolver", "Parsing release assets")
        val release = JSONObject(releaseBody)
        val assets = release.getJSONArray("assets")
        val assetList = (0 until assets.length()).map { i ->
            val asset = JSONObject(assets.getString(i))
            val name = asset.optString("name")
            val url = asset.optString("browser_download_url")
            Log.d("GithubLatestUrlResolver", "Asset: $name -> $url")
            name to url
        }
        return assetList
    }

    private companion object {
        const val RELEASES_BASE = "https://api.github.com/repos"
        const val ACCEPT_HEADER = "application/vnd.github+json"
        const val USER_AGENT = "Diplomat"
        const val API_VERSION = "2022-11-28"
        const val CONNECT_TIMEOUT_MS = 30_000
        const val READ_TIMEOUT_MS = 60_000
    }
}
