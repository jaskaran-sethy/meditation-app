package com.example.myapplication.ui.session.guides

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.Phase
import com.example.myapplication.ui.theme.BreatheColors
import com.example.myapplication.ui.theme.BreatheTheme
import com.example.myapplication.ui.theme.BreatheType
import kotlin.math.PI
import kotlin.math.sin

private const val MIN_SCALE = 0.7f

/** The design's guide: outer ring, accent halo and an orb that grows on the inhale and shrinks on the exhale. */
@Composable
fun OrbGuide(frame: GuideFrame, modifier: Modifier = Modifier) {
    val colors = BreatheTheme.colors
    // Halo breathes ±2% during holds so the screen never feels frozen.
    val holdWobble = if (frame.phase == Phase.HoldIn || frame.phase == Phase.HoldOut) {
        0.02f * sin(2 * PI * frame.elapsedMs / 2000.0).toFloat()
    } else 0f
    val scale = if (frame.reduceMotion) 0.85f else MIN_SCALE + (1f - MIN_SCALE) * frame.breath
    // With Reduce Motion the orb stays still and its brightness carries the rhythm instead.
    val brightness = if (frame.reduceMotion) 0.45f + 0.55f * frame.breath else 1f

    Box(
        modifier = modifier
            .size(300.dp)
            .clearAndSetSemantics { contentDescription = "${frame.phaseLabel}, ${frame.secondsLeft}" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(colors.ink15, radius = size.minDimension / 2, style = Stroke(1.dp.toPx()))
        }
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = scale * (1f + if (frame.reduceMotion) 0f else holdWobble)
                    scaleX = s
                    scaleY = s
                    alpha = frame.alpha
                }
        ) {
            val orbRadius = 100.dp.toPx()
            // Soft mint glow behind the orb.
            drawCircle(
                Brush.radialGradient(
                    0f to BreatheColors.Mint.copy(alpha = 0.4f),
                    1f to Color.Transparent,
                    center = center,
                    radius = orbRadius + 60.dp.toPx()
                ),
                radius = orbRadius + 60.dp.toPx()
            )
            drawCircle(colors.accentFaint, radius = 125.dp.toPx())
        }
        Box(
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = frame.alpha * brightness
                }
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(BreatheColors.OrbCentre, BreatheColors.OrbEdge))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(frame.phaseLabel, style = BreatheType.Phase, color = BreatheColors.OrbText)
                Text(
                    frame.secondsLeft.toString(),
                    style = BreatheType.Count,
                    color = BreatheColors.OrbText.copy(alpha = 0.7f)
                )
            }
        }
    }
}
