package com.roomai.app

import org.junit.Assert.assertEquals
import org.junit.Test

class RoomAIProblemFlowTest {

    @Test
    fun lighting_maps_to_lighting_diagnostic_type() {
        RoomAIProblemFlow.select(RoomAIProblemFlow.LIGHTING)

        assertEquals(
            "lighting",
            RoomAIProblemFlow.diagnosticType()
        )

        assertEquals(
            "Fix the lighting",
            RoomAIProblemFlow.label()
        )
    }

    @Test
    fun unknown_problem_falls_back_to_improve() {
        RoomAIProblemFlow.select("invalid_problem")

        assertEquals(
            "Make my room better",
            RoomAIProblemFlow.label()
        )
    }
}
