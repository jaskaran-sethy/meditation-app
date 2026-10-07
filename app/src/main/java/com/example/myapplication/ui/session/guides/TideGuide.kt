package com.example.myapplication.ui.session.guides

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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.Phase
import com.example.myapplication.ui.theme.BreatheColors
import com.example.myapplication.ui.theme.BreatheTheme
import com.example.myapplication.ui.theme.BreatheType
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

private val VESSEL_RADIUS = 130.dp
private val WAVE_AMPLITUDE = 4.dp
private val STILL_WAVE_AMPLITUDE = 2.dp

/** Water level as a fraction of the vessel's height. Never quite empty or full, like a real tide. */
private const val LOW_TIDE = 0.14f
private const val HIGH_TIDE = 0.86f

/** With Reduce Motion the water rests here, below the words, and only its brightness changes. */
private const val STILL_TIDE = 0.3f

/** The surface drifts one wavelength every seven seconds: slower than any breath. */
private const val WAVE_PERIOD_MS = 7000.0
private const val SHIMMER_PERIOD_MS = 3200.0

/**
 * A round vessel whose water rises on the inhale, rests near the brim on the hold (light glints
 * softly under the surface), and ebbs on the exhale. The words change colour where the water
 * passes them, so they stay legible on the water and on the background in both themes.
 */
@Composable
fun TideGuide(frame: GuideFrame, modifier: Modifier = Modifier) {
    val colors = BreatheTheme.colors
    val reduceMotion = frame.reduceMotion
    val surface = TideSurface(
        level = if (reduceMotion) STILL_TIDE else LOW_TIDE + (HIGH_TIDE - LOW_TIDE) * frame.breath,
        drift = if (reduceMotion) 0f else (2 * PI * frame.elapsedMs / WAVE_PERIOD_MS).toFloat(),
        amplitude = if (reduceMotion) STILL_WAVE_AMPLITUDE else WAVE_AMPLITUDE
    )
    // Glints fade in and out across a hold, so nothing pops at the phase change.
    val shimmer = if (!reduceMotion && frame.phase.isHold) sin(PI * frame.phaseProgress).toFloat() else 0f
    // With Reduce Motion the water stays put and its brightness carries the rhythm instead.
    val brightness = if (reduceMotion) 0.35f + 0.65f * frame.breath else 1f

    val waterPath = remember { Path() }
    val crestPath = remember { Path() }
    val vesselPath = remember { Path() }
    val abovePath = remember { Path() }
    val belowPath = remember { Path() }

    Box(
        modifier = modifier
            .size(VESSEL_RADIUS * 2 + 40.dp)
            .clearAndSetSemantics { contentDescription = "${frame.phaseLabel}, ${frame.secondsLeft}" }
            .graphicsLayer { alpha = frame.alpha },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = VESSEL_RADIUS.toPx()
            vesselPath.reset()
            vesselPath.addOval(Rect(center, radius))
            traceWaterline(waterPath, surface, fillTo = size.height)
            traceWaterline(crestPath, surface, fillTo = null)
            val waterTop = baselineY(surface)

            drawCircle(colors.ink5, radius)
            clipPath(vesselPath) {
                drawPath(
                    waterPath,
                    Brush.verticalGradient(
                        0f to BreatheColors.OrbCentre,
                        1f to BreatheColors.OrbEdge,
                        startY = waterTop,
                        endY = min(waterTop + 180.dp.toPx(), center.y + radius)
                    ),
                    alpha = 0.95f * brightness
                )
                if (shimmer > 0f) drawGlints(waterTop, shimmer, frame.elapsedMs)
                drawPath(
                    crestPath,
                    colors.accent,
                    alpha = 0.75f * brightness,
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            drawCircle(colors.ink15, radius, style = Stroke(1.dp.toPx()))
        }
        // The words twice: in ink above the surface and in the orb's deep teal below it.
        TideWords(
            frame = frame,
            phaseColor = colors.ink,
            countColor = colors.ink70,
            modifier = Modifier.fillMaxSize().drawWithContent {
                traceWaterline(abovePath, surface, fillTo = 0f)
                clipPath(abovePath) { this@drawWithContent.drawContent() }
            }
        )
        TideWords(
            frame = frame,
            phaseColor = BreatheColors.OrbText,
            countColor = BreatheColors.OrbText.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxSize().drawWithContent {
                traceWaterline(belowPath, surface, fillTo = size.height)
                clipPath(belowPath) { this@drawWithContent.drawContent() }
            }
        )
    }
}

@Composable
private fun TideWords(frame: GuideFrame, phaseColor: Color, countColor: Color, modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(frame.phaseLabel, style = BreatheType.Phase, color = phaseColor)
        Text(frame.secondsLeft.toString(), style = BreatheType.Count, color = countColor)
    }
}

