package pl.diplomat.domain.model

/**
 * Sentiment label attached to a conversation when suggesting an answer.
 * The UI maps each value to a color (green/yellow/red); the domain only holds
 * the semantic label.
 */
enum class Sentiment(val label: String) {
    POSITIVE("Positive"),
    NEUTRAL("Neutral"),
    NEGATIVE("Negative"),
}
