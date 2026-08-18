package com.roomai.app

/**
 * RoomAI's commercial workflow is role-driven, not feature-driven.
 *
 * Consumer:
 *   problem -> diagnosis -> solution -> visualization
 *
 * Designer:
 *   client -> brief -> measurements -> constraints -> concepts -> approval
 *
 * Craftsman:
 *   room/product -> measurements -> specification -> materials -> quote
 *
 * Merchant:
 *   product -> staging -> variants -> marketing -> lead
 */
enum class RoomAIRole(
    val title: String,
    val description: String
) {
    HOMEOWNER(
        "Homeowner",
        "Solve a room problem before spending money."
    ),
    DESIGNER(
        "Designer",
        "Turn client requirements into structured design decisions."
    ),
    CRAFTSMAN(
        "Craftsman",
        "Turn measurements and design decisions into practical work."
    ),
    MERCHANT(
        "Merchant",
        "Turn furniture products into realistic selling content."
    )
}

data class RoomAIProjectBrief(
    val role: RoomAIRole,
    val room: String = "",
    val problem: String = "",
    val budget: String = "",
    val measurements: String = "",
    val constraints: List<String> = emptyList(),
    val requestedProduct: String = ""
)
