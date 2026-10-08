package com.example.myapplication.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgeRulesTest {

    private val today = 20_733L

    private fun rec(
        day: Long = today,
        pattern: BreathPattern = PresetPattern.Calm,
        minutes: Int = 5,
        seconds: Int = minutes * 60,
        paused: Boolean = false,
        startMinute: Int = 9 * 60
    ) = SessionRecord(day, startMinute, pattern.id, minutes, seconds, paused)

    private fun progress(records: List<SessionRecord>, badge: Badge, earned: Map<String, Long> = emptyMap()) =
        BadgeRules.evaluate(records, today, earned).single { it.badge == badge }

    private fun earned(records: List<SessionRecord>, badge: Badge) = progress(records, badge).earned

    @Test
    fun `full session threshold is 90 percent of the chosen length`() {
        assertTrue(rec(minutes = 5, seconds = 270).isFull)
        assertFalse(rec(minutes = 5, seconds = 269).isFull)
        // Partial sessions count for nothing.
        val partial = List(10) { rec(day = today - it % 3, pattern = PresetPattern.Box, seconds = 269) }
        assertTrue(BadgeRules.newlyEarned(partial, today, emptyMap()).isEmpty())
        assertEquals(0, progress(partial, Badge.SquareMind).current)
        assertEquals(0, progress(partial, Badge.DeepWater).current)
    }

    @Test
    fun `evaluate returns every badge in order with its target`() {
        val all = BadgeRules.evaluate(emptyList(), today, emptyMap())
        assertEquals(Badge.entries.toList(), all.map { it.badge })
        assertEquals(listOf(3, 1, 7, 5, 4, 100, 10, 30), all.map { it.target })
        assertTrue(all.none { it.earned || it.current != 0 || it.earnedOn != null })
    }

    @Test
    fun `steady start needs 3 full sessions within any 7 days`() {
        val spread = listOf(rec(today - 7), rec(today - 3), rec(today))
        assertFalse(earned(spread, Badge.SteadyStart))
        assertEquals(2, progress(spread, Badge.SteadyStart).current)

        val inWindow = listOf(rec(today - 6), rec(today - 3), rec(today))
        assertTrue(earned(inWindow, Badge.SteadyStart))

        // Several sessions on one day count separately.
        assertTrue(earned(List(3) { rec(today) }, Badge.SteadyStart))

        // Met in an old window: earned even though the last 7 days are empty.
        val old = List(3) { rec(today - 100 + it) }
        val p = progress(old, Badge.SteadyStart)
        assertTrue(p.earned)
        assertEquals(0, p.current)
        assertEquals(today, p.earnedOn)
    }

    @Test
    fun `unbroken needs a full 10 minute session without pausing`() {
        assertFalse(earned(listOf(rec(minutes = 5)), Badge.Unbroken))
        assertFalse(earned(listOf(rec(minutes = 10, paused = true)), Badge.Unbroken))
        assertFalse(earned(listOf(rec(minutes = 10, seconds = 539)), Badge.Unbroken))
        assertTrue(earned(listOf(rec(minutes = 10, seconds = 540)), Badge.Unbroken))
        assertEquals(1, progress(listOf(rec(minutes = 10)), Badge.Unbroken).current)
    }

    @Test
    fun `seven days needs 7 distinct days within any 14 day window`() {
        val sixDays = (0L until 6L).map { rec(today - it * 2) }
        assertFalse(earned(sixDays, Badge.SevenDays))
        assertEquals(6, progress(sixDays, Badge.SevenDays).current)

        // Days 0, 2, … 12 span 13 days: inside a 14-day window.
        val sevenIn14 = (0L until 7L).map { rec(today - it * 2) }
        assertTrue(earned(sevenIn14, Badge.SevenDays))

        // 7 days spread over 15 days (0 … 14) misses.
        val sevenIn15 = listOf(0L, 2, 4, 6, 8, 10, 14).map { rec(today - it) }
        assertFalse(earned(sevenIn15, Badge.SevenDays))

        // Repeat sessions on one day are one day.
        val repeats = List(10) { rec(today) }
        assertEquals(1, progress(repeats, Badge.SevenDays).current)
        assertFalse(earned(repeats, Badge.SevenDays))

        // Only days in the last 14 count towards live progress.
        val old = listOf(rec(today - 14), rec(today - 13))
        assertEquals(1, progress(old, Badge.SevenDays).current)
    }

    @Test
    fun `night tide needs 5 full sleep sessions started after 9 pm`() {
        val night = List(4) { rec(pattern = PresetPattern.Sleep, startMinute = 21 * 60) }
        assertFalse(earned(night, Badge.NightTide))
        assertEquals(4, progress(night, Badge.NightTide).current)

        assertTrue(earned(night + rec(pattern = PresetPattern.Sleep, startMinute = 23 * 60 + 59), Badge.NightTide))
        // Just after midnight and up to 03:59 still counts as night.
        assertTrue(earned(night + rec(pattern = PresetPattern.Sleep, startMinute = 0), Badge.NightTide))
        assertTrue(earned(night + rec(pattern = PresetPattern.Sleep, startMinute = 3 * 60 + 59), Badge.NightTide))
        // 20:59, 04:00, another pattern, or a partial session do not.
        assertFalse(earned(night + rec(pattern = PresetPattern.Sleep, startMinute = 20 * 60 + 59), Badge.NightTide))
        assertFalse(earned(night + rec(pattern = PresetPattern.Sleep, startMinute = 4 * 60), Badge.NightTide))
        assertFalse(earned(night + rec(pattern = PresetPattern.Calm, startMinute = 22 * 60), Badge.NightTide))
        assertFalse(
            earned(night + rec(pattern = PresetPattern.Sleep, startMinute = 22 * 60, seconds = 100), Badge.NightTide)
        )
    }

    @Test
    fun `four winds needs each pattern completed 3 times`() {
        val three = listOf(PresetPattern.Calm, PresetPattern.Box, PresetPattern.Balance)
            .flatMap { p -> List(3) { rec(pattern = p) } }
        val twoSleep = List(2) { rec(pattern = PresetPattern.Sleep) }
        assertFalse(earned(three + twoSleep, Badge.FourWinds))
        assertEquals(3, progress(three + twoSleep, Badge.FourWinds).current)
        assertTrue(earned(three + twoSleep + rec(pattern = PresetPattern.Sleep), Badge.FourWinds))
        assertEquals(
            mapOf(PresetPattern.Calm to 3, PresetPattern.Box to 3, PresetPattern.Balance to 3, PresetPattern.Sleep to 2),
            BadgeRules.fullSessionsByPattern(three + twoSleep + rec(pattern = PresetPattern.Sleep, seconds = 10))
        )
    }

    @Test
    fun `deep water needs 100 minutes in full sessions, rounded down`() {
        val almost = List(9) { rec(minutes = 10) } + rec(minutes = 10, seconds = 599)
        assertEquals(99, progress(almost, Badge.DeepWater).current)
        assertFalse(earned(almost, Badge.DeepWater))
        assertTrue(earned(List(10) { rec(minutes = 10) }, Badge.DeepWater))
        // Partial sessions add nothing.
        assertEquals(10, progress(listOf(rec(minutes = 10), rec(minutes = 10, seconds = 500)), Badge.DeepWater).current)
        // Progress is capped at the target.
        assertEquals(100, progress(List(30) { rec(minutes = 10) }, Badge.DeepWater).current)
    }

    @Test
    fun `square mind needs 10 full box sessions`() {
        val nine = List(9) { rec(pattern = PresetPattern.Box) }
        assertFalse(earned(nine, Badge.SquareMind))
        assertFalse(earned(nine + rec(pattern = PresetPattern.Box, seconds = 200), Badge.SquareMind))
        assertTrue(earned(nine + rec(pattern = PresetPattern.Box), Badge.SquareMind))
    }

    @Test
    fun `mountain needs 30 distinct days within any 60 day window`() {
        // Every other day: days 0, 2, … 58 is 30 days spanning 59 days.
        val everyOther = (0L until 30L).map { rec(today - it * 2) }
        assertTrue(earned(everyOther, Badge.Mountain))
        assertEquals(30, progress(everyOther, Badge.Mountain).current)

        // Stretched to 61 days: misses.
        val stretched = (0L until 29L).map { rec(today - it * 2) } + rec(today - 60)
        assertFalse(earned(stretched, Badge.Mountain))
        assertEquals(29, progress(stretched, Badge.Mountain).current)

        // Met long ago, never lost.
        val longAgo = (0L until 30L).map { rec(today - 200 - it) }
        assertTrue(earned(longAgo, Badge.Mountain))
        assertEquals(0, progress(longAgo, Badge.Mountain).current)
    }

    @Test
    fun `earned badges are never lost and keep their day`() {
        val map = mapOf(Badge.SquareMind.id to today - 40, Badge.Mountain.id to today - 3)
        val p = progress(emptyList(), Badge.SquareMind, map)
        assertTrue(p.earned)
        assertEquals(today - 40, p.earnedOn)
        assertEquals(0, p.current)
        assertEquals(1f, p.fraction)
        assertEquals(0, p.remaining)
        assertTrue(BadgeRules.newlyEarned(emptyList(), today, map).isEmpty())
        assertNull(progress(emptyList(), Badge.Unbroken, map).earnedOn)
    }

    @Test
    fun `newly earned lists only badges met now and not yet stored`() {
        val records = List(3) { rec(minutes = 10) }
        assertEquals(
            listOf(Badge.SteadyStart, Badge.Unbroken),
            BadgeRules.newlyEarned(records, today, emptyMap())
        )
        assertEquals(
            listOf(Badge.Unbroken),
            BadgeRules.newlyEarned(records, today, mapOf(Badge.SteadyStart.id to today - 1))
        )
        val p = progress(records, Badge.Unbroken)
        assertEquals(today, p.earnedOn)
    }

    @Test
    fun `closest sorts unearned by fraction then remaining`() {
        fun bp(badge: Badge, current: Int, target: Int, earned: Boolean = false) =
            BadgeProgress(badge, current, target, earned, if (earned) today else null)
        val list = listOf(
            bp(Badge.SteadyStart, 3, 3, earned = true),
            bp(Badge.Unbroken, 0, 1),
            bp(Badge.SevenDays, 6, 7),
            bp(Badge.NightTide, 4, 5),
            bp(Badge.FourWinds, 3, 4),
            bp(Badge.DeepWater, 85, 100),
            bp(Badge.SquareMind, 8, 10),
            bp(Badge.Mountain, 24, 30)
        )
        // 6/7 = .857, 85/100 = .85; 4/5, 8/10 and 24/30 tie at .8 and are ordered by what is left.
        assertEquals(
            listOf(Badge.SevenDays, Badge.DeepWater, Badge.NightTide, Badge.SquareMind, Badge.Mountain),
            BadgeRules.closest(list, 5).map { it.badge }
        )
        assertEquals(3, BadgeRules.closest(list).size)
        assertTrue(BadgeRules.closest(list.map { it.copy(earned = true) }).isEmpty())
    }
}
