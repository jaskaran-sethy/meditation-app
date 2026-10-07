package com.example.myapplication.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PracticeStatsTest {

    // 2026-10-07 was a Wednesday.
    private val wednesday = 20_733L

    @Test
    fun `epoch day uses the local offset`() {
        val utcMidnight = wednesday * PracticeStats.MS_PER_DAY
        assertEquals(wednesday, PracticeStats.epochDay(utcMidnight, 0))
        // An hour before UTC midnight is already the next day in UTC+5:30.
        assertEquals(wednesday, PracticeStats.epochDay(utcMidnight - 3_600_000L, 19_800_000))
        assertEquals(wednesday - 1, PracticeStats.epochDay(utcMidnight - 1, 0))
    }

    @Test
    fun `weeks start on Monday`() {
        assertEquals(wednesday - 2, PracticeStats.weekStart(wednesday))
        assertEquals(wednesday - 2, PracticeStats.weekStart(wednesday - 2))
        assertEquals(wednesday - 9, PracticeStats.weekStart(wednesday - 3))
        assertEquals(3, PracticeStats.dayOfWeek(wednesday))
        assertEquals(1, PracticeStats.dayOfWeek(wednesday - 2))
        assertEquals(7, PracticeStats.dayOfWeek(wednesday + 4))
    }

    @Test
    fun `streak counts consecutive days ending today or yesterday`() {
        val practiced = setOf(wednesday, wednesday - 1, wednesday - 2, wednesday - 4)
        assertEquals(3, PracticeStats.streak(practiced, wednesday))
        // Nothing yet today: yesterday's streak still counts.
        assertEquals(3, PracticeStats.streak(practiced, wednesday + 1))
        // A missed day breaks it.
        assertEquals(0, PracticeStats.streak(practiced, wednesday + 2))
        assertEquals(0, PracticeStats.streak(emptySet(), wednesday))
    }

    @Test
    fun `best streak is the longest run on record`() {
        val practiced = setOf(1L, 2L, 3L, 10L, 11L, 12L, 13L, 20L)
        assertEquals(4, PracticeStats.bestStreak(practiced))
        assertEquals(0, PracticeStats.bestStreak(emptySet()))
    }

    @Test
    fun `history round-trips, reads the old format, and ignores malformed entries`() {
        val history = mapOf(wednesday to DayTotal(300, 1), wednesday - 1 to DayTotal(720, 2))
        assertEquals(history, PracticeStats.parse(PracticeStats.serialize(history)))
        assertEquals(mapOf(5L to DayTotal(10, 1)), PracticeStats.parse("5:10,bad,7:,:3"))
        assertEquals(emptyMap<Long, DayTotal>(), PracticeStats.parse(null))
    }
}
