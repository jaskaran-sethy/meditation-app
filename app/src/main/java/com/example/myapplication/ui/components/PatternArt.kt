package com.example.myapplication.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.BreathPattern
import com.example.myapplication.ui.theme.BreatheTheme

/**
 * A pattern's drawing on its tile (design component "Pattern Card" › "Diagram Tile"): a 60 dp
 * accent-faint square holding a 48 dp drawing in the accent colours. Decorative only.
 */
@Composable
fun PatternArtTile(pattern: BreathPattern, modifier: Modifier = Modifier) {
    val colors = BreatheTheme.colors
    val art = remember(pattern, colors.accent, colors.accentSoft) {
        patternArt(pattern, colors.accent, colors.accentSoft)
    }
    Box(
        modifier = modifier
            .size(60.dp)
            .background(colors.accentFaint, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(rememberVectorPainter(art), contentDescription = null, modifier = Modifier.size(48.dp))
    }
}

private enum class Tone { Accent, Soft }

/** One shape of a drawing: path data on the 48×48 grid, filled and/or stroked 1.5 wide. */
private class Layer(
    val path: String,
    val fill: Tone? = null,
    val stroke: Tone? = null,
    val round: Boolean = false
)

private fun patternArt(pattern: BreathPattern, accent: Color, soft: Color): ImageVector {
    fun Tone.color() = SolidColor(if (this == Tone.Accent) accent else soft)
    return ImageVector.Builder("pattern-${pattern.id}", 48.dp, 48.dp, 48f, 48f).apply {
        artLayers.getValue(pattern).forEach { layer ->
            addPath(
                pathData = addPathNodes(layer.path),
                fill = layer.fill?.color(),
                stroke = layer.stroke?.color(),
                strokeLineWidth = 1.5f,
                strokeLineCap = if (layer.round) StrokeCap.Round else StrokeCap.Butt,
                strokeLineJoin = if (layer.round) StrokeJoin.Round else StrokeJoin.Miter
            )
        }
    }.build()
}

/** The drawings from the design file, flattened to absolute paths. */
private val artLayers: Map<BreathPattern, List<Layer>> = mapOf(
    /** A feather settling over ripples. */
    BreathPattern.Calm to listOf(
        Layer("M32.83 3.89C33.12 5.49 31.31 7.44 29.98 8.9C28.65 10.37 28.4 13.03 28.53 15.38C28.66 17.53 28.98 18.84 28.98 18.84C28.33 18.17 27.08 14.97 27.08 14.97C24.73 17.52 22.31 21.42 21.74 24.17C21.12 27.24 21.84 28.98 21.84 28.98C23.3 29.31 26.68 29.3 29.22 27.57C33.12 24.88 33.98 21.29 33.98 21.29L33.46 21.29C34.17 20.62 34.99 19.19 34.99 19.19L33.64 20.03C33.64 20.03 34.52 18.33 34.99 15.75C36.06 10.2 33.8 4.85 32.83 3.89Z", fill = Tone.Soft),
        Layer("M32.83 3.89C33.12 5.49 31.31 7.44 29.98 8.9C28.65 10.37 28.4 13.03 28.53 15.38C28.66 17.53 29.06 18.91 29.06 18.91L29.51 20.23C31.52 19.06 34.19 16.53 34.99 14.63C35.97 10.05 33.8 4.85 32.83 3.89Z", fill = Tone.Soft),
        Layer("M24.86 18.49C23.4 20.11 22.11 22.51 21.74 24.48C21.33 26.78 21.73 28.38 21.98 28.42C24.24 27.79 26.37 22.76 24.86 18.49Z", fill = Tone.Soft),
        Layer("M29.68 25.43C28.18 25.51 25.95 25.91 25 26.28C23.89 27.4 22.52 28.76 22.25 29C23.41 29.31 26.02 29.17 27.89 28.32C29.52 27.59 29.68 25.43 29.68 25.43Z", fill = Tone.Soft),
        Layer("M32.75 2.16C32.66 2 32.47 1.96 32.31 2.04C32.16 2.13 32.11 2.33 32.2 2.48C33.73 5.13 34.99 8.55 34.99 12.12C34.99 16.52 32.94 19.09 30.78 20.2C30.62 20.28 30.65 20.44 30.84 20.39C33.46 19.65 35.63 16.52 35.63 12.08C35.63 8.22 34.27 4.64 32.75 2.16Z", fill = Tone.Accent),
        Layer("M32.53 5.71C31.54 7.64 29.21 8.73 28.5 11.61C27.87 14.12 28.23 17.66 28.92 18.74C29.04 18.92 29.23 18.85 29.2 18.6C28.9 16.33 28.65 13.9 29.15 11.74C29.73 9.17 32.1 7.85 32.94 6.02C33.08 5.67 32.7 5.5 32.53 5.71Z", fill = Tone.Accent),
        Layer("M26.77 13.32C26.29 16.02 26.79 19.28 28.07 20.71C28.24 20.88 28.4 20.71 28.3 20.42C27.44 18.34 27.01 16.04 27.38 13.41C27.44 13.04 26.84 12.97 26.77 13.32Z", fill = Tone.Accent),
        Layer("M25.77 16.37C23.4 19.08 21.19 23.08 21.19 26.35C21.19 27 21.28 27.56 21.42 27.9C21.51 28.14 21.88 28.08 21.86 27.82C21.84 27.55 21.83 27.27 21.83 26.95C21.83 23.08 24.22 19.25 26.04 16.63C26.23 16.33 25.99 16.14 25.77 16.37Z", fill = Tone.Accent),
        Layer("M22.99 18.64C20.23 20.8 18.31 24.01 18.31 27.37C18.31 28.71 18.62 30.02 18.97 30.6C19.06 30.74 19.19 30.72 19.17 30.52C19.06 29.65 18.92 28.8 18.92 27.66C18.92 24.08 21.21 20.83 23.3 19.01C23.55 18.71 23.32 18.45 22.99 18.64Z", fill = Tone.Accent),
        Layer("M34.92 18.38C33.73 20.45 31.43 21.66 29.42 21.7C28.98 21.71 29.01 22.24 29.46 22.26C32.3 22.38 34.9 20.19 35.48 18.63C35.58 18.29 35.13 18.1 34.92 18.38Z", fill = Tone.Accent),
        Layer("M33.81 21.51C31.69 25.45 27.86 28.59 24.02 28.84C23.64 28.87 23.68 29.3 24.16 29.3C28.85 29.22 33.26 24.43 34.02 21.6C34.08 21.35 33.95 21.3 33.81 21.51Z", fill = Tone.Accent),
        Layer("M27.92 29.12C25.94 30.49 23.63 31 21.65 30.52C21.15 30.4 21.08 30.97 21.56 31.1C24.16 31.81 26.84 30.86 28.33 29.63C28.64 29.34 28.34 28.92 27.92 29.12Z", fill = Tone.Accent),
        Layer("M32.61 10.69C31.76 15.97 29.04 21.65 23.88 26.68C20.78 29.68 17.38 32.77 15.31 34.76C14.95 35.1 15.37 35.51 15.74 35.2C17.9 33.22 21.21 30.26 24.27 27.38C29.74 22.22 32.68 15.58 32.81 10.73C32.82 10.55 32.63 10.54 32.61 10.69Z", fill = Tone.Accent),
        Layer("M30.47 39.78C30.41 39.79 30.4 39.9 30.52 39.93C31.82 40.16 32.88 40.44 33 40.66C32.44 41.16 28.04 41.67 23.93 41.67C19.5 41.67 15.37 41.1 14.99 40.68C15.11 40.4 16.41 40.13 17.4 39.94C17.53 39.91 17.5 39.8 17.4 39.8C16.14 39.74 14.34 40.01 14.34 40.7C14.34 41.72 19.69 42.25 23.96 42.25C28.49 42.25 33.66 41.73 33.66 40.72C33.66 39.98 31.76 39.73 30.47 39.78Z", fill = Tone.Accent),
        Layer("M34.19 43.19C34.09 43.2 34.08 43.31 34.21 43.35C35.32 43.6 36.32 43.84 36.43 44.08C35.72 44.67 29.85 45.41 23.99 45.41C17.16 45.41 11.87 44.62 11.5 44.11C11.62 43.88 12.8 43.59 13.83 43.36C13.97 43.33 13.95 43.2 13.83 43.2C12.2 43.23 10.91 43.47 10.91 44.14C10.91 45.31 18.01 46 24.01 46C30.62 46 37.09 45.3 37.09 44.13C37.09 43.45 35.58 43.21 34.19 43.19Z", fill = Tone.Accent)
    ),
    /** A square window with the sun on the horizon. */
    BreathPattern.Box to listOf(
        Layer("M2.94 2H45.07V45.92H2.94Z", fill = Tone.Soft, stroke = Tone.Accent),
        Layer("M2.93 33.13L45.07 33.13L45.07 44.79C45.07 45.47 44.53 46 43.86 46L4.14 46C3.47 46 2.93 45.47 2.93 44.79L2.93 33.13Z", fill = Tone.Soft, stroke = Tone.Accent),
        Layer("M23.98 23.68C18.75 23.68 14.81 27.91 14.92 33.04L33.07 33.04C33.18 27.68 29.19 23.68 23.98 23.68Z", fill = Tone.Soft, stroke = Tone.Accent),
        Layer("M2.94 33.12L45.07 33.12", stroke = Tone.Accent)
    ),
    /** Three stacked stones. */
    BreathPattern.Balance to listOf(
        Layer("M20.29 27.81C10.97 28.44 5.32 33.16 5.32 37.57C5.32 40.01 6.48 41.62 8.35 42.71C7.44 38.65 20.61 36.34 20.29 27.81Z", fill = Tone.Soft),
        Layer("M20.13 14.35C14.26 15.07 10.74 18.52 10.74 21.43C10.74 24.14 12.8 25.56 15.3 26.08C15.17 22.84 21.01 21.44 20.29 14.36L20.13 14.35Z", fill = Tone.Soft),
        Layer("M23.8 2.75C18.44 2.96 15.37 6.99 15.37 9.19C15.37 10.99 16.68 12.08 18.33 12.57C17.54 7.97 23.87 8.27 23.8 2.75Z", fill = Tone.Soft),
        Layer("M42.54 37.89C42.54 44.56 33.29 45.17 23.05 45.17C12.81 45.17 4.74 43.91 4.74 37.46C4.74 30.53 11.95 27.26 22.95 27.26C36.78 27.26 42.54 32.67 42.54 37.89Z", stroke = Tone.Accent),
        Layer("M36.56 21.17C36.56 25.9 30.17 26.7 22.91 26.7C15.65 26.7 9.97 25.65 9.97 21.13C9.97 17.36 14.66 13.96 22.72 13.96C32.36 13.96 36.56 17.58 36.56 21.17Z", stroke = Tone.Accent),
        Layer("M31.73 8.85C31.73 12.07 27.88 13.02 22.98 13.02C18.08 13.02 14.84 11.9 14.84 9.09C14.84 5.97 18.96 2.41 23.43 2.41C27.91 3 31.73 5.83 31.73 8.85Z", stroke = Tone.Accent),
        Layer("M2 45.59L46 45.59", stroke = Tone.Accent, round = true)
    ),
    /** A crescent moon over a cloud, with two stars. */
    BreathPattern.Sleep to listOf(
        Layer("M32.57 32.76C36.32 30.57 38.3 27.29 38.02 26.9C37.89 26.72 37.51 26.94 37.15 27.08C35.6 27.68 33.98 27.99 32.41 27.99C24.59 27.99 19.06 21.75 19.06 13.91C19.06 9.38 20.98 5.34 23.94 3.1C24.34 2.74 24.26 2.24 23.64 2.33C15.21 3.57 9.31 10.63 9.31 18.84C9.31 23.45 11.13 27.55 13.95 30.05C17.45 28.92 21.42 30.49 23.47 34.9C25.9 32.68 29.54 31.84 32.57 32.76Z", fill = Tone.Soft, stroke = Tone.Accent, round = true),
        Layer("M35 35.62C34.59 33.91 32.84 32.27 29.84 32.27C27.16 32.27 24.68 33.68 23.44 34.92C21.95 31.8 19.16 29.7 15.99 29.7C12.58 29.7 10.17 32.3 9.78 34.9C6.63 35.27 4.59 36.91 4.59 39.34C4.59 42.18 7.32 44.15 10.99 44.15C12.46 44.15 13.78 43.84 14.61 43.51C16.71 45.17 19.21 46 21.78 46C24.99 46 27.98 44.53 29.78 42.92C30.95 43.29 32.15 43.44 33.21 43.44C36.75 43.44 39.13 41.41 39.13 39.17C39.13 37.08 37.36 35.79 35 35.62Z", fill = Tone.Soft, stroke = Tone.Accent, round = true),
        Layer("M5.13 2L4.63 3.75L2.75 4.65L4.46 5.81L4.69 7.75L6.38 6.65L8.3 6.96L7.69 5.09L8.64 3.33L6.65 3.39L5.13 2Z", fill = Tone.Soft, stroke = Tone.Accent, round = true),
        Layer("M42.22 16.21L41.33 17.75L39.27 18.03L40.5 19.72L40.11 21.66L42.03 20.98L43.87 21.92L43.79 19.9L45.25 18.52L43.17 17.97L42.22 16.21Z", fill = Tone.Soft, stroke = Tone.Accent, round = true)
    )
)
