package com.jaskaransethy.breathe.ui.session.guides

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaskaransethy.breathe.data.BreathPattern
import com.jaskaransethy.breathe.data.PresetPattern
import com.jaskaransethy.breathe.data.Phase
import com.jaskaransethy.breathe.ui.theme.BreatheColors
import com.jaskaransethy.breathe.ui.theme.BreatheTheme
import com.jaskaransethy.breathe.ui.theme.BreatheType
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

private val TRACK_SIDE = 236.dp
private val TRACK_CORNER = 52.dp
private val DOT_RADIUS = 9.dp
private val GLOW_RADIUS = 30.dp

/** How much of the loop the fading trail covers behind the dot. */
private const val TRAIL_LENGTH = 0.22f
private const val TRAIL_STEPS = 48

/**
 * How much sine easing to blend into the dot's pace: enough that it settles slightly into each
 * phase change, never so much that it stops.
 */
private const val EASE_MIX = 0.35f

/** With Reduce Motion, the share of a phase over which its stretch of the track cross-fades in. */
private const val CROSS_FADE = 0.12f

/** Positions on the loop, clockwise from the bottom-left corner: the middles of the left and right sides. */
private const val LEFT_SIDE_MIDDLE = 0.125f
private const val RIGHT_SIDE_MIDDLE = 0.625f

/**
 * A soft point of light travelling clockwise around a rounded square, leaving a fading mint
 * trail. Each phase owns a stretch of the loop in proportion to its length, so the dot keeps one
 * steady pace; the inhale rises up the left side and the exhale falls down the right. For Box
 * breathing each phase is exactly one side. The stretch for the current phase is lit ahead of the
 * dot, so you can see how far this phase has to go.
 */
@Composable
fun TraceGuide(frame: GuideFrame, pattern: BreathPattern, modifier: Modifier = Modifier) {
    val colors = BreatheTheme.colors
    val loop = remember(pattern) { TraceLoop(pattern) }
    val index = loop.indexOf(frame.phase)
    val eased = ((1 - cos(PI * frame.phaseProgress)) / 2).toFloat()
    val pace = frame.phaseProgress + (eased - frame.phaseProgress) * EASE_MIX
    val dotAt = loop.start(index) + loop.length(index) * pace
    // With Reduce Motion nothing travels: the current stretch glows brighter as the lungs fill.
    val stretchFade = if (frame.reduceMotion) (frame.phaseProgress / CROSS_FADE).coerceIn(0f, 1f) else 1f
    val fillAlpha = if (frame.reduceMotion) 0.2f + 0.8f * frame.breath else 0.35f + 0.65f * frame.breath

    Box(
        modifier = modifier
            .size(300.dp)
            .clearAndSetSemantics { contentDescription = "${frame.phaseLabel}, ${frame.secondsLeft}" }
            .graphicsLayer { alpha = frame.alpha },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val square = RoundedSquare(center, TRACK_SIDE.toPx(), TRACK_CORNER.toPx())
            val trackStroke = 1.5.dp.toPx()

            drawRoundRect(
                if (frame.reduceMotion) colors.accentSoft else colors.accentFaint,
                topLeft = square.topLeft,
                size = Size(square.side, square.side),
                cornerRadius = CornerRadius(square.radius),
                alpha = if (frame.reduceMotion) 0.35f * fillAlpha else fillAlpha
            )
            drawRoundRect(
                colors.ink12,
                topLeft = square.topLeft,
                size = Size(square.side, square.side),
                cornerRadius = CornerRadius(square.radius),
                style = Stroke(trackStroke)
            )

            if (frame.reduceMotion) {
                val previous = (index - 1 + loop.size) % loop.size
                val lit = colors.accent.copy(alpha = 0.4f + 0.6f * frame.breath)
                if (stretchFade < 1f) {
                    drawStretch(square, loop.start(previous), loop.end(previous), lit, 3.dp.toPx(), 1f - stretchFade)
                }
                drawStretch(square, loop.start(index), loop.end(index), lit, 3.dp.toPx(), stretchFade)
            } else {
                drawStretch(square, loop.start(index), loop.end(index), colors.ink30, 2.dp.toPx(), 1f)
            }

            // Small marks where one phase hands over to the next.
            repeat(loop.size) { i ->
                drawCircle(colors.ink40, radius = 2.5.dp.toPx(), center = square.pointAt(loop.start(i)))
            }

            if (!frame.reduceMotion) {
                drawTrail(square, dotAt, colors.accent)
                drawDot(square.pointAt(dotAt), colors.accent)
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(frame.phaseLabel, style = BreatheType.Phase, color = colors.ink)
            Text(frame.secondsLeft.toString(), style = BreatheType.Count, color = colors.ink70)
        }
    }
}

/**
 * Where each phase sits on the loop (0..1, clockwise from the bottom-left corner). Stretches are
 * proportional to phase length, and the loop is turned so the inhale is centred on the left side
 * and the exhale on the right as nearly as the pattern allows (exactly, for Box and two-phase
 * patterns).
 */
private class TraceLoop(pattern: BreathPattern) {
    private val phases = pattern.steps.map { it.phase }
    private val lengths = pattern.steps.map { it.seconds.toFloat() / pattern.cycleSeconds }
    private val starts: List<Float>

