package pl.diplomat.domain.normalization

class StringNormalizationBuilder(
    private val registry: NormalizationStrategyRegistry,
    private val strategyKeys: List<String> = emptyList(),
) {
    fun with(strategyKey: String): StringNormalizationBuilder =
        copy(strategyKeys = strategyKeys + strategyKey)

    fun normalize(value: String): String =
        strategyKeys.fold(value) { current, key ->
            registry.stringStrategy(key).apply(current)
        }

    private fun copy(strategyKeys: List<String>): StringNormalizationBuilder =
        StringNormalizationBuilder(registry, strategyKeys)
}
