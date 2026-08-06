package pl.diplomat.domain.normalization

object NormalizationStrategyKeys {
    const val TRIM = "trim"
    const val COLLAPSE_WHITESPACE = "collapse_whitespace"

    const val PHONE_TRIM = "phone.trim"
    const val PHONE_EXTRACT_DIGITS = "phone.extract_digits"
    const val PHONE_BUILD_NORMALIZED = "phone.build_normalized"
    const val PHONE_MATCH_KEY_NATIONAL_9 = "phone.match_key.national_9"
}
