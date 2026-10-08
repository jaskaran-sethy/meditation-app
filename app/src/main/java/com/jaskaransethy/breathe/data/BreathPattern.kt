package com.jaskaransethy.breathe.data

import androidx.annotation.StringRes
import com.jaskaransethy.breathe.R
import java.net.URLDecoder
import java.net.URLEncoder

enum class Phase { Inhale, HoldIn, Exhale, HoldOut }

data class PhaseStep(val phase: Phase, val seconds: Int)

/** A breathing rhythm: one of the four [PresetPattern]s, or a [CustomPattern] the user built. */
sealed interface BreathPattern {
    /** Stable id, safe to use in routes, intents and the session log (no ':' or ','). */
    val id: String

    /** The phases of one breath, skipping holds of zero length. */
    val steps: List<PhaseStep>

    val cycleSeconds: Int get() = steps.sumOf { it.seconds }

    /** The rhythm as counts, e.g. [4, 7, 8]. */
    val counts: List<Int> get() = steps.map { it.seconds }

    /** The rhythm as text, e.g. "4 · 7 · 8". */
    val rhythm: String get() = counts.joinToString(" · ")

    companion object {
        val Default: BreathPattern = PresetPattern.Calm
    }
}

private fun phaseSteps(inhale: Int, holdIn: Int, exhale: Int, holdOut: Int): List<PhaseStep> = listOf(
    PhaseStep(Phase.Inhale, inhale),
    PhaseStep(Phase.HoldIn, holdIn),
    PhaseStep(Phase.Exhale, exhale),
    PhaseStep(Phase.HoldOut, holdOut)
).filter { it.seconds > 0 }

enum class PresetPattern(
    override val id: String,
    @StringRes val title: Int,
    @StringRes val description: Int,
    inhale: Int,
    holdIn: Int,
    exhale: Int,
    holdOut: Int
) : BreathPattern {
    Calm("calm", R.string.pattern_calm, R.string.pattern_calm_description, 4, 7, 8, 0),
    Box("box", R.string.pattern_box, R.string.pattern_box_description, 4, 4, 4, 4),
    Balance("balance", R.string.pattern_balance, R.string.pattern_balance_description, 5, 0, 5, 0),
    Sleep("sleep", R.string.pattern_sleep, R.string.pattern_sleep_description, 4, 0, 8, 0);

    override val steps: List<PhaseStep> = phaseSteps(inhale, holdIn, exhale, holdOut)

    companion object {
        fun fromId(id: String?): PresetPattern? = entries.firstOrNull { it.id == id }
    }
}

/** A rhythm the user built and named. Breathing in and out are at least a second; holds may be off. */
data class CustomPattern(
    override val id: String,
    val name: String,
    val inhale: Int,
    val holdIn: Int,
    val exhale: Int,
    val holdOut: Int
) : BreathPattern {
    override val steps: List<PhaseStep> = phaseSteps(inhale, holdIn, exhale, holdOut)

    fun serialize(): String =
        listOf(id, inhale, holdIn, exhale, holdOut, URLEncoder.encode(name, "UTF-8")).joinToString(":")

    companion object {
        const val ID_PREFIX = "custom-"
        const val MAX_NAME_LENGTH = 24
        val BreathSeconds = 1..20
        val HoldSeconds = 0..20

        /** Unique enough for one person's handful of patterns; base 36 keeps routes short. */
        fun newId(nowMillis: Long = System.currentTimeMillis()): String = ID_PREFIX + nowMillis.toString(36)

        /** Clamps counts into range and trims the name, so a saved pattern is always playable. */
        fun of(id: String, name: String, inhale: Int, holdIn: Int, exhale: Int, holdOut: Int) = CustomPattern(
            id = id,
            name = name.trim().take(MAX_NAME_LENGTH),
            inhale = inhale.coerceIn(BreathSeconds),
            holdIn = holdIn.coerceIn(HoldSeconds),
            exhale = exhale.coerceIn(BreathSeconds),
            holdOut = holdOut.coerceIn(HoldSeconds)
        )

        fun parse(entry: String): CustomPattern? {
            val p = entry.split(':')
            if (p.size != 6 || !p[0].startsWith(ID_PREFIX)) return null
            val counts = p.subList(1, 5).map { it.toIntOrNull() ?: return null }
            val name = runCatching { URLDecoder.decode(p[5], "UTF-8") }.getOrNull() ?: return null
            return of(p[0], name, counts[0], counts[1], counts[2], counts[3])
        }
    }
}
