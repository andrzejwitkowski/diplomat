package pl.diplomat.domain.model

fun PhoneNumber.matchKey(): String {
    val digits = value.filter { it.isDigit() }
    return if (digits.length >= 9) digits.takeLast(9) else digits
}

fun String.normalizeDisplayName(): String = trim().replace(Regex("\\s+"), " ")
