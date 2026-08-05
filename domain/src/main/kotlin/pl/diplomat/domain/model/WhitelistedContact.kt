package pl.diplomat.domain.model

@JvmInline
value class PhoneNumber(val value: String) {
    init {
        require(value.isNotBlank()) { "Phone number cannot be blank" }
        require(value.all { it.isDigit() || it == '+' || it == ' ' || it == '-' || it == '(' || it == ')' }) {
            "Phone number contains invalid characters"
        }
    }

    fun normalized(): String = value.filter { it.isDigit() || it == '+' }
}

data class WhitelistedContact(
    val id: Long,
    val displayName: String,
    val phoneNumber: PhoneNumber,
) {
    init {
        require(displayName.isNotBlank()) { "Display name cannot be blank" }
    }
}
