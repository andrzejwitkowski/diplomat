package pl.diplomat.domain.port

interface AvatarStoragePort {
    suspend fun saveFromUri(sourceUri: String): String
}