private class TideSurface(val level: Float, val drift: Float, val amplitude: Dp)

private val Phase.isHold get() = this == Phase.HoldIn || this == Phase.HoldOut

/** The water's resting height in px; the waves ride either side of it. */
private fun DrawScope.baselineY(surface: TideSurface): Float {
    val radius = VESSEL_RADIUS.toPx()
    return center.y + radius - surface.level * 2 * radius
}

/** Two slow, unequal swells, so the surface never looks mechanical. */
private fun DrawScope.waterlineY(x: Float, surface: TideSurface): Float {
    val k = (2 * PI / (size.width / 1.3f)).toFloat()
    val swell = 0.65f * sin(k * x + surface.drift) + 0.35f * sin(1.8f * k * x - 0.7f * surface.drift + 1f)
    return baselineY(surface) + surface.amplitude.toPx() * swell
}

/**
 * Traces the waterline into [path] across the full width. With [fillTo] the path is closed down
 * (or up) to that y, giving the water body or the air above it; without, it is just the crest.
 */
private fun DrawScope.traceWaterline(path: Path, surface: TideSurface, fillTo: Float?) {
    path.reset()
    val step = 4.dp.toPx()
    path.moveTo(0f, waterlineY(0f, surface))
    var x = 0f
    while (x < size.width) {
        x = min(x + step, size.width)
        path.lineTo(x, waterlineY(x, surface))
    }
    if (fillTo != null) {
        path.lineTo(size.width, fillTo)
        path.lineTo(0f, fillTo)
        path.close()
    }
}

/** Soft lines of light just under the surface during a hold. They brighten and dim; they never move. */
private fun DrawScope.drawGlints(waterTop: Float, shimmer: Float, elapsedMs: Long) {
    val time = (2 * PI * elapsedMs / SHIMMER_PERIOD_MS).toFloat()
    repeat(3) { i ->
        val twinkle = 0.5f + 0.5f * sin(time + i * 2.1f)
        val y = waterTop + (12 + 9 * i).dp.toPx()
        val half = (44 - 12 * i).dp.toPx()
        val cx = center.x + (i - 1) * 22.dp.toPx()
        drawLine(
            Color.White.copy(alpha = 0.6f * shimmer * twinkle),
            start = Offset(cx - half, y),
            end = Offset(cx + half, y),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Preview(widthDp = 980, heightDp = 340, name = "Tide · dark")
@Composable
private fun TideDarkPreview() {
    GuidePreviewRow(dark = true) { TideGuide(it) }
}

@Preview(widthDp = 980, heightDp = 340, name = "Tide · light")
@Composable
private fun TideLightPreview() {
    GuidePreviewRow(dark = false) { TideGuide(it) }
}

@Preview(widthDp = 980, heightDp = 340, name = "Tide · reduce motion")
@Composable
private fun TideReduceMotionPreview() {
    GuidePreviewRow(dark = true, reduceMotion = true) { TideGuide(it) }
}
