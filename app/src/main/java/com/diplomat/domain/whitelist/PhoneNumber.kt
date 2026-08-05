package com.diplomat.domain.whitelist

/**
 * Normalized phone number for whitelist matching.
 * Digits and optional leading '+' only.
 */
@JvmInline
value class PhoneNumber private constructor(val value: String) {
    companion object {
        fun parse(raw: String): PhoneNumber? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            val normalized = buildString {
                trimmed.forEachIndexed { i, c ->
                    when {
                        c.isDigit() -> append(c)
                        c == '+' && i == 0 && isEmpty() -> append(c)
                    }
                }
            }
            return if (normalized.count { it.isDigit() } >= 3) PhoneNumber(normalized) else null
        }

        fun ofNormalized(value: String) = PhoneNumber(value)
    }
}
