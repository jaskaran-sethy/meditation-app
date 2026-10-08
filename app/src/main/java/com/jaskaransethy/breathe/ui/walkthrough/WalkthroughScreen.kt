package com.jaskaransethy.breathe.ui.walkthrough

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaskaransethy.breathe.R
import com.jaskaransethy.breathe.data.PresetPattern
import com.jaskaransethy.breathe.data.BreatheStore
import com.jaskaransethy.breathe.data.shouldReduceMotion
import com.jaskaransethy.breathe.ui.components.Medal
import com.jaskaransethy.breathe.ui.components.PatternCard
import com.jaskaransethy.breathe.ui.components.PrimaryButton
import com.jaskaransethy.breathe.ui.icons.BreatheIcons
import com.jaskaransethy.breathe.ui.theme.BreatheColors
import com.jaskaransethy.breathe.ui.theme.BreatheTheme
import com.jaskaransethy.breathe.ui.theme.BreatheType
import kotlin.math.PI
import kotlin.math.cos
import kotlinx.coroutines.launch

private const val STEPS = 3

/** The first session after the walkthrough is short: a 2-minute taste. */
const val FirstSessionMinutes = 2

/**
 * First-open walkthrough (design: "Breathe — Walkthrough 1–3"). Shown once after Welcome: three
 * steps that each teach one thing, then straight into a short first session. Skip is offered on
 * every step except the last; swipe or Next advances.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WalkthroughScreen(store: BreatheStore, onStart: (PresetPattern) -> Unit) {
    val context = LocalContext.current
    val reduceMotion = remember { shouldReduceMotion(context, store) }
    val pager = rememberPagerState(pageCount = { STEPS })
    val scope = rememberCoroutineScope()
    // The first session defaults to Calm; step 2 lets people pick something else.
    var pattern by rememberSaveable { mutableStateOf(PresetPattern.Calm) }
    val colors = BreatheTheme.colors

    fun goTo(page: Int) {
        scope.launch { if (reduceMotion) pager.scrollToPage(page) else pager.animateScrollToPage(page) }
    }
    BackHandler(enabled = pager.currentPage > 0) { goTo(pager.currentPage - 1) }

    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        TopBar(
            step = pager.currentPage,
            showSkip = pager.currentPage < STEPS - 1,
            onSkip = { onStart(pattern) }
        )
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> Step(
                    title = R.string.walkthrough_orb_title,
                    body = R.string.walkthrough_orb_body
                ) { OrbDemo(reduceMotion, animate = pager.currentPage == 0) }
                1 -> Step(
                    title = R.string.walkthrough_rhythm_title,
                    body = R.string.walkthrough_rhythm_body
                ) { RhythmPicker(pattern, onSelect = { pattern = it }) }
                else -> Step(
                    title = R.string.walkthrough_progress_title,
                    body = R.string.walkthrough_progress_body
                ) { ProgressPreview() }
            }
        }
        val last = pager.currentPage == STEPS - 1
        PrimaryButton(
            text = if (last) {
                stringResource(R.string.walkthrough_start, FirstSessionMinutes)
            } else {
                stringResource(R.string.walkthrough_next)
            },
            onClick = { if (last) onStart(pattern) else goTo(pager.currentPage + 1) },
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 28.dp, end = 28.dp, bottom = 16.dp)
        )
    }
}

@Composable
private fun TopBar(step: Int, showSkip: Boolean, onSkip: () -> Unit) {
    val colors = BreatheTheme.colors
    val stepDescription = stringResource(R.string.walkthrough_step, step + 1, STEPS)
    Row(
        modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.clearAndSetSemantics { contentDescription = stepDescription },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(STEPS) { index ->
                val active = index == step
                Box(
                    Modifier
                        .size(width = if (active) 22.dp else 6.dp, height = 6.dp)
                        .background(if (active) colors.accent else colors.ink20, RoundedCornerShape(3.dp))
                )
            }
        }
        // Keep the slot on the last step so the dots don't jump; it's just not shown.
        Box(
            modifier = Modifier
                .height(44.dp)
                .alpha(if (showSkip) 1f else 0f)
                .clip(RoundedCornerShape(22.dp))
                .then(if (showSkip) Modifier.clickable(role = Role.Button, onClick = onSkip) else Modifier)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (showSkip) {
                Text(stringResource(R.string.walkthrough_skip), style = BreatheType.Label, color = colors.ink70)
            }
        }
    }
}

/** One page: an illustration filling the space above a title and a short paragraph. */
@Composable
private fun Step(title: Int, body: Int, illustration: @Composable () -> Unit) {
    val colors = BreatheTheme.colors
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            illustration()
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(title),
                style = BreatheType.Heading.copy(fontSize = BreatheType.Heading.fontSize * (34f / 32f)),
                color = colors.ink,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                stringResource(body),
                style = BreatheType.Body.copy(lineHeight = BreatheType.Body.fontSize * 1.5f),
                color = colors.ink80
            )
        }
    }
}

private enum class DemoPhase(val label: Int, val chip: Int, val icon: ImageVector) {
    In(R.string.phase_inhale, R.string.walkthrough_chip_grows, BreatheIcons.ArrowUpRight),
    Hold(R.string.phase_hold, R.string.walkthrough_chip_holds, BreatheIcons.Pause),
    Out(R.string.phase_exhale, R.string.walkthrough_chip_shrinks, BreatheIcons.ArrowDownLeft)
}

