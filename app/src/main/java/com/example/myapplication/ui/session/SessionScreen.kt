package com.example.myapplication.ui.session

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.myapplication.R
import com.example.myapplication.data.BreathPattern
import com.example.myapplication.data.Phase
import com.example.myapplication.data.SessionPlan
import com.example.myapplication.data.GuideStyle
import com.example.myapplication.data.SessionRecord
import com.example.myapplication.data.SessionState
import com.example.myapplication.ui.components.IconCircleButton
import com.example.myapplication.ui.components.displayName
import com.example.myapplication.ui.components.PhotoBackground
import com.example.myapplication.ui.components.Rhythm
import com.example.myapplication.ui.icons.BreatheIcons
import com.example.myapplication.ui.session.guides.BreathingGuide
import com.example.myapplication.ui.session.guides.GuideFrame
import com.example.myapplication.ui.theme.BreatheTheme
import com.example.myapplication.ui.theme.BreatheType
import kotlin.math.PI
import kotlin.math.cos
import kotlinx.coroutines.launch

private const val FINISH_FADE_MS = 800

/** What happened in a session, for the practice history and badges. */
data class SessionResult(
    val breaths: Int,
    val completedSeconds: Int,
    /** True if the session was paused at any point, including by leaving the app. */
    val paused: Boolean,
    val startMillis: Long
)

@Composable
fun SessionScreen(
    pattern: BreathPattern,
    minutes: Int,
    guideStyle: GuideStyle,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    keepScreenAwake: Boolean,
    reduceMotion: Boolean,
    onToggleSound: () -> Unit,
    /** Closed early. [result] is set only if at least 90% was completed, so it still counts. */
    onClose: (result: SessionResult?) -> Unit,
    onFinished: (SessionResult) -> Unit
) {
    val plan = remember(pattern, minutes) { SessionPlan(pattern, minutes) }
    var elapsedMs by rememberSaveable { mutableLongStateOf(0L) }
    var running by rememberSaveable { mutableStateOf(true) }
    var pausedEver by rememberSaveable { mutableStateOf(false) }
    val startMillis = rememberSaveable { System.currentTimeMillis() }
    val state = plan.stateAt(elapsedMs)
    fun result() = SessionResult(
        breaths = if (state.finished) plan.breaths else state.breathIndex,
        completedSeconds = (elapsedMs / 1000).toInt(),
        paused = pausedEver,
        startMillis = startMillis
    )

    val view = LocalView.current
    val sounds = remember(view) { SessionSounds(view.context) }
    DisposableEffect(sounds) { onDispose { sounds.release() } }
    val cues = remember(view, sounds, hapticsEnabled) { SessionCues(view, sounds, hapticsEnabled) }
    // Read inside long-lived effects, so toggling sound mid-session applies to the next cue.
    val currentSoundEnabled by rememberUpdatedState(soundEnabled)
    val orbAlpha = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    // The screen stays awake for the whole session (Settings: Keep screen awake).
    DisposableEffect(view, keepScreenAwake) {
        view.keepScreenOn = keepScreenAwake
        onDispose { view.keepScreenOn = false }
    }

    // Leaving the app pauses the session rather than letting it run unseen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && running) {
                running = false
                pausedEver = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Advance the session clock on every frame while running.
    LaunchedEffect(running, state.finished) {
        if (!running || state.finished) return@LaunchedEffect
        var last = withFrameMillis { it }
        while (true) {
            val now = withFrameMillis { it }
            elapsedMs = (elapsedMs + (now - last)).coerceAtMost(plan.totalMs)
            last = now
            if (elapsedMs >= plan.totalMs) break
        }
    }

    // Cue each phase change: tone, haptic tap and a short screen-reader announcement (never every second).
    val phaseLabel = stringResource(phaseLabelRes(state.step.phase))
    LaunchedEffect(state.breathIndex, state.stepIndex) {
        if (state.finished) return@LaunchedEffect
        cues.phaseStarted(state.step.phase, currentSoundEnabled)
        view.announceForAccessibility("$phaseLabel, ${state.step.seconds}")
    }

    LaunchedEffect(state.finished) {
        if (!state.finished) return@LaunchedEffect
        cues.sessionEnded(currentSoundEnabled)
        if (!reduceMotion) orbAlpha.animateTo(0f, tween(FINISH_FADE_MS))
        onFinished(result())
    }

    val scrim = BreatheTheme.colors.scrim(0.82f)
    PhotoBackground(overlay = Brush.verticalGradient(listOf(scrim, scrim))) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            TopBar(
                pattern = pattern,
                soundEnabled = soundEnabled,
                onToggleSound = onToggleSound,
                onClose = {
                    val counts = elapsedMs >= minutes * 60_000L * SessionRecord.FULL_FRACTION
                    onClose(if (counts) result() else null)
                }
            )
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BreathingGuide(
                    style = guideStyle,
                    frame = GuideFrame(
                        phase = state.step.phase,
                        breath = breathLevel(state),
                        phaseProgress = state.phaseProgress,
                        elapsedMs = state.elapsedMs,
                        phaseLabel = phaseLabel,
                        secondsLeft = state.secondsLeftInPhase,
                        reduceMotion = reduceMotion,
                        alpha = orbAlpha.value
                    ),
                    pattern = pattern
                )
                PhaseStrip(pattern = pattern, activeStep = state.stepIndex)
            }
            Controls(
                state = state,
                running = running,
                onTogglePause = {
                    if (!state.finished) {
                        if (running) pausedEver = true
                        running = !running
                        scope.launch { orbAlpha.snapTo(1f) }
                    }
                }
            )
        }
    }
}

