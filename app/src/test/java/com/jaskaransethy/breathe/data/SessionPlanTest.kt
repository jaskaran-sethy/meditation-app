package com.jaskaransethy.breathe.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPlanTest {

    @Test
    fun `patterns skip zero-length holds`() {
        assertEquals(listOf(Phase.Inhale, Phase.HoldIn, Phase.Exhale), PresetPattern.Calm.steps.map { it.phase })
        assertEquals(listOf(Phase.Inhale, Phase.Exhale), PresetPattern.Sleep.steps.map { it.phase })
        assertEquals(4, PresetPattern.Box.steps.size)
        assertEquals("4 · 7 · 8", PresetPattern.Calm.rhythm)
        assertEquals("4 · 4 · 4 · 4", PresetPattern.Box.rhythm)
    }

    @Test
    fun `session covers the chosen length with whole breaths`() {
        // Calm is 19 s per breath: 300 s needs 16 breaths (304 s).
        val plan = SessionPlan(PresetPattern.Calm, 5)
        assertEquals(16, plan.breaths)
        assertEquals(304_000L, plan.totalMs)

        // Balance is 10 s per breath and divides 2 minutes exactly.
        assertEquals(12, SessionPlan(PresetPattern.Balance, 2).breaths)
    }

    @Test
    fun `state walks through the phases of a breath`() {
        val plan = SessionPlan(PresetPattern.Calm, 5)

        plan.stateAt(0).let {
            assertEquals(Phase.Inhale, it.step.phase)
            assertEquals(4, it.secondsLeftInPhase)
            assertEquals(0, it.breathIndex)
        }
        plan.stateAt(3_500).let {
            assertEquals(Phase.Inhale, it.step.phase)
            assertEquals(1, it.secondsLeftInPhase)
            assertEquals(0.875f, it.phaseProgress, 0.001f)
        }
        plan.stateAt(4_000).let {
            assertEquals(Phase.HoldIn, it.step.phase)
            assertEquals(7, it.secondsLeftInPhase)
        }
        plan.stateAt(11_000).let { assertEquals(Phase.Exhale, it.step.phase) }
        plan.stateAt(19_000).let {
            assertEquals(Phase.Inhale, it.step.phase)
            assertEquals(1, it.breathIndex)
        }
    }

    @Test
    fun `session finishes at the end of the last exhale`() {
        val plan = SessionPlan(PresetPattern.Sleep, 2)
        val almost = plan.stateAt(plan.totalMs - 1)
        assertFalse(almost.finished)
        assertEquals(Phase.Exhale, almost.step.phase)

        val end = plan.stateAt(plan.totalMs + 5_000)
        assertTrue(end.finished)
        assertEquals(Phase.Exhale, end.step.phase)
        assertEquals(plan.breaths - 1, end.breathIndex)
        assertEquals(0L, end.remainingMs)
        assertEquals(0, end.secondsLeftInPhase)
    }
}
