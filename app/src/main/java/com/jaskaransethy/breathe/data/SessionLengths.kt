package com.jaskaransethy.breathe.data

import kotlin.math.abs

const val DefaultSessionMinutes = 5

/**
 * The lengths offered on Home's length bar, in minutes. People can swap the defaults for their
 * own from Settings → Length options, or by long-pressing the bar. There are always [COUNT]
 * different lengths, shortest first.
 */
object SessionLengths {
    const val COUNT = 3
    val Default = listOf(2, 5, 10)

    /** What the editor steps through: every minute up to 10, then coarser. */
    val Steps = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20, 25, 30, 40, 45, 60)

    /** Saved lengths, or [Default] if nothing valid was saved. */
    fun parse(raw: String?): List<Int> {
        val lengths = raw.orEmpty().split(',').mapNotNull { it.trim().toIntOrNull() }
        return if (isValid(lengths)) lengths.sorted() else Default
    }

    fun serialize(lengths: List<Int>): String = lengths.sorted().joinToString(",")

    fun isValid(lengths: List<Int>): Boolean =
        lengths.size == COUNT && lengths.distinct().size == COUNT && lengths.all { it in Steps }

    /**
     * The next step above (or below) [minutes] that isn't in [taken], so two slots can never
     * hold the same length. Null at the end of the range.
     */
    fun step(minutes: Int, longer: Boolean, taken: Collection<Int> = emptyList()): Int? {
        val candidates = Steps.filter { it !in taken }
        return if (longer) candidates.firstOrNull { it > minutes } else candidates.lastOrNull { it < minutes }
    }

    /** [minutes] if it's one of [lengths], otherwise the nearest of them (the shorter on a tie). */
    fun closest(lengths: List<Int>, minutes: Int): Int =
        lengths.sorted().minBy { abs(it - minutes) }
}
