package com.jaskaransethy.breathe.data

/** Practice on one local calendar day. */
data class DayTotal(val seconds: Int, val sessions: Int) {
    /** Rounded to the nearest minute, so a two-minute session counts as 2. */
    val minutes: Int get() = (seconds + 30) / 60
}

/**
 * Practice history is kept per local calendar day, keyed by epoch day (days since 1970-01-01).
 * Plain arithmetic keeps this usable on API 24 without java.time.
 */
object PracticeStats {
    const val MS_PER_DAY = 86_400_000L

    fun epochDay(epochMillis: Long, utcOffsetMillis: Int): Long =
        Math.floorDiv(epochMillis + utcOffsetMillis, MS_PER_DAY)

    /** Monday of the week containing [day]. 1970-01-01 was a Thursday. */
    fun weekStart(day: Long): Long = day - Math.floorMod(day + 3, 7L)

    /** ISO day of week: 1 = Monday … 7 = Sunday. */
    fun dayOfWeek(day: Long): Int = Math.floorMod(day + 3, 7L).toInt() + 1

    /** Consecutive days with practice, ending today, or yesterday if today has none yet. */
    fun streak(practiced: Set<Long>, today: Long): Int {
        var day = if (today in practiced) today else today - 1
        var count = 0
        while (day in practiced) {
            count++
            day--
        }
        return count
    }

    /** Longest run of consecutive practised days on record. */
    fun bestStreak(practiced: Set<Long>): Int {
        var best = 0
        for (day in practiced) {
            if (day - 1 in practiced) continue // only count from the start of each run
            var length = 1
            while (day + length in practiced) length++
            best = maxOf(best, length)
        }
        return best
    }

    fun practicedDays(history: Map<Long, DayTotal>): Set<Long> =
        history.filterValues { it.sessions > 0 || it.seconds > 0 }.keys

    /**
     * Serialized as "day:seconds:sessions" entries. Older "day:seconds" entries count as one
     * session.
     */
    fun parse(serialized: String?): Map<Long, DayTotal> =
        serialized.orEmpty()
            .split(',')
            .mapNotNull { entry ->
                val parts = entry.split(':')
                val day = parts.getOrNull(0)?.toLongOrNull()
                val seconds = parts.getOrNull(1)?.toIntOrNull()
                val sessions = parts.getOrNull(2)?.toIntOrNull() ?: 1
                if (day != null && seconds != null) day to DayTotal(seconds, sessions) else null
            }
            .toMap()

    fun serialize(history: Map<Long, DayTotal>): String =
        history.entries.sortedBy { it.key }
            .joinToString(",") { "${it.key}:${it.value.seconds}:${it.value.sessions}" }
}
