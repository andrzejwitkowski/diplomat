package pl.diplomat.domain.normalization

class NormalizationStrategyRegistry {
    private val stringStrategies = mutableMapOf<String, StringNormalizationStrategy>()
    private val phoneStrategies = mutableMapOf<String, PhoneNormalizationStrategy>()

    fun register(strategy: StringNormalizationStrategy) {
        stringStrategies[strategy.key] = strategy
    }

    fun register(strategy: PhoneNormalizationStrategy) {
        phoneStrategies[strategy.key] = strategy
    }

    fun stringStrategy(key: String): StringNormalizationStrategy =
        stringStrategies.getValue(key)

    fun phoneStrategy(key: String): PhoneNormalizationStrategy =
        phoneStrategies.getValue(key)

    companion object {
        fun withDefaults(): NormalizationStrategyRegistry = NormalizationStrategyRegistry().apply {
            DefaultStringStrategies.forEach(::register)
            DefaultPhoneStrategies.forEach(::register)
        }
    }
}
