package pl.diplomat.domain.normalization

import pl.diplomat.domain.model.PhoneNumber

class NormalizationService(
    private val registry: NormalizationStrategyRegistry,
) {
    fun displayName(): StringNormalizationBuilder = StringNormalizationBuilder(registry)

    fun phoneNumber(): PhoneNormalizationBuilder = PhoneNormalizationBuilder(registry)

    fun normalizeDisplayName(value: String): NormalizedDisplayName =
        NormalizedDisplayName(
            displayName()
                .with(NormalizationStrategyKeys.TRIM)
                .with(NormalizationStrategyKeys.COLLAPSE_WHITESPACE)
                .normalize(value),
        )

    fun normalizePhone(value: String): NormalizedPhone =
        phoneNumber()
            .with(NormalizationStrategyKeys.PHONE_TRIM)
            .with(NormalizationStrategyKeys.PHONE_EXTRACT_DIGITS)
            .with(NormalizationStrategyKeys.PHONE_BUILD_NORMALIZED)
            .with(NormalizationStrategyKeys.PHONE_MATCH_KEY_NATIONAL_9)
            .normalize(value)

    fun phonesMatch(contactPhone: PhoneNumber, telephonyAddress: String): Boolean {
        val contactKey = normalizePhone(contactPhone.value).matchKey
        val addressKey = runCatching { normalizePhone(telephonyAddress.trim()).matchKey }.getOrNull()
            ?: return false
        return contactKey == addressKey
    }

    companion object {
        val default: NormalizationService = NormalizationService(NormalizationStrategyRegistry.withDefaults())
    }
}
