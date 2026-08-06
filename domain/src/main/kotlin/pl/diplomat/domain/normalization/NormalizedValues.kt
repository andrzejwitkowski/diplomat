package pl.diplomat.domain.normalization

data class NormalizedDisplayName(val value: String)

data class NormalizedPhone(
    val normalized: String,
    val matchKey: String,
)