/**
 * A slow demo breath (4 s in, 2 s hold, 4 s out, 1 s rest) so people see what "follow the orb"
 * means before their first session. With Reduce Motion it shows the inhale, still, as designed.
 */
@Composable
private fun OrbDemo(reduceMotion: Boolean, animate: Boolean) {
    val colors = BreatheTheme.colors
    val cycleMs = 11_000
    val time = if (reduceMotion || !animate) {
        2_000f
    } else {
        val transition = rememberInfiniteTransition(label = "demo")
        val t by transition.animateFloat(
            initialValue = 0f,
            targetValue = cycleMs.toFloat(),
            animationSpec = infiniteRepeatable(tween(cycleMs, easing = LinearEasing), RepeatMode.Restart),
            label = "demoTime"
        )
        t
    }
    fun ease(p: Float) = ((1 - cos(PI * p)) / 2).toFloat()
    val (phase, breath) = when {
        time < 4_000f -> DemoPhase.In to ease(time / 4_000f)
        time < 6_000f -> DemoPhase.Hold to 1f
        time < 10_000f -> DemoPhase.Out to 1f - ease((time - 6_000f) / 4_000f)
        else -> DemoPhase.Out to 0f
    }
    val scale = if (reduceMotion) 1f else 0.7f + 0.3f * breath

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Box(
            Modifier
                .size(260.dp)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(colors.ink15, radius = size.minDimension / 2, style = Stroke(1.dp.toPx()))
            }
            Box(
                Modifier
                    .size(220.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .background(colors.accentFaint, CircleShape)
            )
            Canvas(Modifier.size(170.dp).graphicsLayer { scaleX = scale; scaleY = scale }) {
                // Soft mint glow behind the orb.
                drawCircle(
                    Brush.radialGradient(
                        0f to BreatheColors.Mint.copy(alpha = 0.4f),
                        1f to Color.Transparent,
                        center = center,
                        radius = size.minDimension / 2 + 50.dp.toPx()
                    ),
                    radius = size.minDimension / 2 + 50.dp.toPx()
                )
            }
            Box(
                Modifier
                    .size(170.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(BreatheColors.OrbCentre, BreatheColors.OrbEdge))),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(phase.label),
                        style = BreatheType.Phase.copy(fontSize = BreatheType.Phase.fontSize * (28f / 32f)),
                        color = BreatheColors.OrbText
                    )
                    Icon(
                        if (phase == DemoPhase.In) BreatheIcons.Maximize else phase.icon,
                        null,
                        Modifier.size(16.dp),
                        tint = BreatheColors.OrbText.copy(alpha = 0.7f)
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DemoPhase.entries.forEach { chip -> PhaseChip(chip, active = chip == phase) }
        }
    }
}

@Composable
private fun PhaseChip(phase: DemoPhase, active: Boolean) {
    val colors = BreatheTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (active) colors.surfaceSelected else colors.surface)
            .border(1.dp, if (active) colors.ink40 else colors.ink12, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tint = if (active) colors.ink else colors.ink70
        Icon(phase.icon, null, Modifier.size(14.dp), tint = tint)
        Text(
            stringResource(phase.chip),
            style = if (active) BreatheType.CaptionStrong else BreatheType.CaptionMedium,
            color = tint
        )
    }
}

/** The real pattern cards from Home, so nothing looks new later. */
@Composable
private fun RhythmPicker(selected: PresetPattern, onSelect: (PresetPattern) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp).selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PresetPattern.entries.forEach { option ->
            PatternCard(
                title = stringResource(option.title),
                description = stringResource(option.description),
                rhythm = option.counts,
                selected = option == selected,
                onClick = { onSelect(option) }
            )
        }
        Text(
            stringResource(R.string.walkthrough_rhythm_hint),
            style = BreatheType.Caption,
            color = BreatheTheme.colors.ink60,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Sets expectations for badges up front: practice, not log-ins, and nothing to lose. */
@Composable
private fun ProgressPreview() {
    val colors = BreatheTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.accentFaint, RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 28.dp)
        ) {
            MedalExample(BreatheIcons.Sprout, earned = true, progress = 1f, R.string.walkthrough_medal_earned, Modifier.weight(1f))
            MedalExample(BreatheIcons.Sunrise, earned = false, progress = 0.6f, R.string.walkthrough_medal_on_the_way, Modifier.weight(1f))
            MedalExample(BreatheIcons.Compass, earned = false, progress = 0f, R.string.walkthrough_medal_next, Modifier.weight(1f))
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RuleRow(BreatheIcons.CircleCheck, R.string.walkthrough_rule_full_sessions)
            RuleRow(BreatheIcons.CalendarRange, R.string.walkthrough_rule_nothing_lost)
            RuleRow(BreatheIcons.Eye, R.string.walkthrough_rule_see_whats_needed)
        }
    }
}

@Composable
private fun MedalExample(icon: ImageVector, earned: Boolean, progress: Float, label: Int, modifier: Modifier) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Medal(icon = icon, earned = earned, progress = progress)
        Text(
            stringResource(label),
            style = BreatheType.CaptionStrong,
            color = BreatheTheme.colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(96.dp)
        )
    }
}

@Composable
private fun RuleRow(icon: ImageVector, text: Int) {
    val colors = BreatheTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.ink12, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = colors.accent)
        Text(stringResource(text), style = BreatheType.Small, color = colors.ink80)
    }
}
