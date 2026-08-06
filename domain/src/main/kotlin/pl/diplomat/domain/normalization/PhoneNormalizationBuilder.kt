package pl.diplomat.domain.normalization

class PhoneNormalizationBuilder(
    private val registry: NormalizationStrategyRegistry,
    private val strategyKeys: List<String> = emptyList(),
) {
    fun with(strategyKey: String): PhoneNormalizationBuilder =
        copy(strategyKeys = strategyKeys + strategyKey)

    fun normalize(value: String): NormalizedPhone {
        val context = PhoneNormalizationContext(raw = value)
        strategyKeys.forEach { key -> registry.phoneStrategy(key).apply(context) }
        return NormalizedPhone(
            normalized = context.normalized.ifBlank { context.digits },
            matchKey = context.matchKey,
        )
    }

    private fun copy(strategyKeys: List<String>): PhoneNormalizationBuilder =
        PhoneNormalizationBuilder(registry, strategyKeys)
}
