package com.jaskaransethy.breathe.ui.session.guides

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jaskaransethy.breathe.R
import com.jaskaransethy.breathe.data.BreathPattern
import com.jaskaransethy.breathe.data.GuideStyle
import com.jaskaransethy.breathe.data.Phase
import com.jaskaransethy.breathe.ui.theme.BreatheTheme
import kotlin.math.PI
import kotlin.math.cos

@get:StringRes
val GuideStyle.label: Int
    get() = when (this) {
        GuideStyle.Orb -> R.string.guide_orb
        GuideStyle.Tide -> R.string.guide_tide
        GuideStyle.Trace -> R.string.guide_trace
    }

@get:StringRes
val GuideStyle.description: Int
    get() = when (this) {
        GuideStyle.Orb -> R.string.guide_orb_description
        GuideStyle.Tide -> R.string.guide_tide_description
        GuideStyle.Trace -> R.string.guide_trace_description
    }

/** The chosen breathing visual. Every guide fits the same 300dp square and renders only from [frame]. */
@Composable
fun BreathingGuide(
    style: GuideStyle,
    frame: GuideFrame,
    pattern: BreathPattern,
    modifier: Modifier = Modifier
) {
    when (style) {
        GuideStyle.Orb -> OrbGuide(frame, modifier)
        GuideStyle.Tide -> TideGuide(frame, modifier)
        GuideStyle.Trace -> TraceGuide(frame, pattern, modifier)
    }
}

/** A frame for previews: [progress] through [phase], with the breath level the session would give it. */
internal fun previewFrame(
    phase: Phase,
    progress: Float,
    seconds: Int,
    reduceMotion: Boolean = false
): GuideFrame {
    val eased = ((1 - cos(PI * progress)) / 2).toFloat()
    val breath = when (phase) {
        Phase.Inhale -> eased
        Phase.HoldIn -> 1f
        Phase.Exhale -> 1f - eased
        Phase.HoldOut -> 0f
    }
    val label = when (phase) {
        Phase.Inhale -> "Breathe in"
        Phase.HoldIn, Phase.HoldOut -> "Hold"
        Phase.Exhale -> "Breathe out"
    }
    return GuideFrame(
        phase = phase,
        breath = breath,
        phaseProgress = progress,
        elapsedMs = 12_345L,
        phaseLabel = label,
        secondsLeft = seconds,
        reduceMotion = reduceMotion,
        alpha = 1f
    )
}

/** Mid-inhale, hold and mid-exhale side by side on the theme's background. */
@Composable
internal fun GuidePreviewRow(
    dark: Boolean,
    reduceMotion: Boolean = false,
    guide: @Composable (GuideFrame) -> Unit
) {
    BreatheTheme(darkTheme = dark) {
        Row(
            modifier = Modifier.background(BreatheTheme.colors.background).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            guide(previewFrame(Phase.Inhale, 0.5f, 2, reduceMotion))
            guide(previewFrame(Phase.HoldIn, 0.4f, 3, reduceMotion))
            guide(previewFrame(Phase.Exhale, 0.5f, 4, reduceMotion))
        }
    }
}
