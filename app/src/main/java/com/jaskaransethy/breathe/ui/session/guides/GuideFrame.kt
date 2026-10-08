package com.jaskaransethy.breathe.ui.session.guides

import com.jaskaransethy.breathe.data.Phase

/**
 * Everything a breathing visual needs for one frame. Every guide is a pure function of this, so
 * pausing, Reduce Motion and the end-of-session fade behave the same whichever guide is chosen.
 */
data class GuideFrame(
    val phase: Phase,
    /** 0 = fully exhaled, 1 = fully inhaled, already eased (sine in-out). */
    val breath: Float,
    /** 0..1 through the current phase, linear. */
    val phaseProgress: Float,
    /** Session time, for slow ambient motion such as the ±2% "breathing" during holds. */
    val elapsedMs: Long,
    /** "Breathe in", "Hold" or "Breathe out". */
    val phaseLabel: String,
    /** Whole seconds left in this phase, shown as the count. */
    val secondsLeft: Int,
    /** Reduce Motion: no scaling or travelling; carry the rhythm with brightness instead. */
    val reduceMotion: Boolean,
    /** 1 normally; fades to 0 when the session ends. */
    val alpha: Float
)
