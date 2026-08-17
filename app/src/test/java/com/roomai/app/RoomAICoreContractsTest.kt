package com.roomai.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomAICoreContractsTest {

    @Test
    fun valid_solution_passes() {
        val result = RoomAISolutionGuard.validate(
            goal = "Fix lighting",
            summary = "The room has insufficient task lighting.",
            actions = listOf("Add task lighting"),
            requiresMeasurement = false,
            measurementCount = 0
        )

        assertTrue(result.passed)
        assertTrue(result.status == "PASS")
    }

    @Test
    fun missing_goal_fails() {
        val result = RoomAISolutionGuard.validate(
            goal = "",
            summary = "Problem found.",
            actions = listOf("Fix it"),
            requiresMeasurement = false,
            measurementCount = 0
        )

        assertFalse(result.passed)
    }

    @Test
    fun required_measurement_blocks_solution() {
        val result = RoomAISolutionGuard.validate(
            goal = "Improve layout",
            summary = "Clearance may be insufficient.",
            actions = listOf("Move furniture"),
            requiresMeasurement = true,
            measurementCount = 0
        )

        assertFalse(result.passed)
        assertTrue(
            result.reasons.any {
                it.contains("measurement", ignoreCase = true)
            }
        )
    }

    @Test
    fun measurement_allows_solution_when_other_requirements_are_valid() {
        val result = RoomAISolutionGuard.validate(
            goal = "Improve layout",
            summary = "Clearance is constrained.",
            actions = listOf("Move furniture"),
            requiresMeasurement = true,
            measurementCount = 1
        )

        assertTrue(result.passed)
    }

    @Test
    fun cost_range_rejects_invalid_range() {
        var failed = false

        try {
            RoomAICostRange(
                min = 100,
                max = 50
            )
        } catch (_: IllegalArgumentException) {
            failed = true
        }

        assertTrue(failed)
    }
}
