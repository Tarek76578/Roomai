package com.roomai.app

/**
 * Problem-first entry point for RoomAI.
 *
 * This object keeps the user's intent separate from the UI.
 * The existing Decision Engine remains the actual diagnosis engine.
 */
object RoomAIProblemFlow {

    const val IMPROVE = "improve"
    const val BUDGET = "budget"
    const val EXISTING_FURNITURE = "existing_furniture"
    const val SPACE = "space"
    const val SHOPPING = "shopping"
    const val SPECIFIC_CHANGE = "specific_change"

    var selectedProblem: String = IMPROVE
        private set

    fun select(problem: String) {
        selectedProblem = problem
    }

    fun label(): String {
        return when (selectedProblem) {
            BUDGET -> "Fit my budget"
            EXISTING_FURNITURE -> "Use my existing furniture"
            SPACE -> "Use my space better"
            SHOPPING -> "I don't know what to buy"
            SPECIFIC_CHANGE -> "Change something specific"
            else -> "Make my room better"
        }
    }
}
