package com.roomai.app

/**
 * Single source of truth for the user's room problem.
 *
 * UI wording is separated from the diagnostic taxonomy so that
 * "Bad lighting" does not become an unrelated generic request.
 */
object RoomAIProblemFlow {

    const val IMPROVE = "improve"
    const val BUDGET = "budget"
    const val EXISTING_FURNITURE = "existing_furniture"
    const val SPACE = "space"
    const val SHOPPING = "shopping"
    const val SPECIFIC_CHANGE = "specific_change"

    const val LIGHTING = "lighting"
    const val STORAGE = "storage"
    const val LAYOUT = "layout"
    const val MOVEMENT = "movement"
    const val ERGONOMICS = "ergonomics"
    const val FUNCTION = "function"
    const val COLOR = "color"
    const val DECOR = "decor"
    const val SAFETY = "safety"
    const val ACCESS = "access"

    var selectedProblem: String = IMPROVE
        private set

    fun select(problem: String) {
        selectedProblem =
            when (problem) {
                IMPROVE,
                BUDGET,
                EXISTING_FURNITURE,
                SPACE,
                SHOPPING,
                SPECIFIC_CHANGE,
                LIGHTING,
                STORAGE,
                LAYOUT,
                MOVEMENT,
                ERGONOMICS,
                FUNCTION,
                COLOR,
                DECOR,
                SAFETY,
                ACCESS -> problem

                else -> IMPROVE
            }
    }

    fun label(): String {
        return when (selectedProblem) {
            BUDGET -> "Fit my budget"
            EXISTING_FURNITURE -> "Use my existing furniture"
            SPACE -> "Use my space better"
            SHOPPING -> "I don't know what to buy"
            LIGHTING -> "Fix the lighting"
            STORAGE -> "Improve storage"
            LAYOUT -> "Fix the layout"
            MOVEMENT -> "Improve movement"
            ERGONOMICS -> "Improve comfort"
            FUNCTION -> "Make the room work better"
            COLOR -> "Fix the colors"
            DECOR -> "Improve the decor"
            SAFETY -> "Fix a safety problem"
            ACCESS -> "Improve access"
            SPECIFIC_CHANGE -> "Change something specific"
            else -> "Make my room better"
        }
    }

    fun diagnosticType(): String {
        return when (selectedProblem) {
            LIGHTING -> "lighting"
            STORAGE -> "storage"
            LAYOUT -> "layout"
            MOVEMENT -> "movement"
            ERGONOMICS -> "ergonomics"
            FUNCTION -> "function"
            COLOR -> "color"
            DECOR -> "decor"
            SAFETY -> "safety"
            ACCESS -> "access"
            SPACE -> "space"
            else -> "other"
        }
    }
}
