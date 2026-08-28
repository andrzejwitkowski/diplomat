package pl.diplomat.domain.port

/**
 * Resolves the static download URL of the artifact attached to the newest GitHub release
 * (the response of `GET /releases/latest`). Used for OTA updates, so the app always
 * downloads the most recently built version without requiring a manually pasted URL.
 */
interface LatestReleaseUrlResolver {
    /**
     * @return the static download URL of the latest release asset, or a [Failure] when
     * the URL cannot be resolved (network error, HTTP error, or no assets).
     */
    suspend fun resolveLatestUrl(): Result<String>
}
