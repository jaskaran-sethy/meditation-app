package com.jaskaransethy.breathe.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jaskaransethy.breathe.R
import com.jaskaransethy.breathe.ui.icons.BreatheIcons
import com.jaskaransethy.breathe.ui.theme.BreatheTheme
import com.jaskaransethy.breathe.ui.theme.BreatheType

/** Full-bleed lake photo with a scrim on top: dark Night in dark mode, pale mist in light mode. */
@Composable
fun PhotoBackground(
    overlay: Brush,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.fillMaxSize().background(BreatheTheme.colors.bgBottom)) {
        Image(
            painter = painterResource(R.drawable.lake_photo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().background(overlay))
        content()
    }
}

/** 56 dp pill — the one primary action per screen. White on dark, Deep Teal on light. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showArrow: Boolean = true,
    elevated: Boolean = false
) {
    val colors = BreatheTheme.colors
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .then(
                if (elevated) Modifier.shadow(16.dp, shape, spotColor = Color.Black.copy(alpha = 0.25f))
                else Modifier
            )
            .clip(shape)
            .background(colors.buttonBg)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = BreatheType.Button, color = colors.buttonFg)
        if (showArrow) {
            Icon(BreatheIcons.ArrowRight, null, Modifier.size(18.dp), tint = colors.buttonFg)
        }
    }
}

/** 44 dp translucent circle holding a single icon. */
@Composable
fun IconCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    background: Color = BreatheTheme.colors.surfaceRaised,
    tint: Color = BreatheTheme.colors.ink
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, Modifier.size(iconSize), tint = tint)
    }
}

/** Segmented 2 / 5 / 10 minute control; the selected segment uses the primary button colours. */
@Composable
fun LengthControl(
    options: List<Int>,
    selected: Int,
    label: @Composable (Int) -> String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BreatheTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) colors.buttonBg else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(option) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label(option),
                    style = if (isSelected) BreatheType.LabelStrong else BreatheType.Label,
                    color = if (isSelected) colors.buttonFg else colors.ink80
                )
            }
        }
    }
}

/**
 * A breathing rhythm as counts separated by small dots, e.g. 4 · 7 · 8. Read aloud as the
 * plain [description] (e.g. "4 7 8").
 */
@Composable
fun Rhythm(
    counts: List<Int>,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    gap: Dp = 6.dp
) {
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = counts.joinToString(" ") },
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        counts.forEachIndexed { index, count ->
            if (index > 0) Box(Modifier.size(3.dp).background(color, CircleShape))
            Text(count.toString(), style = style, color = color)
        }
    }
}

/** A breathing pattern choice: name, purpose, rhythm and a radio. */
@Composable
fun PatternCard(
    title: String,
    description: String,
    rhythm: List<Int>,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BreatheTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.surfaceSelected else colors.surface)
            .border(1.dp, if (selected) colors.ink70 else colors.ink12, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 19.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = BreatheType.CardTitle, color = colors.ink)
            Text(description, style = BreatheType.Small, color = colors.ink70)
        }
        Rhythm(rhythm, BreatheType.Rhythm, if (selected) colors.ink else colors.ink70)
        Radio(selected)
    }
}

@Composable
private fun Radio(selected: Boolean) {
    val colors = BreatheTheme.colors
    Box(
        modifier = Modifier
            .size(20.dp)
            .border(1.5.dp, if (selected) colors.ink else colors.ink40, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) Box(Modifier.size(10.dp).background(colors.ink, CircleShape))
    }
}

/** 44 dp outline chip; the selected chip is filled. */
@Composable
fun MoodChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BreatheTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(if (selected) colors.surfaceSelected else Color.Transparent)
            .border(BorderStroke(1.dp, if (selected) colors.ink else colors.ink30), shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = if (selected) BreatheType.SmallStrong else BreatheType.SmallMedium,
            color = if (selected) colors.ink else colors.ink80,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Badge medal (design component "Badge Medal"): a thin ring around a disc with an icon.
 * Earned: solid accent disc, full ring. In progress: the ring fills clockwise from the top.
 * Not started: outline only. [progress] is 0..1 and ignored once [earned].
 */
@Composable
fun Medal(
    icon: ImageVector,
    earned: Boolean,
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val colors = BreatheTheme.colors
    val started = earned || progress > 0f
    val scale = size / 64.dp
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            // Ring is the outer 10% of the radius (inner radius 0.9 in the design).
            val ringWidth = this.size.minDimension * 0.05f
            val inset = ringWidth / 2
            val arcSize = Size(this.size.width - ringWidth, this.size.height - ringWidth)
            val topLeft = Offset(inset, inset)
            drawArc(colors.ink10, 0f, 360f, false, topLeft, arcSize, style = Stroke(ringWidth))
            val sweep = if (earned) 360f else 360f * progress.coerceIn(0f, 1f)
            if (sweep > 0f) {
                drawArc(colors.accent, -90f, sweep, false, topLeft, arcSize, style = Stroke(ringWidth))
            }
        }
        Box(
            Modifier
                .size(50.dp * scale)
                .background(
                    when {
                        earned -> colors.accent
                        started -> colors.surfaceRaised
                        else -> colors.surface
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                Modifier.size(24.dp * scale),
                tint = when {
                    earned -> colors.onAccent
                    started -> colors.ink
                    else -> colors.ink40
                }
            )
        }
    }
}
