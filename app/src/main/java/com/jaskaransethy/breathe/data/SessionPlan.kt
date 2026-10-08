package com.jaskaransethy.breathe.data

import kotlin.math.ceil

/**
 * A session is a whole number of breaths that covers at least the chosen length, so it always
 * ends after an exhale rather than mid-breath.
 */
class SessionPlan(val pattern: BreathPattern, val minutes: Int) {
    val cycleMs: Long = pattern.cycleSeconds * 1000L
    val breaths: Int = ceil(minutes * 60_000.0 / cycleMs).toInt().coerceAtLeast(1)
    val totalMs: Long = breaths * cycleMs

    fun stateAt(elapsedMs: Long): SessionState {
        val clamped = elapsedMs.coerceIn(0, totalMs)
        val finished = clamped >= totalMs
        // On the final frame, report the end of the last exhale instead of wrapping to a new breath.
        val inSession = if (finished) totalMs - 1 else clamped
        val breathIndex = (inSession / cycleMs).toInt()
        var intoCycle = inSession % cycleMs

        var stepIndex = 0
        while (intoCycle >= pattern.steps[stepIndex].seconds * 1000L) {
            intoCycle -= pattern.steps[stepIndex].seconds * 1000L
            stepIndex++
        }
        val stepMs = pattern.steps[stepIndex].seconds * 1000L

        return SessionState(
            breathIndex = breathIndex,
            stepIndex = stepIndex,
            step = pattern.steps[stepIndex],
            phaseProgress = if (finished) 1f else intoCycle.toFloat() / stepMs,
            secondsLeftInPhase = if (finished) 0 else ceil((stepMs - intoCycle) / 1000.0).toInt(),
            elapsedMs = clamped,
            remainingMs = totalMs - clamped,
            finished = finished
        )
    }
}

data class SessionState(
    /** Zero-based index of the current breath. */
    val breathIndex: Int,
    val stepIndex: Int,
    val step: PhaseStep,
    /** 0..1 through the current phase. */
    val phaseProgress: Float,
    val secondsLeftInPhase: Int,
    val elapsedMs: Long,
    val remainingMs: Long,
    val finished: Boolean
)
