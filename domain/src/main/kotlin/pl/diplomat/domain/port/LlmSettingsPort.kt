package pl.diplomat.domain.port

import pl.diplomat.domain.model.LlmSettings

interface LlmSettingsPort {
    fun load(): LlmSettings
    suspend fun save(settings: LlmSettings)
}
