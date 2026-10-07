package com.example.myapplication.reminder

import com.example.myapplication.data.PracticeStats
import com.example.myapplication.data.ReminderSettings
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReminderSchedulerTest {
    private val original = TimeZone.getDefault()

    // 2026-10-07 (a Wednesday) at 00:00 UTC.
    private val wednesday = 20_733L
    private val wednesdayMidnight = wednesday * PracticeStats.MS_PER_DAY
    private val hour = 3_600_000L

    @Before
    fun useUtc() = TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

    @After
    fun restore() = TimeZone.setDefault(original)

    @Test
    fun `fires later today when the time has not passed`() {
        val settings = ReminderSettings(enabled = true, hour = 21, minute = 0)
        val next = ReminderScheduler.nextTrigger(settings, wednesdayMidnight + 8 * hour, Long.MIN_VALUE)
        assertEquals(wednesdayMidnight + 21 * hour, next)
    }

    @Test
    fun `moves to tomorrow once today's time has passed or today is handled`() {
        val settings = ReminderSettings(enabled = true, hour = 9, minute = 30)
        val tomorrow = wednesdayMidnight + PracticeStats.MS_PER_DAY + 9 * hour + 30 * 60_000L
        assertEquals(tomorrow, ReminderScheduler.nextTrigger(settings, wednesdayMidnight + 10 * hour, Long.MIN_VALUE))
        assertEquals(tomorrow, ReminderScheduler.nextTrigger(settings, wednesdayMidnight + 8 * hour, wednesday))
    }

    @Test
    fun `skips days that are not selected`() {
        // Weekends only; Wednesday → Saturday.
        val settings = ReminderSettings(enabled = true, hour = 21, minute = 0, days = setOf(6, 7))
        val next = ReminderScheduler.nextTrigger(settings, wednesdayMidnight + 8 * hour, Long.MIN_VALUE)
        assertEquals(wednesdayMidnight + 3 * PracticeStats.MS_PER_DAY + 21 * hour, next)
    }
}
