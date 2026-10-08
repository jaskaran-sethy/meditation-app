package com.jaskaransethy.breathe.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.jaskaransethy.breathe.R

/** Fixed brand colours from the design document. Screens use [BreatheTheme.colors] instead. */
object BreatheColors {
    val Night = Color(0xFF061C22)
    val DeepTeal = Color(0xFF0B2A33)
    val Lake = Color(0xFF0F3A45)
    val Mint = Color(0xFF9FD3C7)
    val DeepMint = Color(0xFF3E8F84)
    val Mist = Color(0xFFCFEDE6)
    val Paper = Color(0xFFF7F4EE)
    val Morning = Color(0xFFE9F1EE)
    val PaleMist = Color(0xFFF4F1EA)
    val White = Color.White

    // The breathing orb looks the same in both themes.
    val OrbCentre = Mist
    val OrbEdge = Color(0xFF6FB3A6)
    val OrbText = DeepTeal
}

/**
 * The design's colour variables. Every colour has a dark and a light value; "ink" is white in
 * dark mode and Deep Teal in light mode, always used at a few fixed opacities.
 */
@Immutable
data class BreatheColorScheme(
    val isDark: Boolean,
    val ink: Color,
    val bgTop: Color,
    val bgBottom: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceSelected: Color,
    val buttonBg: Color,
    val buttonFg: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentFaint: Color,
    val onAccent: Color,
    val glass: Color,
    val sheet: Color,
    val tabSelected: Color,
    /** Soft glow in the top-right corner of the screen background, and its transparent edge. */
    val glowA: Color,
    val glowAEdge: Color,
    /** Soft glow towards the bottom-left of the screen background, and its transparent edge. */
    val glowB: Color,
    val glowBEdge: Color,
    /** Base colour of photo scrims: Night in dark mode, a pale morning mist in light mode. */
    private val scrimBase: Color
) {
    val ink90 get() = ink.copy(alpha = 0.90f)
    val ink85 get() = ink.copy(alpha = 0.85f)
    val ink80 get() = ink.copy(alpha = 0.80f)
    val ink70 get() = ink.copy(alpha = 0.70f)
    val ink60 get() = ink.copy(alpha = 0.60f)
    val ink50 get() = ink.copy(alpha = 0.50f)
    val ink40 get() = ink.copy(alpha = 0.40f)
    val ink30 get() = ink.copy(alpha = 0.30f)
    val ink25 get() = ink.copy(alpha = 0.25f)
    val ink20 get() = ink.copy(alpha = 0.20f)
    val ink15 get() = ink.copy(alpha = 0.15f)
    val ink14 get() = ink.copy(alpha = 0.14f)
    val ink12 get() = ink.copy(alpha = 0.12f)
    val ink10 get() = ink.copy(alpha = 0.10f)
    val ink8 get() = ink.copy(alpha = 0.08f)
    val ink5 get() = ink.copy(alpha = 0.05f)

    /** Photo scrim at the given opacity (the design's $scrim-XX variables). */
    fun scrim(alpha: Float): Color = scrimBase.copy(alpha = alpha)
}

/**
 * Full-screen background behind Home, Progress, Settings, Badges and the walkthrough: the
 * top-to-bottom gradient with two soft glows laid over it, as in the design.
 */
fun Modifier.screenBackground(colors: BreatheColorScheme): Modifier = drawBehind {
    drawRect(Brush.verticalGradient(listOf(colors.bgTop, colors.bgBottom)))
    drawGlow(colors.glowA, colors.glowAEdge, centre = Offset(0.92f, 0.06f), extent = Size(1.5f, 0.7f))
    drawGlow(colors.glowB, colors.glowBEdge, centre = Offset(0.02f, 0.82f), extent = Size(1.4f, 0.6f))
}

/**
 * An elliptical radial glow. [centre] and [extent] (the ellipse's width and height) are fractions
 * of the background's size, matching the design's gradient settings.
 */
private fun DrawScope.drawGlow(colour: Color, edge: Color, centre: Offset, extent: Size) {
    val c = Offset(size.width * centre.x, size.height * centre.y)
    val radiusX = size.width * extent.width / 2
    val radiusY = size.height * extent.height / 2
    clipRect {
        // A circular gradient stretched sideways into an ellipse.
        scale(scaleX = radiusX / radiusY, scaleY = 1f, pivot = c) {
            drawCircle(Brush.radialGradient(listOf(colour, edge), center = c, radius = radiusY), radiusY, c)
        }
    }
}

val DarkBreatheColors = BreatheColorScheme(
    isDark = true,
    ink = Color.White,
    bgTop = BreatheColors.Lake,
    bgBottom = BreatheColors.Night,
    surface = Color(0x0DFFFFFF),
    surfaceRaised = Color(0x1AFFFFFF),
    surfaceSelected = Color(0x24FFFFFF),
    buttonBg = Color.White,
    buttonFg = BreatheColors.DeepTeal,
    accent = BreatheColors.Mint,
    accentSoft = Color(0x669FD3C7),
    accentFaint = Color(0x1A9FD3C7),
    onAccent = BreatheColors.DeepTeal,
    glass = Color(0xB30B2A33),
    sheet = Color(0xFF12404B),
    tabSelected = Color(0x24FFFFFF),
    glowA = Color(0x5C9FD3C7),
    glowAEdge = Color(0x009FD3C7),
    glowB = Color(0x8C4A6BA8),
    glowBEdge = Color(0x003C5C94),
    scrimBase = BreatheColors.Night
)

