package pl.diplomat.domain.model

@JvmInline
value class PhoneNumber(val value: String) {
    init {
        require(value.isNotBlank()) { "Phone number cannot be blank" }
        require(value.all { it.isDigit() || it == '+' || it == ' ' || it == '-' || it == '(' || it == ')' }) {
            "Phone number contains invalid characters"
        }
        val normalizedValue = normalized()
        require(normalizedValue.any { it.isDigit() }) {
            "Phone number must contain a digit"
        }
        require(
            normalizedValue.count { it == '+' } <= 1 &&
                (normalizedValue.indexOf('+') == -1 || normalizedValue.startsWith("+")),
        ) {
            "Phone number has an invalid '+' position"
        }
    }

    fun normalized(): String = value.filter { it.isDigit() || it == '+' }
}

data class WhitelistedContact(
    val id: Long,
    val displayName: String,
    val phoneNumber: PhoneNumber,
    val avatarUri: String? = null,
) {
    init {
        require(displayName.isNotBlank()) { "Display name cannot be blank" }
    }
}