    init {
        val cumulative = lengths.runningFold(0f) { total, length -> total + length }
        val inhale = phases.indexOf(Phase.Inhale).coerceAtLeast(0)
        val exhale = phases.indexOf(Phase.Exhale).coerceAtLeast(0)
        val inhaleTurn = LEFT_SIDE_MIDDLE - (cumulative[inhale] + lengths[inhale] / 2)
        val exhaleTurn = RIGHT_SIDE_MIDDLE - (cumulative[exhale] + lengths[exhale] / 2)
        val turn = (inhaleTurn + exhaleTurn) / 2
        starts = cumulative.dropLast(1).map { it + turn }
    }

    val size: Int get() = phases.size

    fun indexOf(phase: Phase): Int = phases.indexOf(phase).coerceAtLeast(0)

    fun start(index: Int): Float = starts[index]

    fun length(index: Int): Float = lengths[index]

    fun end(index: Int): Float = starts[index] + lengths[index]
}

/** A rounded square walked clockwise by arc length; 0 is the middle of the bottom-left corner. */
private class RoundedSquare(centre: Offset, val side: Float, val radius: Float) {
    val topLeft = Offset(centre.x - side / 2, centre.y - side / 2)
    private val left = topLeft.x
    private val top = topLeft.y
    private val right = left + side
    private val bottom = top + side
    private val straight = side - 2 * radius
    private val corner = (PI / 2 * radius).toFloat()
    val perimeter = 4 * (straight + corner)

    fun pointAt(position: Float): Offset {
        val wrapped = ((position % 1f) + 1f) % 1f
        // Walk from the bottom of the left side, half a corner after position 0.
        var d = wrapped * perimeter - corner / 2
        if (d < 0f) d += perimeter
        val segment = straight + corner
        val edge = (d / segment).toInt().coerceIn(0, 3)
        val t = d - edge * segment
        return if (t < straight) {
            when (edge) {
                0 -> Offset(left, bottom - radius - t)
                1 -> Offset(left + radius + t, top)
                2 -> Offset(right, top + radius + t)
                else -> Offset(right - radius - t, bottom)
            }
        } else {
            val turn = (t - straight) / radius
            val (cx, cy, from) = when (edge) {
                0 -> Triple(left + radius, top + radius, PI.toFloat())
                1 -> Triple(right - radius, top + radius, 1.5f * PI.toFloat())
                2 -> Triple(right - radius, bottom - radius, 0f)
                else -> Triple(left + radius, bottom - radius, 0.5f * PI.toFloat())
            }
            Offset(cx + radius * cos(from + turn), cy + radius * sin(from + turn))
        }
    }
}

/** One phase's stretch of the track, from [from] to [to] on the loop. */
private fun DrawScope.drawStretch(
    square: RoundedSquare,
    from: Float,
    to: Float,
    color: Color,
    width: Float,
    alpha: Float
) {
    val steps = ceil((to - from) * square.perimeter / 2.dp.toPx()).toInt().coerceAtLeast(2)
    val path = Path()
    val first = square.pointAt(from)
    path.moveTo(first.x, first.y)
    for (i in 1..steps) {
        val point = square.pointAt(from + (to - from) * i / steps)
        path.lineTo(point.x, point.y)
    }
    drawPath(path, color, alpha = alpha, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** A trail that thickens and brightens towards the dot. */
private fun DrawScope.drawTrail(square: RoundedSquare, head: Float, color: Color) {
    var previous = square.pointAt(head - TRAIL_LENGTH)
    for (i in 1..TRAIL_STEPS) {
        val fraction = i.toFloat() / TRAIL_STEPS
        val point = square.pointAt(head - TRAIL_LENGTH * (1f - fraction))
        drawLine(
            color.copy(alpha = color.alpha * 0.85f * fraction.pow(1.6f)),
            start = previous,
            end = point,
            strokeWidth = (1.5f + 2.5f * fraction).dp.toPx(),
            cap = StrokeCap.Butt
        )
        previous = point
    }
}

private fun DrawScope.drawDot(at: Offset, glow: Color) {
    val glowRadius = GLOW_RADIUS.toPx()
    drawCircle(
        Brush.radialGradient(
            0f to glow.copy(alpha = 0.5f),
            1f to Color.Transparent,
            center = at,
            radius = glowRadius
        ),
        radius = glowRadius,
        center = at
    )
    val dotRadius = DOT_RADIUS.toPx()
    drawCircle(
        Brush.radialGradient(
            listOf(BreatheColors.OrbCentre, BreatheColors.OrbEdge),
            center = at,
            radius = dotRadius
        ),
        radius = dotRadius,
        center = at
    )
}

@Preview(widthDp = 980, heightDp = 340, name = "Trace · Box · dark")
@Composable
private fun TraceDarkPreview() {
    GuidePreviewRow(dark = true) { TraceGuide(it, PresetPattern.Box) }
}

@Preview(widthDp = 980, heightDp = 340, name = "Trace · 4-7-8 · light")
@Composable
private fun TraceLightPreview() {
    GuidePreviewRow(dark = false) { TraceGuide(it, PresetPattern.Calm) }
}

@Preview(widthDp = 980, heightDp = 340, name = "Trace · reduce motion")
@Composable
private fun TraceReduceMotionPreview() {
    GuidePreviewRow(dark = true, reduceMotion = true) { TraceGuide(it, PresetPattern.Box) }
}
