package com.jaskaransethy.breathe.data

import androidx.annotation.StringRes
import com.jaskaransethy.breathe.R

enum class Phase { Inhale, HoldIn, Exhale, HoldOut }

data class PhaseStep(val phase: Phase, val seconds: Int)

enum class BreathPattern(
    val id: String,
    @StringRes val title: Int,
    @StringRes val description: Int,
    inhale: Int,
    holdIn: Int,
    exhale: Int,
    holdOut: Int
) {
    Calm("calm", R.string.pattern_calm, R.string.pattern_calm_description, 4, 7, 8, 0),
    Box("box", R.string.pattern_box, R.string.pattern_box_description, 4, 4, 4, 4),
    Balance("balance", R.string.pattern_balance, R.string.pattern_balance_description, 5, 0, 5, 0),
    Sleep("sleep", R.string.pattern_sleep, R.string.pattern_sleep_description, 4, 0, 8, 0);

    /** The phases of one breath, skipping holds of zero length. */
    val steps: List<PhaseStep> = listOf(
        PhaseStep(Phase.Inhale, inhale),
        PhaseStep(Phase.HoldIn, holdIn),
        PhaseStep(Phase.Exhale, exhale),
        PhaseStep(Phase.HoldOut, holdOut)
    ).filter { it.seconds > 0 }

    val cycleSeconds: Int = steps.sumOf { it.seconds }

    /** The rhythm as counts, e.g. [4, 7, 8]. */
    val counts: List<Int> = steps.map { it.seconds }

    /** The rhythm as text, e.g. "4 · 7 · 8". */
    val rhythm: String = counts.joinToString(" · ")

    companion object {
        val Default = Calm

        fun fromId(id: String?): BreathPattern = entries.firstOrNull { it.id == id } ?: Default
    }
}

/** Session lengths offered on Home, in minutes. */
val SessionLengths = listOf(2, 5, 10)
const val DefaultSessionMinutes = 5
