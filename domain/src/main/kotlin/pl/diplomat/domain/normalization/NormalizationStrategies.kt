package pl.diplomat.domain.normalization

internal object TrimStrategy : StringNormalizationStrategy {
    override val key: String = NormalizationStrategyKeys.TRIM
    override fun apply(value: String): String = value.trim()
}

internal object CollapseWhitespaceStrategy : StringNormalizationStrategy {
    override val key: String = NormalizationStrategyKeys.COLLAPSE_WHITESPACE
    override fun apply(value: String): String = value.replace(Regex("\\s+"), " ")
}

internal object PhoneTrimStrategy : PhoneNormalizationStrategy {
    override val key: String = NormalizationStrategyKeys.PHONE_TRIM
    override fun apply(context: PhoneNormalizationContext) {
        context.working = context.working.trim()
    }
}

internal object PhoneExtractDigitsStrategy : PhoneNormalizationStrategy {
    override val key: String = NormalizationStrategyKeys.PHONE_EXTRACT_DIGITS
    override fun apply(context: PhoneNormalizationContext) {
        context.digits = context.working.filter { it.isDigit() }
    }
}

internal object PhoneBuildNormalizedStrategy : PhoneNormalizationStrategy {
    override val key: String = NormalizationStrategyKeys.PHONE_BUILD_NORMALIZED
    override fun apply(context: PhoneNormalizationContext) {
        context.normalized = context.working.filter { it.isDigit() || it == '+' }
    }
}

internal object PhoneMatchKeyNational9Strategy : PhoneNormalizationStrategy {
    override val key: String = NormalizationStrategyKeys.PHONE_MATCH_KEY_NATIONAL_9
    override fun apply(context: PhoneNormalizationContext) {
        val digits = context.digits.ifBlank { context.working.filter { it.isDigit() } }
        context.matchKey = if (digits.length >= 9) digits.takeLast(9) else digits
    }
}

internal val DefaultStringStrategies: List<StringNormalizationStrategy> = listOf(
    TrimStrategy,
    CollapseWhitespaceStrategy,
)

internal val DefaultPhoneStrategies: List<PhoneNormalizationStrategy> = listOf(
    PhoneTrimStrategy,
    PhoneExtractDigitsStrategy,
    PhoneBuildNormalizedStrategy,
    PhoneMatchKeyNational9Strategy,
)
