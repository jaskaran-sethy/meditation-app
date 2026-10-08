package com.jaskaransethy.breathe.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The Lucide icons used by the Breathe design (https://lucide.dev, ISC License; see
 * assets/licenses/Lucide-LICENSE.txt). Drawn as 24×24 strokes so they tint like any other icon.
 */
object BreatheIcons {
    val ArrowRight = lucide("arrow-right", "M5 12h14", "M12 5l7 7-7 7")
    val Timer = lucide("timer", "M10 2h4", "M12 14l3-3", circle(12f, 14f, 8f))
    val Close = lucide("x", "M18 6 6 18", "M6 6l12 12")
    val Volume = lucide(
        "volume-2",
        "M11 5 6 9H2v6h4l5 4V5z",
        "M15.54 8.46a5 5 0 0 1 0 7.07",
        "M19.07 4.93a10 10 0 0 1 0 14.14"
    )
    val VolumeOff = lucide("volume-x", "M11 5 6 9H2v6h4l5 4V5z", "M22 9l-6 6", "M16 9l6 6")
    val Pause = lucide(
        "pause",
        "M7 4h2a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z",
        "M15 4h2a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1h-2a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z"
    )
    val Play = lucide("play", "M6 3l14 9-14 9z")
    val Check = lucide("check", "M20 6 9 17l-5-5")
    val Minus = lucide("minus", "M5 12h14")
    val Plus = lucide("plus", "M5 12h14", "M12 5v14")
    val Sliders = lucide(
        "sliders-horizontal",
        "M21 4h-7", "M10 4H3", "M21 12h-9", "M8 12H3", "M21 20h-5", "M12 20H3",
        "M14 2v4", "M8 10v4", "M16 18v4"
    )
    val RotateCcw = lucide("rotate-ccw", "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8", "M3 3v5h5")
    val Wind = lucide(
        "wind",
        "M17.7 7.7a2.5 2.5 0 1 1 1.8 4.3H2",
        "M9.6 4.6A2 2 0 1 1 11 8H2",
        "M12.6 19.4A2 2 0 1 0 14 16H2"
    )
    val Chart = lucide("chart-no-axes-column", "M18 20V10", "M12 20V4", "M6 20v-6")
    val Settings = lucide("settings-2", "M20 7h-9", "M14 17H5", circle(17f, 17f, 3f), circle(7f, 7f, 3f))
    val Leaf = lucide(
        "leaf",
        "M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10Z",
        "M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"
    )
    val Bell = lucide("bell", "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10.3 21a1.94 1.94 0 0 0 3.4 0")
    val Vibrate = lucide(
        "vibrate",
        "M2 8l2 2-2 2 2 2-2 2",
        "M22 8l-2 2 2 2-2 2 2 2",
        "M9 5h6a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1z"
    )
    val EyeOff = lucide(
        "eye-off",
        "M9.88 9.88a3 3 0 1 0 4.24 4.24",
        "M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68",
        "M6.61 6.61A13.53 13.53 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61",
        "M2 2l20 20"
    )

    val ChevronRight = lucide("chevron-right", "M9 18l6-6-6-6")
    val Clock = lucide("clock-3", circle(12f, 12f, 10f), "M12 6v6h4.5")
    val Calendar = lucide(
        "calendar",
        "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z",
        "M16 2v4",
        "M8 2v4",
        "M3 10h18"
    )
    val Sun = lucide(
        "sun",
        circle(12f, 12f, 4f),
        "M12 2v2",
        "M12 20v2",
        "M4.93 4.93l1.41 1.41",
        "M17.66 17.66l1.41 1.41",
        "M2 12h2",
        "M20 12h2",
        "M6.34 17.66l-1.41 1.41",
        "M19.07 4.93l-1.41 1.41"
    )

    // Badge medal icons.
    val ChevronLeft = lucide("chevron-left", "M15 18l-6-6 6-6")
    val Sprout = lucide(
        "sprout",
        "M7 20h10",
        "M10 20c5.5-2.5.8-6.4 3-10",
        "M9.5 9.4c1.1.8 1.8 2.2 2.3 3.7-2 .4-3.5.4-4.8-.3-1.2-.6-2.3-1.9-3-4.2 2.8-.5 4.4 0 5.5.8z",
        "M14.1 6a7 7 0 0 0-1.1 4c1.9-.1 3.3-.6 4.3-1.4 1-1 1.6-2.3 1.7-4.6-2.7.1-4 1-4.9 2z"
    )
    val Sunrise = lucide(
        "sunrise",
        "M12 2v8",
        "M4.93 10.93l1.41 1.41",
        "M2 18h2",
        "M20 18h2",
        "M19.07 10.93l-1.41 1.41",
        "M22 22H2",
        "M8 6l4-4 4 4",
        "M16 18a4 4 0 0 0-8 0"
    )
    val Moon = lucide("moon", "M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z")
    val Compass = lucide(
        "compass",
        circle(12f, 12f, 10f),
        "M16.24 7.76l-1.804 5.411a2 2 0 0 1-1.265 1.265L7.76 16.24l1.804-5.411a2 2 0 0 1 1.265-1.265z"
    )
    val Waves = lucide(
        "waves",
        "M2 6c.6.5 1.2 1 2.5 1C7 7 7 5 9.5 5c2.6 0 2.4 2 5 2 2.5 0 2.5-2 5-2 1.3 0 1.9.5 2.5 1",
        "M2 12c.6.5 1.2 1 2.5 1 2.5 0 2.5-2 5-2 2.6 0 2.4 2 5 2 2.5 0 2.5-2 5-2 1.3 0 1.9.5 2.5 1",
        "M2 18c.6.5 1.2 1 2.5 1 2.5 0 2.5-2 5-2 2.6 0 2.4 2 5 2 2.5 0 2.5-2 5-2 1.3 0 1.9.5 2.5 1"
    )
    val Square = lucide("square", "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z")
    val Mountain = lucide("mountain", "M8 3l4 8 5-5 5 15H2L8 3z")

    val Share = lucide(
        "share-2",
        circle(18f, 5f, 3f),
        circle(6f, 12f, 3f),
        circle(18f, 19f, 3f),
        "M8.59 13.51l6.83 3.98",
        "M15.41 6.51l-6.82 3.98"
    )

    // Walkthrough icons.
    val Maximize = lucide("maximize-2", "M15 3h6v6", "M9 21H3v-6", "M21 3l-7 7", "M3 21l7-7")
    val ArrowUpRight = lucide("arrow-up-right", "M7 7h10v10", "M7 17L17 7")
    val ArrowDownLeft = lucide("arrow-down-left", "M17 7L7 17", "M17 17H7V7")
    val CircleCheck = lucide("circle-check", circle(12f, 12f, 10f), "M9 12l2 2 4-4")
    val CalendarRange = lucide(
        "calendar-range",
        "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z",
        "M16 2v4",
        "M3 10h18",
        "M8 2v4",
        "M17 14h-6",
        "M13 18H7",
        "M7 14h.01",
        "M17 18h.01"
    )
    val Eye = lucide("eye", "M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z", circle(12f, 12f, 3f))
    val Pencil = lucide(
        "pencil",
        "M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z",
        "M15 5l4 4"
    )
    val Trash = lucide(
        "trash-2",
        "M3 6h18",
        "M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6",
        "M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2",
        "M10 11v6",
        "M14 11v6"
    )

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

    private fun lucide(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { path ->
                addPath(
                    pathData = addPathNodes(path),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                )
            }
        }.build()
}
