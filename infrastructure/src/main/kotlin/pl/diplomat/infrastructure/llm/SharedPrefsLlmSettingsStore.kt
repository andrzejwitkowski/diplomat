package pl.diplomat.infrastructure.llm

import android.content.Context
import pl.diplomat.domain.model.LlmSettings
import pl.diplomat.domain.port.LlmSettingsPort

class SharedPrefsLlmSettingsStore(context: Context) : LlmSettingsPort {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun load(): LlmSettings {
        val settings = LlmSettings(
            baseUrl = prefs.getString(KEY_BASE_URL, null) ?: LlmSettings.DEFAULT_BASE_URL,
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            model = prefs.getString(KEY_MODEL, null) ?: LlmSettings.DEFAULT_MODEL,
        )
        Log.d("LlmSettings", "Loaded settings: $settings")
        return settings
    }

    override suspend fun save(settings: LlmSettings) {
        Log.d("LlmSettings", "Saving settings: $settings")
        prefs.edit()
            .putString(KEY_BASE_URL, settings.baseUrl)
            .putString(KEY_API_KEY, settings.apiKey)
            .putString(KEY_MODEL, settings.model)
            .apply()
    }

    private companion object {
        const val PREFS = "llm_settings"
        const val KEY_BASE_URL = "base_url"
        const val KEY_API_KEY = "api_key"
        const val KEY_MODEL = "model"
    }
}
