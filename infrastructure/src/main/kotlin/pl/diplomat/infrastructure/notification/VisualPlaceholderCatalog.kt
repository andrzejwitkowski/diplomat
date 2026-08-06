package pl.diplomat.infrastructure.notification

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.ArrayRes
import pl.diplomat.domain.model.VisualMediaKind
import pl.diplomat.infrastructure.R
import java.util.Locale

class VisualPlaceholderCatalog private constructor(
    private val byKind: Map<VisualMediaKind, Set<String>>,
) {
    fun labelsFor(kind: VisualMediaKind): Set<String> = byKind[kind].orEmpty()

    fun detectKind(text: String): VisualMediaKind? {
        val normalized = text.trim().lowercase()
        if (normalized.isBlank()) return null
        return byKind.entries.firstOrNull { (_, labels) ->
            labels.any { label -> normalized == label || normalized.endsWith(" $label") }
        }?.key
    }

    fun isPlaceholderOnly(text: String, kind: VisualMediaKind): Boolean {
        val normalized = text.trim().lowercase()
        if (normalized.isBlank()) return true
        val labels = labelsFor(kind)
        return labels.any { label -> normalized == label || normalized.endsWith(" $label") }
    }

    companion object {
        private val SUPPORTED_LOCALES = listOf(
            Locale.ENGLISH,
            Locale.forLanguageTag("pl"),
        )

        private val KIND_ARRAYS = mapOf(
            VisualMediaKind.PHOTO to R.array.visual_placeholders_photo,
            VisualMediaKind.GIF to R.array.visual_placeholders_gif,
            VisualMediaKind.STICKER to R.array.visual_placeholders_sticker,
            VisualMediaKind.VIDEO to R.array.visual_placeholders_video,
        )

        fun fromContext(context: Context): VisualPlaceholderCatalog {
            val byKind = KIND_ARRAYS.mapValues { (kind, arrayRes) ->
                SUPPORTED_LOCALES
                    .flatMap { locale -> loadLabels(context, locale, arrayRes) }
                    .map { it.trim().lowercase() }
                    .filter { it.isNotBlank() }
                    .toSet()
            }
            return VisualPlaceholderCatalog(byKind)
        }

        fun fromLabels(byKind: Map<VisualMediaKind, Set<String>>): VisualPlaceholderCatalog =
            VisualPlaceholderCatalog(
                byKind.mapValues { (_, labels) -> labels.map { it.lowercase() }.toSet() },
            )

        private fun loadLabels(context: Context, locale: Locale, @ArrayRes arrayRes: Int): List<String> {
            val configuration = Configuration(context.resources.configuration)
            configuration.setLocale(locale)
            val localizedContext = context.createConfigurationContext(configuration)
            return localizedContext.resources.getStringArray(arrayRes).toList()
        }
    }
}
