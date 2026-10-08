package com.jaskaransethy.breathe.data

import android.content.Context
import java.util.TimeZone

enum class Mood(val key: String) {
    Calmer("calmer"), Same("same"), StillTense("still_tense");

    companion object {
        fun fromKey(key: String): Mood? = entries.firstOrNull { it.key == key }
    }
}

/** Reduce motion follows the OS by default, with an override in Settings. */
enum class ReduceMotion(val key: String) {
    System("system"), On("on"), Off("off");

    companion object {
        fun fromKey(key: String?): ReduceMotion = entries.firstOrNull { it.key == key } ?: System
    }
}

data class ReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = 21,
    val minute: Int = 0,
    /** ISO days of week, 1 = Monday … 7 = Sunday. */
    val days: Set<Int> = (1..7).toSet()
)

data class PracticeSummary(
    val streakDays: Int,
    val minutesThisWeek: Int,
    val totalSessions: Int
)

/** Everything the Progress tab shows, for the week containing today. */
data class WeekProgress(
    /** Epoch day of this week's Monday. */
    val weekStart: Long,
    val today: Long,
    /** Seven entries, Monday to Sunday; days without practice have zero totals. */
    val days: List<DayTotal>,
    val streakDays: Int,
    val bestStreakDays: Int,
    val minutesThisWeek: Int,
    val sessionsThisWeek: Int,
    /** Answers from the Complete screen's mood check this week, oldest first. */
    val moods: List<Mood>
)

/** Small persistent store for onboarding, settings, and practice history. */
class BreatheStore(context: Context) {
    private val prefs = context.getSharedPreferences("breathe", Context.MODE_PRIVATE)

