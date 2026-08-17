package com.roomai.app

/**
 * Product-level contracts shared by diagnosis, solution planning
 * and future persistence.
 *
 * These types intentionally do not call network APIs.
 */

enum class RoomCaseStatus {
    NEW,
    EVIDENCE_REQUIRED,
    DIAGNOSING,
    DIAGNOSED,
    MEASUREMENT_REQUIRED,
    SOLUTION_READY,
    VISUALIZED,
    VALIDATED,
    NEEDS_REVISION,
    SOLVED
}

data class RoomAICostRange(
    val min: Int,
    val max: Int,
    val currency: String = "DZD",
    val confidence: String = "unknown"
) {
    init {
        require(min >= 0)
        require(max >= min)
    }
}

data class RoomAISolutionOption(
    val id: String,
    val title: String,
    val summary: String,
    val actions: List<String>,
    val estimatedCost: RoomAICostRange? = null,
    val impact: String = "unknown",
    val feasibility: String = "unknown",
    val requiresMeasurement: Boolean = false
)

data class RoomAISolutionValidation(
    val passed: Boolean,
    val status: String,
    val reasons: List<String> = emptyList()
)

data class RoomAIFeedback(
    val result: String,
    val note: String = ""
)

data class RoomAICaseSnapshot(
    val caseId: String,
    val goal: String,
    val problemType: String,
    val status: RoomCaseStatus,
    val photoCount: Int,
    val measurementCount: Int,
    val solutionCount: Int
)

object RoomAISolutionGuard {

    fun validate(
        goal: String,
        summary: String,
        actions: List<String>,
        requiresMeasurement: Boolean,
        measurementCount: Int
    ): RoomAISolutionValidation {

        val reasons = mutableListOf<String>()

        if (goal.isBlank()) {
            reasons += "Missing user goal."
        }

        if (summary.isBlank()) {
            reasons += "Missing diagnosis summary."
        }

        if (actions.isEmpty()) {
            reasons += "No actionable solution steps."
        }

        if (requiresMeasurement && measurementCount <= 0) {
            reasons += "Required measurement is missing."
        }

        return if (reasons.isEmpty()) {
            RoomAISolutionValidation(
                passed = true,
                status = "PASS"
            )
        } else {
            RoomAISolutionValidation(
                passed = false,
                status = "REVISION_REQUIRED",
                reasons = reasons
            )
        }
    }
}
