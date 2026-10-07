package com.example.myapplication.data

/** One finished (or ended) session, as kept in the per-session log that badges are judged on. */
data class SessionRecord(
    /** Local calendar day the session started on (epoch day). */
    val day: Long,
    /** Local start time, minutes after midnight. */
    val startMinute: Int,
    val pattern: BreathPattern,
    /** The length the user chose: 2, 5 or 10. */
    val plannedMinutes: Int,
    val completedSeconds: Int,
    /** True if the session was paused at any point (including by leaving the app). */
    val paused: Boolean
) {
    /** Only full sessions count for badges: at least 90% of the chosen length. */
    val isFull: Boolean get() = completedSeconds >= plannedMinutes * 60 * FULL_FRACTION

    fun serialize(): String =
        listOf(day, startMinute, pattern.id, plannedMinutes, completedSeconds, if (paused) 1 else 0)
            .joinToString(":")

    companion object {
        const val FULL_FRACTION = 0.9

        fun parse(entry: String): SessionRecord? {
            val p = entry.split(':')
            if (p.size != 6) return null
            return SessionRecord(
                day = p[0].toLongOrNull() ?: return null,
                startMinute = p[1].toIntOrNull() ?: return null,
                pattern = BreathPattern.entries.firstOrNull { it.id == p[2] } ?: return null,
                plannedMinutes = p[3].toIntOrNull() ?: return null,
                completedSeconds = p[4].toIntOrNull() ?: return null,
                paused = p[5] == "1"
            )
        }
    }
}