    var onboardingComplete: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, value).apply()

    var lastPattern: BreathPattern
        get() = BreathPattern.fromId(prefs.getString(KEY_LAST_PATTERN, null))
        set(value) = prefs.edit().putString(KEY_LAST_PATTERN, value.id).apply()

    /** The lengths on Home's length bar, shortest first. Edited from Home or Settings. */
    var sessionLengths: List<Int>
        get() = SessionLengths.parse(prefs.getString(KEY_SESSION_LENGTHS, null))
        set(value) = prefs.edit().putString(KEY_SESSION_LENGTHS, SessionLengths.serialize(value)).apply()

    /**
     * Pre-selects the length on Home. Changed from Home or from Settings → Default length. If the
     * saved length was since removed from [sessionLengths], the nearest remaining one is used.
     */
    var lastMinutes: Int
        get() = SessionLengths.closest(sessionLengths, prefs.getInt(KEY_LAST_MINUTES, DefaultSessionMinutes))
        set(value) = prefs.edit().putInt(KEY_LAST_MINUTES, value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, value).apply()

    var keepScreenAwake: Boolean
        get() = prefs.getBoolean(KEY_KEEP_AWAKE, true)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_AWAKE, value).apply()

    var reduceMotion: ReduceMotion
        get() = ReduceMotion.fromKey(prefs.getString(KEY_REDUCE_MOTION, null))
        set(value) = prefs.edit().putString(KEY_REDUCE_MOTION, value.key).apply()

    var reminder: ReminderSettings
        get() = ReminderSettings(
            enabled = prefs.getBoolean(KEY_REMINDER_ENABLED, false),
            hour = prefs.getInt(KEY_REMINDER_HOUR, 21),
            minute = prefs.getInt(KEY_REMINDER_MINUTE, 0),
            days = prefs.getString(KEY_REMINDER_DAYS, null)
                ?.split(',')?.mapNotNull { it.toIntOrNull() }?.filter { it in 1..7 }?.toSet()
                ?: (1..7).toSet()
        )
        set(value) = prefs.edit()
            .putBoolean(KEY_REMINDER_ENABLED, value.enabled)
            .putInt(KEY_REMINDER_HOUR, value.hour)
            .putInt(KEY_REMINDER_MINUTE, value.minute)
            .putString(KEY_REMINDER_DAYS, value.days.sorted().joinToString(","))
            .apply()

    /** Epoch day the reminder was last shown, snoozed past, or skipped — at most one a day. */
    var reminderHandledDay: Long
        get() = prefs.getLong(KEY_REMINDER_HANDLED_DAY, Long.MIN_VALUE)
        set(value) = prefs.edit().putLong(KEY_REMINDER_HANDLED_DAY, value).apply()

    /** Consecutive days the reminder was skipped; reset by starting a session. */
    var reminderSkipStreak: Int
        get() = prefs.getInt(KEY_REMINDER_SKIPS, 0)
        set(value) = prefs.edit().putInt(KEY_REMINDER_SKIPS, value).apply()

    /** Which breathing visual the session shows. */
    var guideStyle: GuideStyle
        get() = GuideStyle.fromId(prefs.getString(KEY_GUIDE_STYLE, null))
        set(value) = prefs.edit().putString(KEY_GUIDE_STYLE, value.id).apply()

    /** Badge id → epoch day earned. Badges are never lost once earned. */
    val earnedBadges: Map<String, Long>
        get() = prefs.getString(KEY_EARNED_BADGES, null).orEmpty().split(',').mapNotNull { entry ->
            val parts = entry.split(':')
            val day = parts.getOrNull(1)?.toLongOrNull()
            if (parts.size == 2 && day != null) parts[0] to day else null
        }.toMap()

    fun markEarned(badges: Collection<Badge>, nowMillis: Long = System.currentTimeMillis()) {
        if (badges.isEmpty()) return
        val today = today(nowMillis)
        val all = earnedBadges + badges.filter { it.id !in earnedBadges }.associate { it.id to today }
        prefs.edit().putString(KEY_EARNED_BADGES, all.entries.joinToString(",") { "${it.key}:${it.value}" }).apply()
    }

    /** Every logged session, oldest first. Kept in full: badges look back up to 60 days and more. */
    fun sessionLog(): List<SessionRecord> =
        prefs.getString(KEY_SESSION_LOG, null).orEmpty().split(',').mapNotNull(SessionRecord::parse)

    /**
     * Records a session that reached its end, or was closed after at least 90% of its length.
     * Updates the daily history (streaks, weekly minutes) and the per-session log (badges).
     */
    fun recordSession(record: SessionRecord) {
        val history = history().toMutableMap()
        val current = history[record.day] ?: DayTotal(0, 0)
        history[record.day] = DayTotal(current.seconds + record.completedSeconds, current.sessions + 1)
        // A year is plenty for streaks and weekly totals.
        history.keys.removeAll { it < record.day - 366 }
        val log = prefs.getString(KEY_SESSION_LOG, null).orEmpty()
        prefs.edit()
            .putString(KEY_HISTORY, PracticeStats.serialize(history))
            .putString(KEY_SESSION_LOG, if (log.isEmpty()) record.serialize() else "$log,${record.serialize()}")
            .putInt(KEY_TOTAL_SESSIONS, prefs.getInt(KEY_TOTAL_SESSIONS, 0) + 1)
            .putInt(KEY_REMINDER_SKIPS, 0)
            .apply()
    }

    /** Builds a record for a session that started at [startMillis], stamped in local time. */
    fun sessionRecord(
        pattern: BreathPattern,
        plannedMinutes: Int,
        completedSeconds: Int,
        paused: Boolean,
        startMillis: Long
    ): SessionRecord {
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = startMillis }
        return SessionRecord(
            day = today(startMillis),
            startMinute = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + calendar.get(java.util.Calendar.MINUTE),
            pattern = pattern,
            plannedMinutes = plannedMinutes,
            completedSeconds = completedSeconds,
            paused = paused
        )
    }

    fun recordMood(mood: Mood, nowMillis: Long = System.currentTimeMillis()) {
        val today = today(nowMillis)
        val log = moodLog() + (today to mood)
        prefs.edit()
            .putString(KEY_MOODS, log.takeLast(MAX_MOODS).joinToString(",") { "${it.first}:${it.second.key}" })
            .apply()
    }

    fun summary(nowMillis: Long = System.currentTimeMillis()): PracticeSummary {
        val today = today(nowMillis)
        val history = history()
        val start = PracticeStats.weekStart(today)
        return PracticeSummary(
            streakDays = PracticeStats.streak(PracticeStats.practicedDays(history), today),
            minutesThisWeek = history.filterKeys { it in start..today }.values.sumOf { it.minutes },
            totalSessions = prefs.getInt(KEY_TOTAL_SESSIONS, 0)
        )
    }

    fun weekProgress(nowMillis: Long = System.currentTimeMillis()): WeekProgress {
        val today = today(nowMillis)
        val start = PracticeStats.weekStart(today)
        val history = history()
        val practiced = PracticeStats.practicedDays(history)
        val days = (0 until 7).map { history[start + it] ?: DayTotal(0, 0) }
        return WeekProgress(
            weekStart = start,
            today = today,
            days = days,
            streakDays = PracticeStats.streak(practiced, today),
            bestStreakDays = PracticeStats.bestStreak(practiced),
            minutesThisWeek = days.sumOf { it.minutes },
            sessionsThisWeek = days.sumOf { it.sessions },
            moods = moodLog().filter { it.first in start..today }.map { it.second }
        )
    }

    fun today(nowMillis: Long = System.currentTimeMillis()): Long =
        PracticeStats.epochDay(nowMillis, TimeZone.getDefault().getOffset(nowMillis))

    fun practicedOn(day: Long): Boolean = (history()[day]?.sessions ?: 0) > 0

    private fun history(): Map<Long, DayTotal> = PracticeStats.parse(prefs.getString(KEY_HISTORY, null))

    private fun moodLog(): List<Pair<Long, Mood>> =
        prefs.getString(KEY_MOODS, null).orEmpty().split(',').mapNotNull { entry ->
            val parts = entry.split(':')
            val day = parts.getOrNull(0)?.toLongOrNull()
            val mood = parts.getOrNull(1)?.let(Mood::fromKey)
            if (day != null && mood != null) day to mood else null
        }

    private companion object {
        const val MAX_MOODS = 60
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        const val KEY_LAST_PATTERN = "last_pattern"
        const val KEY_LAST_MINUTES = "last_minutes"
        const val KEY_SESSION_LENGTHS = "session_lengths"
        const val KEY_SOUND_ENABLED = "sound_enabled"
        const val KEY_HAPTICS_ENABLED = "haptics_enabled"
        const val KEY_KEEP_AWAKE = "keep_screen_awake"
        const val KEY_REDUCE_MOTION = "reduce_motion"
        const val KEY_REMINDER_ENABLED = "reminder_enabled"
        const val KEY_REMINDER_HOUR = "reminder_hour"
        const val KEY_REMINDER_MINUTE = "reminder_minute"
        const val KEY_REMINDER_DAYS = "reminder_days"
        const val KEY_REMINDER_HANDLED_DAY = "reminder_handled_day"
        const val KEY_REMINDER_SKIPS = "reminder_skip_streak"
        const val KEY_HISTORY = "history"
        const val KEY_TOTAL_SESSIONS = "total_sessions"
        const val KEY_MOODS = "moods"
        const val KEY_GUIDE_STYLE = "guide_style"
        const val KEY_EARNED_BADGES = "earned_badges"
        const val KEY_SESSION_LOG = "session_log"
    }
}
