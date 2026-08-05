package com.diplomat.ui.navigation

/**
 * Navigation routes for the app.
 */
object DiplomatDestinations {
    const val DASHBOARD = "dashboard"

    const val DECISION_ARG_ID = "messageId"
    const val DECISION_ROUTE = "decision/{$DECISION_ARG_ID}"

    fun decision(messageId: Long): String = "decision/$messageId"
}
