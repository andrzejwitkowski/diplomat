package pl.diplomat.infrastructure.conversation

/**
 * The constant system prompt sent to the LLM provider when suggesting an answer.
 * Kept in its own file so it can be swapped for a larger prompt later without
 disturbing the ViewModel.
 */
object SuggestAnswerPrompt {
    const val SYSTEM_PROMPT = "Zaproponuj odpowiedz na konwersacie zgodnie z sentymentem"
}