@Composable
private fun TopBar(
    pattern: BreathPattern,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircleButton(BreatheIcons.Close, stringResource(R.string.session_close), onClose)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                pattern.displayName(),
                style = BreatheType.BodyStrong,
                color = BreatheTheme.colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Rhythm(pattern.counts, BreatheType.SmallMedium, BreatheTheme.colors.ink70, gap = 5.dp)
        }
        IconCircleButton(
            icon = if (soundEnabled) BreatheIcons.Volume else BreatheIcons.VolumeOff,
            contentDescription = stringResource(if (soundEnabled) R.string.sound_off else R.string.sound_on),
            onClick = onToggleSound
        )
    }
}

/** 0 = fully exhaled, 1 = fully inhaled, eased with a sine curve. */
private fun breathLevel(state: SessionState): Float {
    val eased = ((1 - cos(PI * state.phaseProgress)) / 2).toFloat()
    return when (state.step.phase) {
        Phase.Inhale -> eased
        Phase.HoldIn -> 1f
        Phase.Exhale -> 1f - eased
        Phase.HoldOut -> 0f
    }
}

@Composable
private fun PhaseStrip(pattern: BreathPattern, activeStep: Int) {
    val colors = BreatheTheme.colors
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        pattern.steps.forEachIndexed { index, step ->
            val active = index == activeStep
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .width(40.dp)
                        .height(3.dp)
                        .background(if (active) colors.ink else colors.ink25, RoundedCornerShape(2.dp))
                )
                Text(
                    stringResource(phaseShortRes(step.phase), step.seconds),
                    style = if (active) BreatheType.SmallStrong else BreatheType.SmallMedium,
                    color = if (active) colors.ink else colors.ink60
                )
            }
        }
    }
}

@Composable
private fun Controls(state: SessionState, running: Boolean, onTogglePause: () -> Unit) {
    val colors = BreatheTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val progress = state.elapsedMs.toFloat() / (state.elapsedMs + state.remainingMs)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.ink20)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.ink)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatClock(state.elapsedMs, roundUp = false), style = BreatheType.SmallMedium, color = colors.ink70)
                Text(
                    stringResource(R.string.session_time_left, formatClock(state.remainingMs)),
                    style = BreatheType.SmallMedium,
                    color = colors.ink70
                )
            }
        }
        IconCircleButton(
            icon = if (running) BreatheIcons.Pause else BreatheIcons.Play,
            contentDescription = stringResource(if (running) R.string.session_pause else R.string.session_resume),
            onClick = onTogglePause,
            size = 72.dp,
            iconSize = 26.dp,
            background = colors.buttonBg,
            tint = colors.buttonFg
        )
    }
}

/** Elapsed time rounds down and remaining time rounds up, so the two always add up to the total. */
private fun formatClock(ms: Long, roundUp: Boolean = true): String {
    val totalSeconds = if (roundUp) (ms + 999) / 1000 else ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun phaseLabelRes(phase: Phase) = when (phase) {
    Phase.Inhale -> R.string.phase_inhale
    Phase.HoldIn, Phase.HoldOut -> R.string.phase_hold
    Phase.Exhale -> R.string.phase_exhale
}

private fun phaseShortRes(phase: Phase) = when (phase) {
    Phase.Inhale -> R.string.phase_short_in
    Phase.HoldIn, Phase.HoldOut -> R.string.phase_short_hold
    Phase.Exhale -> R.string.phase_short_out
}
