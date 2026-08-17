package com.roomai.app

/**
 * Converts RoomAI diagnosis into an actionable solution brief.
 *
 * This is deliberately NOT an image generator.
 *
 * Diagnosis
 *   -> Priorities
 *   -> Keep / Replace / Upgrade
 *   -> Budget
 *   -> Solution Brief
 *   -> Generation
 */
data class RoomAISolutionAction(
    val title: String,
    val action: String,
    val priority: String,
    val budget: Int
)

data class RoomAISolutionBrief(
    val goal: String,
    val summary: String,
    val actions: List<RoomAISolutionAction>,
    val keep: List<String>,
    val replace: List<String>,
    val upgrade: List<String>,
    val totalBudget: Int
) {
    fun generationBrief(): String {
        val builder = StringBuilder()

        builder.appendLine("ROOMAI SOLUTION BRIEF")
        builder.appendLine()
        builder.appendLine("Goal:")
        builder.appendLine(goal)
        builder.appendLine()

        builder.appendLine("Room problem:")
        builder.appendLine(summary)
        builder.appendLine()

        builder.appendLine("What must be preserved:")
        if (keep.isEmpty()) {
            builder.appendLine("- Preserve existing useful elements whenever possible.")
        } else {
            keep.forEach {
                builder.appendLine("- $it")
            }
        }

        builder.appendLine()
        builder.appendLine("What should be replaced:")
        if (replace.isEmpty()) {
            builder.appendLine("- No replacement required unless necessary.")
        } else {
            replace.forEach {
                builder.appendLine("- $it")
            }
        }

        builder.appendLine()
        builder.appendLine("What should be upgraded:")
        if (upgrade.isEmpty()) {
            builder.appendLine("- No unnecessary upgrades.")
        } else {
            upgrade.forEach {
                builder.appendLine("- $it")
            }
        }

        builder.appendLine()
        builder.appendLine("ACTION PLAN:")

        actions.forEachIndexed { index, action ->
            builder.appendLine(
                "${index + 1}. " +
                    "${action.title}: " +
                    "${action.action} " +
                    "[${action.priority}] " +
                    "[${action.budget} DZD]"
            )
        }

        builder.appendLine()
        builder.appendLine(
            "Maximum planned budget: ${totalBudget} DZD"
        )

        builder.appendLine()
        builder.appendLine(
            "IMPORTANT: Solve the identified room problems. " +
                "Do not redesign unrelated areas merely for visual novelty."
        )

        return builder.toString()
    }
}

fun buildRoomAISolutionBrief(
    diagnosis: RoomDiagnosis,
    budgetPlan: List<BudgetAllocation>,
    selectedGoal: String = "Improve my room"
): RoomAISolutionBrief {

    val actions = budgetPlan.map {
        RoomAISolutionAction(
            title = it.item,
            action = it.action,
            priority = it.priority,
            budget = it.amount
        )
    }

    return RoomAISolutionBrief(
        goal = selectedGoal,
        summary = diagnosis.summary.ifBlank {
            "RoomAI identified several opportunities for improvement."
        },
        actions = actions,
        keep = diagnosis.keep,
        replace = diagnosis.replace,
        upgrade = diagnosis.upgrade,
        totalBudget = budgetPlan.sumOf { it.amount }
    )
}