val LightBreatheColors = BreatheColorScheme(
    isDark = false,
    ink = BreatheColors.DeepTeal,
    bgTop = BreatheColors.Morning,
    bgBottom = BreatheColors.Paper,
    surface = Color(0xB3FFFFFF),
    surfaceRaised = Color(0xCCFFFFFF),
    surfaceSelected = Color.White,
    buttonBg = BreatheColors.DeepTeal,
    buttonFg = Color.White,
    accent = BreatheColors.DeepMint,
    accentSoft = Color(0x593E8F84),
    accentFaint = Color(0x1F3E8F84),
    onAccent = Color.White,
    glass = Color(0xD9FFFFFF),
    sheet = Color.White,
    tabSelected = Color(0x140B2A33),
    glowA = Color(0xCCB9E0D5),
    glowAEdge = Color(0x00B9E0D5),
    glowB = Color(0xB3F2D9C2),
    glowBEdge = Color(0x00F2D9C2),
    scrimBase = BreatheColors.PaleMist
)

private val LocalBreatheColors = staticCompositionLocalOf { DarkBreatheColors }

val Cormorant = FontFamily(
    Font(R.font.cormorant_garamond_medium, FontWeight.Medium),
    Font(R.font.cormorant_garamond_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_garamond_bold, FontWeight.Bold)
)

/** Interface and numbers: art-deco geometry that echoes Cormorant, with level figures. */
val Josefin = FontFamily(
    Font(R.font.josefin_sans_regular, FontWeight.Normal),
    Font(R.font.josefin_sans_medium, FontWeight.Medium),
    Font(R.font.josefin_sans_semibold, FontWeight.SemiBold)
)

private fun style(
    family: FontFamily,
    size: Int,
    weight: FontWeight,
    lineHeight: TextUnit = 1.3.em,
    letterSpacing: TextUnit = TextUnit.Unspecified
) = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    fontWeight = weight,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

/**
 * Type scale from the design document. Cormorant Garamond is the voice for words; Josefin Sans
 * handles everything you tap or scan, including numbers. Captions start at 13 because Josefin
 * has a small x-height.
 */
object BreatheType {
    // Cormorant Garamond — display and voice
    val Wordmark = style(Cormorant, 56, FontWeight.Bold, 1.1.em, 8.sp)
    val Title = style(Cormorant, 48, FontWeight.SemiBold, 1.em)
    val BadgeTitle = style(Cormorant, 40, FontWeight.SemiBold, 1.05.em)
    val Heading = style(Cormorant, 32, FontWeight.SemiBold, 1.1.em)
    val Phase = style(Cormorant, 32, FontWeight.SemiBold, 1.1.em)
    val Tagline = style(Cormorant, 26, FontWeight.Medium, 1.25.em)

    // Josefin Sans — interface and numbers
    val StatLarge = style(Josefin, 44, FontWeight.SemiBold, 1.em)
    val Stat = style(Josefin, 24, FontWeight.SemiBold, 1.2.em)
    val StatSmall = style(Josefin, 22, FontWeight.SemiBold)
    val Count = style(Josefin, 20, FontWeight.Medium)
    val Button = style(Josefin, 17, FontWeight.SemiBold)
    val CardTitle = style(Josefin, 17, FontWeight.SemiBold)
    val Body = style(Josefin, 16, FontWeight.Normal)
    val BodyStrong = style(Josefin, 16, FontWeight.SemiBold)
    val Label = style(Josefin, 15, FontWeight.Medium)
    val LabelStrong = style(Josefin, 15, FontWeight.SemiBold)
    val LabelRegular = style(Josefin, 15, FontWeight.Normal)
    val Small = style(Josefin, 14, FontWeight.Normal)
    val SmallMedium = style(Josefin, 14, FontWeight.Medium)
    val SmallStrong = style(Josefin, 14, FontWeight.SemiBold)
    val Caption = style(Josefin, 13, FontWeight.Normal)
    val CaptionMedium = style(Josefin, 13, FontWeight.Medium)
    val CaptionStrong = style(Josefin, 13, FontWeight.SemiBold)
    /** Only for chart values, where space is tight. */
    val Micro = style(Josefin, 12, FontWeight.Medium)
    val SectionLabel = style(Josefin, 12, FontWeight.SemiBold, letterSpacing = 1.2.sp)
    val Eyebrow = style(Josefin, 12, FontWeight.SemiBold, letterSpacing = 1.5.sp)
    val Tab = style(Josefin, 11, FontWeight.Medium)
    val TabSelected = style(Josefin, 11, FontWeight.SemiBold)
}

/** Follows the system light/dark setting, as the design specifies. */
@Composable
fun BreatheTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkBreatheColors else LightBreatheColors
    CompositionLocalProvider(LocalBreatheColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme) {
                darkColorScheme(
                    primary = colors.accent,
                    onPrimary = colors.onAccent,
                    background = colors.bgBottom,
                    onBackground = colors.ink,
                    surface = colors.sheet,
                    onSurface = colors.ink
                )
            } else {
                lightColorScheme(
                    primary = colors.accent,
                    onPrimary = colors.onAccent,
                    background = colors.bgBottom,
                    onBackground = colors.ink,
                    surface = colors.sheet,
                    onSurface = colors.ink
                )
            },
            content = content
        )
    }
}

object BreatheTheme {
    val colors: BreatheColorScheme
        @Composable @ReadOnlyComposable get() = LocalBreatheColors.current
}
