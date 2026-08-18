package com.roomai.app

/**
 * Single source of truth for the user's room problem and constraints.
 *
 * Problem:
 *   What does the user actually want to solve?
 *
 * Constraints:
 *   How should the solution fit the user's situation?
 */
object RoomAIProblemFlow {

    // ------------------------------------------------------------
    // REAL ROOM PROBLEMS
    // ------------------------------------------------------------

    const val IMPROVE = "improve"
    const val SPACE = "space"
    const val LIGHTING = "lighting"
    const val STORAGE = "storage"
    const val EXISTING_FURNITURE = "existing_furniture"
    const val FUNCTION = "function"
    const val COLOR = "color"
    const val DECOR = "decor"
    const val SPECIFIC_CHANGE = "specific_change"

    const val LAYOUT = "layout"
    const val MOVEMENT = "movement"
    const val ERGONOMICS = "ergonomics"
    const val SAFETY = "safety"
    const val ACCESS = "access"

    // ------------------------------------------------------------
    // CONSTRAINTS
    // ------------------------------------------------------------

    const val CONSTRAINT_BUDGET = "budget"
    const val CONSTRAINT_KEEP_FURNITURE = "keep_furniture"
    const val CONSTRAINT_MINIMAL_CHANGES = "minimal_changes"
    const val CONSTRAINT_READY_TO_BUY = "ready_to_buy"

    var selectedProblem: String = IMPROVE
        private set

    private val constraints = linkedSetOf<String>()

    val selectedConstraints: Set<String>
        get() = constraints.toSet()

    fun select(problem: String) {
        selectedProblem =
            when (problem) {
                IMPROVE,
                SPACE,
                LIGHTING,
                STORAGE,
                EXISTING_FURNITURE,
                FUNCTION,
                COLOR,
                DECOR,
                SPECIFIC_CHANGE,
                LAYOUT,
                MOVEMENT,
                ERGONOMICS,
                SAFETY,
                ACCESS -> problem

                else -> IMPROVE
            }
    }

    fun toggleConstraint(constraint: String) {
        if (constraint !in validConstraints()) return

        if (!constraints.add(constraint)) {
            constraints.remove(constraint)
        }
    }

    fun hasConstraint(constraint: String): Boolean {
        return constraint in constraints
    }

    fun clearConstraints() {
        constraints.clear()
    }

    fun label(): String {
        return when (selectedProblem) {
            SPACE -> "Use my space better"
            LIGHTING -> "Fix the lighting"
            STORAGE -> "Improve storage"
            EXISTING_FURNITURE -> "Improve my furniture"
            FUNCTION -> "Make the room work better"
            COLOR -> "Fix the colors"
            DECOR -> "Improve the decor"
            LAYOUT -> "Fix the layout"
            MOVEMENT -> "Improve movement"
            ERGONOMICS -> "Improve comfort"
            SAFETY -> "Fix a safety problem"
            ACCESS -> "Improve access"
            SPECIFIC_CHANGE -> "Change something specific"
            else -> "Make my room better"
        }
    }

    fun constraintsSummary(): String {
        return constraints.mapNotNull {
            when (it) {
                CONSTRAINT_BUDGET ->
                    "keep spending low"

                CONSTRAINT_KEEP_FURNITURE ->
                    "keep useful existing furniture"

                CONSTRAINT_MINIMAL_CHANGES ->
                    "prefer minimal changes"

                CONSTRAINT_READY_TO_BUY ->
                    "ready to buy recommended items"

                else -> null
            }
        }.joinToString(", ")
    }

    fun labelWithConstraints(): String {
        val summary = constraintsSummary()

        return if (summary.isBlank()) {
            label()
        } else {
            "${label()} | Constraints: $summary"
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

    private fun validConstraints(): Set<String> {
        return setOf(
            CONSTRAINT_BUDGET,
            CONSTRAINT_KEEP_FURNITURE,
            CONSTRAINT_MINIMAL_CHANGES,
            CONSTRAINT_READY_TO_BUY
        )
    }
}
