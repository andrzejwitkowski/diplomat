package pl.diplomat.domain.normalization

interface StringNormalizationStrategy {
    val key: String
    fun apply(value: String): String
}

data class PhoneNormalizationContext(
    val raw: String,
    var working: String = raw,
    var digits: String = "",
    var normalized: String = "",
    var matchKey: String = "",
)

interface PhoneNormalizationStrategy {
    val key: String
    fun apply(context: PhoneNormalizationContext)
}
