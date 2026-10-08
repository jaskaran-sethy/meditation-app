package com.example.myapplication.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomPatternTest {

    private val pattern = CustomPattern.of("custom-abc", "Morning: 4, 7 & 8 ☀", 4, 7, 8, 0)

    @Test
    fun `round trips through storage, even with separators in the name`() {
        val stored = pattern.serialize()
        // The store joins patterns with ',' and the session log splits on ':'; neither may leak.
        assertEquals(5, stored.count { it == ':' })
        assertTrue(',' !in stored)
        assertEquals(pattern, CustomPattern.parse(stored))
    }

    @Test
    fun `skips zero-length holds like the presets`() {
        assertEquals(listOf(Phase.Inhale, Phase.HoldIn, Phase.Exhale), pattern.steps.map { it.phase })
        assertEquals("4 · 7 · 8", pattern.rhythm)
        assertEquals(19, pattern.cycleSeconds)
    }

    @Test
    fun `counts and name are clamped so every saved pattern is playable`() {
        val wild = CustomPattern.of("custom-x", "  " + "a".repeat(40) + "  ", 0, -3, 99, 21)
        assertEquals(1, wild.inhale)
        assertEquals(0, wild.holdIn)
        assertEquals(20, wild.exhale)
        assertEquals(20, wild.holdOut)
        assertEquals(CustomPattern.MAX_NAME_LENGTH, wild.name.length)
        assertTrue(wild.cycleSeconds > 0)
    }

    @Test
    fun `rejects malformed entries`() {
        assertNull(CustomPattern.parse(""))
        assertNull(CustomPattern.parse("calm:4:7:8:0:Calm"))
        assertNull(CustomPattern.parse("custom-a:4:x:8:0:Name"))
        assertNull(CustomPattern.parse("custom-a:4:7:8:Name"))
    }

    @Test
    fun `new ids never clash with presets or break routes`() {
        val id = CustomPattern.newId(1_791_405_853_055L)
        assertTrue(id.startsWith(CustomPattern.ID_PREFIX))
        assertNull(PresetPattern.fromId(id))
        assertTrue(id.all { it.isLetterOrDigit() || it == '-' })
    }

    @Test
    fun `sessions with a custom pattern end after a whole breath`() {
        val plan = SessionPlan(CustomPattern.of("custom-a", "Slow", 6, 0, 9, 3), 2)
        assertEquals(18_000L, plan.cycleMs)
        assertEquals(7, plan.breaths)
        assertEquals(Phase.HoldOut, plan.stateAt(plan.totalMs).step.phase)
    }

    @Test
    fun `session records keep custom pattern ids, so deleted patterns still count`() {
        val record = SessionRecord(20_000L, 540, "custom-abc", 5, 300, false)
        assertEquals(record, SessionRecord.parse(record.serialize()))
    }

    @Test
    fun `custom patterns count towards minutes but not Four Winds`() {
        val records = List(3) { SessionRecord(20_000L, 540, "custom-abc", 5, 300, false) }
        assertEquals(0, BadgeRules.fullSessionsByPattern(records).values.sum())
        assertEquals(15, BadgeRules.fullMinutes(records))
    }
}
