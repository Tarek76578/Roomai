package com.roomai.app

/**
 * RoomAI's product flow.
 *
 * The application is problem-first rather than generation-first.
 *
 * Problem
 *    ->
 * Diagnose
 *    ->
 * Plan
 *    ->
 * Generate
 *    ->
 * Precision Edit
 *    ->
 * Verify
 *    ->
 * Remember
 *
 * Existing engines remain reusable:
 * - RoomAIDecisionEngine
 * - RoomAIPrecision
 * - RoomAIFoundation / RoomState
 */
object RoomAIProductFlow {

    const val PROBLEM = "problem"
    const val DIAGNOSE = "diagnose"
    const val PLAN = "plan"
    const val GENERATE = "generate"
    const val PRECISION = "precision"
    const val VERIFY = "verify"
    const val REMEMBER = "remember"

    val primarySteps = listOf(
        PROBLEM,
        DIAGNOSE,
        PLAN,
        GENERATE,
        PRECISION,
        VERIFY,
        REMEMBER
    )
}
