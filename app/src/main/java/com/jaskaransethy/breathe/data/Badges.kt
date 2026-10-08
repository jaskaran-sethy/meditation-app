package com.jaskaransethy.breathe.data

// Badge rules from the design ("Badges & progression"). Only full sessions count, mood answers
// are never consulted, and an earned badge is never lost. Consistency is judged in forgiving
// windows ("7 days within 14"), not unbroken streaks.

/** The eight badges from the design ("Badges & progression → The eight badges"). */
enum class Badge(val id: String) {
    SteadyStart("steady_start"),
    Unbroken("unbroken"),
    SevenDays("seven_days"),
    NightTide("night_tide"),
    FourWinds("four_winds"),
    DeepWater("deep_water"),
    SquareMind("square_mind"),
    Mountain("mountain");

    companion object {
        fun fromId(id: String): Badge? = entries.firstOrNull { it.id == id }
    }
}

/** Live progress towards one badge. */
data class BadgeProgress(
    val badge: Badge,
    /** Progress in the badge's own unit (sessions, days, minutes or patterns). */
    val current: Int,
    val target: Int,
    /** Earned badges are never lost, even if the current count later drops. */
    val earned: Boolean,
    /** Epoch day it was earned, if earned. */
    val earnedOn: Long?
) {
    val fraction: Float get() = if (earned) 1f else (current.toFloat() / target).coerceIn(0f, 1f)
    val remaining: Int get() = if (earned) 0 else (target - current).coerceAtLeast(0)
}

/**
 * The rules for each badge. Steady Start: the design says "3 full sessions within your first 7
 * days"; here any 7-day window counts, so people who started before badges existed can still
 * earn it. Window rules are met if ANY window in the history qualifies, not only the latest.
 */
object BadgeRules {
    const val STEADY_START_SESSIONS = 3
    const val STEADY_START_WINDOW_DAYS = 7
    /** Unbroken needs a session at least this long; longer custom lengths count too. */
    const val UNBROKEN_MINUTES = 10
    const val SEVEN_DAYS_DAYS = 7
    const val SEVEN_DAYS_WINDOW_DAYS = 14
    const val NIGHT_TIDE_SESSIONS = 5
    /** Sessions started at or after 21:00, or before 04:00, count as night. */
    const val NIGHT_STARTS_MINUTE = 21 * 60
    const val NIGHT_ENDS_MINUTE = 4 * 60
    const val FOUR_WINDS_PER_PATTERN = 3
    const val DEEP_WATER_MINUTES = 100
    const val SQUARE_MIND_SESSIONS = 10
    const val MOUNTAIN_DAYS = 30
    const val MOUNTAIN_WINDOW_DAYS = 60

    /** The number each badge counts up to, in its own unit. */
    fun target(badge: Badge): Int = when (badge) {
        Badge.SteadyStart -> STEADY_START_SESSIONS
        Badge.Unbroken -> 1
        Badge.SevenDays -> SEVEN_DAYS_DAYS
        Badge.NightTide -> NIGHT_TIDE_SESSIONS
        Badge.FourWinds -> PresetPattern.entries.size
        Badge.DeepWater -> DEEP_WATER_MINUTES
        Badge.SquareMind -> SQUARE_MIND_SESSIONS
        Badge.Mountain -> MOUNTAIN_DAYS
    }

    /**
     * The look-back window, in days ending today, that a window badge's live progress (and the
     * Badge Earned proof) is measured over; null for badges that count all-time.
     */
    fun windowDays(badge: Badge): Int? = when (badge) {
        Badge.SteadyStart -> STEADY_START_WINDOW_DAYS
        Badge.SevenDays -> SEVEN_DAYS_WINDOW_DAYS
        Badge.Mountain -> MOUNTAIN_WINDOW_DAYS
        else -> null
    }

    /**
     * Progress for every badge, in [Badge] order. [earned] maps badge id → epoch day it was
     * earned (from [BreatheStore.earnedBadges]); those stay earned regardless of [records].
     */
    fun evaluate(records: List<SessionRecord>, today: Long, earned: Map<String, Long>): List<BadgeProgress> {
        val full = records.filter { it.isFull }
        return Badge.entries.map { badge ->
            val target = target(badge)
            val met = isMet(badge, full)
            val earnedOn = earned[badge.id] ?: if (met) today else null
            BadgeProgress(
                badge = badge,
                current = current(badge, full, today).coerceIn(0, target),
                target = target,
                earned = earnedOn != null,
                earnedOn = earnedOn
            )
        }
    }

