package com.jaskaransethy.breathe.ui.badges

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaskaransethy.breathe.R
import com.jaskaransethy.breathe.data.Badge
import com.jaskaransethy.breathe.data.BadgeProgress
import com.jaskaransethy.breathe.ui.components.Medal
import com.jaskaransethy.breathe.ui.icons.BreatheIcons
import com.jaskaransethy.breathe.ui.theme.BreatheTheme
import com.jaskaransethy.breathe.ui.theme.BreatheType

/** The medal icon for each badge, as in the design's badge table. */
val Badge.icon: ImageVector
    get() = when (this) {
        Badge.SteadyStart -> BreatheIcons.Sprout
        Badge.Unbroken -> BreatheIcons.Timer
        Badge.SevenDays -> BreatheIcons.Sunrise
        Badge.NightTide -> BreatheIcons.Moon
        Badge.FourWinds -> BreatheIcons.Compass
        Badge.DeepWater -> BreatheIcons.Waves
        Badge.SquareMind -> BreatheIcons.Square
        Badge.Mountain -> BreatheIcons.Mountain
    }

@get:StringRes
val Badge.title: Int
    get() = when (this) {
        Badge.SteadyStart -> R.string.badges_name_steady_start
        Badge.Unbroken -> R.string.badges_name_unbroken
        Badge.SevenDays -> R.string.badges_name_seven_days
        Badge.NightTide -> R.string.badges_name_night_tide
        Badge.FourWinds -> R.string.badges_name_four_winds
        Badge.DeepWater -> R.string.badges_name_deep_water
        Badge.SquareMind -> R.string.badges_name_square_mind
        Badge.Mountain -> R.string.badges_name_mountain
    }

/** What the badge takes, as one sentence (e.g. "Breathe on 7 different days within any 14-day window."). */
@get:StringRes
val Badge.requirement: Int
    get() = when (this) {
        Badge.SteadyStart -> R.string.badges_requirement_steady_start
        Badge.Unbroken -> R.string.badges_requirement_unbroken
        Badge.SevenDays -> R.string.badges_requirement_seven_days
        Badge.NightTide -> R.string.badges_requirement_night_tide
        Badge.FourWinds -> R.string.badges_requirement_four_winds
        Badge.DeepWater -> R.string.badges_requirement_deep_water
        Badge.SquareMind -> R.string.badges_requirement_square_mind
        Badge.Mountain -> R.string.badges_requirement_mountain
    }

/** The warm sentence on the Badge Earned sheet, naming the practice that earned it. */
@get:StringRes
val Badge.earnedMessage: Int
    get() = when (this) {
        Badge.SteadyStart -> R.string.badges_message_steady_start
        Badge.Unbroken -> R.string.badges_message_unbroken
        Badge.SevenDays -> R.string.badges_message_seven_days
        Badge.NightTide -> R.string.badges_message_night_tide
        Badge.FourWinds -> R.string.badges_message_four_winds
        Badge.DeepWater -> R.string.badges_message_deep_water
        Badge.SquareMind -> R.string.badges_message_square_mind
        Badge.Mountain -> R.string.badges_message_mountain
    }

@PluralsRes
private fun remainingPlural(badge: Badge): Int = when (badge) {
    Badge.SteadyStart, Badge.Unbroken -> R.plurals.badges_remaining_sessions
    Badge.SevenDays, Badge.Mountain -> R.plurals.badges_remaining_days
    Badge.NightTide -> R.plurals.badges_remaining_night_sessions
    Badge.FourWinds -> R.plurals.badges_remaining_patterns
    Badge.DeepWater -> R.plurals.badges_remaining_minutes
    Badge.SquareMind -> R.plurals.badges_remaining_box
}

@PluralsRes
private fun progressPlural(badge: Badge): Int = when (badge) {
    Badge.SteadyStart, Badge.Unbroken -> R.plurals.badges_progress_sessions
    Badge.SevenDays, Badge.Mountain -> R.plurals.badges_progress_days
    Badge.NightTide -> R.plurals.badges_progress_nights
    Badge.FourWinds -> R.plurals.badges_progress_patterns
    Badge.DeepWater -> R.plurals.badges_progress_minutes
    Badge.SquareMind -> R.plurals.badges_progress_box
}

/** What is left, e.g. "1 day to go", "15 min to go"; "Earned" once earned. */
@Composable
fun badgeRemainingText(progress: BadgeProgress): String =
    if (progress.earned) {
        stringResource(R.string.badges_earned)
    } else {
        pluralStringResource(remainingPlural(progress.badge), progress.remaining, progress.remaining)
    }

/** Progress so far, e.g. "6 of 7 days", "85 of 100 min", "0 of 10 Box"; "Earned" once earned. */
@Composable
fun badgeProgressText(progress: BadgeProgress): String =
    if (progress.earned) {
        stringResource(R.string.badges_earned)
    } else {
        pluralStringResource(progressPlural(progress.badge), progress.target, progress.current, progress.target)
    }

/**
 * A badge in a grid or row: medal, name and a status line. The subtitle is accent and semibold
 * once earned, quieter when the badge hasn't been started. Read aloud as one item.
 */
@Composable
fun BadgeTile(progress: BadgeProgress, subtitle: String, modifier: Modifier = Modifier) {
    val colors = BreatheTheme.colors
    val name = stringResource(progress.badge.title)
    val notStarted = !progress.earned && progress.current == 0
    val earnedText = stringResource(R.string.badges_earned)
    val description = when {
        progress.earned && subtitle == earnedText ->
            stringResource(R.string.badges_tile_description, name, subtitle)
        progress.earned -> stringResource(R.string.badges_tile_description_earned, name, subtitle)
        notStarted -> stringResource(R.string.badges_tile_description_not_started, name, subtitle)
        else -> stringResource(R.string.badges_tile_description, name, subtitle)
    }
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Medal(icon = progress.badge.icon, earned = progress.earned, progress = progress.fraction)
        Text(
            text = name,
            style = BreatheType.SmallStrong,
            color = colors.ink,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = if (progress.earned) BreatheType.CaptionStrong else BreatheType.Caption,
            color = when {
                progress.earned -> colors.accent
                notStarted -> colors.ink50
                else -> colors.ink70
            },
            textAlign = TextAlign.Center
        )
    }
}