    /** Badges whose rules are met by [records] but are not yet in [earned]. */
    fun newlyEarned(records: List<SessionRecord>, today: Long, earned: Map<String, Long>): List<Badge> {
        val full = records.filter { it.isFull }
        return Badge.entries.filter { it.id !in earned && isMet(it, full) }
    }

    /** The three unearned badges closest to completion, closest first (for Progress). */
    fun closest(progress: List<BadgeProgress>, count: Int = 3): List<BadgeProgress> =
        progress.filter { !it.earned }
            .sortedWith(compareByDescending<BadgeProgress> { it.fraction }.thenBy { it.remaining })
            .take(count)

    /**
     * Full sessions per preset, including presets with none (Four Winds' breakdown). The user's
     * own patterns count towards every other badge, but Four Winds is about the four presets.
     */
    fun fullSessionsByPattern(records: List<SessionRecord>): Map<PresetPattern, Int> {
        val counts = records.filter { it.isFull }.groupingBy { it.patternId }.eachCount()
        return PresetPattern.entries.associateWith { counts[it.id] ?: 0 }
    }

    /** True for a session started after 9 pm, or in the small hours before 4 am. */
    fun isNight(record: SessionRecord): Boolean =
        record.startMinute >= NIGHT_STARTS_MINUTE || record.startMinute < NIGHT_ENDS_MINUTE

    /** Minutes breathed in the full sessions among [records], rounded down. */
    fun fullMinutes(records: List<SessionRecord>): Int =
        records.filter { it.isFull }.sumOf { it.completedSeconds } / 60

    // --- Rules. Every function below receives full sessions only. ---

    private fun isMet(badge: Badge, full: List<SessionRecord>): Boolean = when (badge) {
        Badge.SteadyStart ->
            bestWindow(full.map { it.day }, STEADY_START_WINDOW_DAYS) >= STEADY_START_SESSIONS
        Badge.Unbroken -> full.any { isUnbroken(it) }
        Badge.SevenDays ->
            bestWindow(full.map { it.day }.distinct(), SEVEN_DAYS_WINDOW_DAYS) >= SEVEN_DAYS_DAYS
        Badge.NightTide -> full.count { isNightTide(it) } >= NIGHT_TIDE_SESSIONS
        Badge.FourWinds -> patternsDone(full) >= PresetPattern.entries.size
        Badge.DeepWater -> fullMinutes(full) >= DEEP_WATER_MINUTES
        Badge.SquareMind -> full.count { it.patternId == PresetPattern.Box.id } >= SQUARE_MIND_SESSIONS
        Badge.Mountain ->
            bestWindow(full.map { it.day }.distinct(), MOUNTAIN_WINDOW_DAYS) >= MOUNTAIN_DAYS
    }

    private fun current(badge: Badge, full: List<SessionRecord>, today: Long): Int = when (badge) {
        Badge.SteadyStart -> inLastDays(full, today, STEADY_START_WINDOW_DAYS).size
        Badge.Unbroken -> if (full.any { isUnbroken(it) }) 1 else 0
        Badge.SevenDays -> inLastDays(full, today, SEVEN_DAYS_WINDOW_DAYS).map { it.day }.distinct().size
        Badge.NightTide -> full.count { isNightTide(it) }
        Badge.FourWinds -> patternsDone(full)
        Badge.DeepWater -> fullMinutes(full)
        Badge.SquareMind -> full.count { it.patternId == PresetPattern.Box.id }
        Badge.Mountain -> inLastDays(full, today, MOUNTAIN_WINDOW_DAYS).map { it.day }.distinct().size
    }

    /** Records from the [days] days ending today (today − days + 1 … today). */
    fun inLastDays(records: List<SessionRecord>, today: Long, days: Int): List<SessionRecord> =
        records.filter { it.day in (today - days + 1)..today }

    fun isUnbroken(record: SessionRecord): Boolean =
        record.plannedMinutes >= UNBROKEN_MINUTES && record.isFull && !record.paused

    private fun isNightTide(record: SessionRecord) =
        record.patternId == PresetPattern.Sleep.id && record.isFull && isNight(record)

    private fun patternsDone(full: List<SessionRecord>): Int =
        fullSessionsByPattern(full).values.count { it >= FOUR_WINDS_PER_PATTERN }

    /** The most entries of [days] that fall inside any window of [length] consecutive days. */
    private fun bestWindow(days: List<Long>, length: Int): Int {
        val sorted = days.sorted()
        var best = 0
        var start = 0
        for (end in sorted.indices) {
            while (sorted[end] - sorted[start] >= length) start++
            best = maxOf(best, end - start + 1)
        }
        return best
    }
}
