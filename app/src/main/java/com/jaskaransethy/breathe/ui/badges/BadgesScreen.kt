package com.jaskaransethy.breathe.ui.badges

import com.jaskaransethy.breathe.ui.components.shareText
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.jaskaransethy.breathe.R
import com.jaskaransethy.breathe.data.Badge
import com.jaskaransethy.breathe.data.BadgeProgress
import com.jaskaransethy.breathe.data.BadgeRules
import com.jaskaransethy.breathe.data.BreatheStore
import com.jaskaransethy.breathe.data.SessionRecord
import com.jaskaransethy.breathe.ui.components.IconCircleButton
import com.jaskaransethy.breathe.ui.components.Medal
import com.jaskaransethy.breathe.ui.icons.BreatheIcons
import com.jaskaransethy.breathe.ui.theme.BreatheTheme
import com.jaskaransethy.breathe.ui.theme.BreatheType

private const val GridColumns = 3

/** Every badge with its live progress, and "Next up" spelling out the one thing left to do. */
@Composable
fun BadgesScreen(store: BreatheStore, onBack: () -> Unit) {
    val colors = BreatheTheme.colors
    val records = remember { store.sessionLog() }
    val today = remember { store.today() }
    val progress = remember { BadgeRules.evaluate(records, today, store.earnedBadges) }
    val next = remember(progress) { BadgeRules.closest(progress, 1).firstOrNull() }
    // Earned first, then the closest to completion, as in the design.
    val ordered = remember(progress) {
        progress.filter { it.earned } + progress.filter { !it.earned }.sortedByDescending { it.fraction }
    }

    BackHandler(onBack = onBack)

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            val earnedTitles = progress.filter { it.earned }.map { stringResource(it.badge.title) }
            val shareText = pluralStringResource(
                R.plurals.share_badges_text,
                earnedTitles.size,
                earnedTitles.size,
                progress.size,
                earnedTitles.joinToString(", ")
            )
            val context = LocalContext.current
            TopBar(
                onBack = onBack,
                // Offered only once there is something to share.
                onShare = if (earnedTitles.isNotEmpty()) ({ context.shareText(shareText) }) else null
            )
            Header(earned = progress.count { it.earned }, total = progress.size)
            if (next != null) {
                NextUpCard(next, records, today)
            } else {
                Text(
                    text = stringResource(R.string.badges_all_earned),
                    style = BreatheType.Body,
                    color = colors.ink80
                )
            }
            Text(
                text = stringResource(R.string.badges_all_badges),
                style = BreatheType.SectionLabel,
                color = colors.ink60,
                modifier = Modifier.semantics { heading() }
            )
            BadgeGrid(ordered)
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, onShare: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircleButton(
            icon = BreatheIcons.ChevronLeft,
            contentDescription = stringResource(R.string.badges_back),
            onClick = onBack,
            iconSize = 22.dp
        )
        Text(
            text = stringResource(R.string.badges_back_label),
            style = BreatheType.LabelRegular,
            color = BreatheTheme.colors.ink70,
            // The button already says "Back to Progress".
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics { }
        )
        if (onShare != null) {
            IconCircleButton(
                icon = BreatheIcons.Share,
                contentDescription = stringResource(R.string.share_badges_action),
                onClick = onShare,
                iconSize = 20.dp
            )
        }
    }
}

@Composable
private fun Header(earned: Int, total: Int) {
    val colors = BreatheTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.badges_title),
            style = BreatheType.Heading,
            color = colors.ink,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = pluralStringResource(R.plurals.badges_header_summary, total, earned, total),
            style = BreatheType.Small.copy(lineHeight = 1.45.em),
            color = colors.ink70
        )
    }
}

@Composable
private fun NextUpCard(next: BadgeProgress, records: List<SessionRecord>, today: Long) {
    val colors = BreatheTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val progressText = badgeProgressText(next)
    val hint = nextUpHint(next, records, today)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.accent, shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.badges_next_up),
            style = BreatheType.SectionLabel,
            color = colors.accent,
            modifier = Modifier.semantics { heading() }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Medal(icon = next.badge.icon, earned = false, progress = next.fraction)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(next.badge.title),
                    style = BreatheType.CardTitle.copy(fontSize = 18.sp),
                    color = colors.ink
                )
                Text(
                    text = stringResource(next.badge.requirement),
                    style = BreatheType.Small.copy(lineHeight = 1.4.em),
                    color = colors.ink70
                )
            }
        }
        Column(
            modifier = Modifier.clearAndSetSemantics { contentDescription = "$progressText. $hint" },
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProgressTrack(next.fraction)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(progressText, style = BreatheType.CaptionStrong, color = colors.ink)
                Text(
                    text = hint,
                    style = BreatheType.Caption,
                    color = colors.ink60,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ProgressTrack(fraction: Float) {
    val colors = BreatheTheme.colors
    val shape = RoundedCornerShape(3.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(shape)
            .background(colors.ink10)
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(shape)
                    .background(colors.accent)
            )
        }
    }
}

/** The one thing left to do for [progress], in a few words. */
@Composable
private fun nextUpHint(progress: BadgeProgress, records: List<SessionRecord>, today: Long): String {
    val remaining = progress.remaining
    return when (progress.badge) {
        Badge.SevenDays, Badge.Mountain -> if (remaining == 1) {
            val doneToday = records.any { it.day == today && it.isFull }
            stringResource(if (doneToday) R.string.badges_hint_tomorrow else R.string.badges_hint_today)
        } else {
            pluralStringResource(R.plurals.badges_hint_more_days, remaining, remaining)
        }
        Badge.SteadyStart -> pluralStringResource(R.plurals.badges_hint_steady_start, remaining, remaining)
        Badge.Unbroken -> stringResource(R.string.badges_hint_unbroken)
        Badge.NightTide -> pluralStringResource(R.plurals.badges_hint_night_tide, remaining, remaining)
        Badge.FourWinds -> {
            val missing = BadgeRules.fullSessionsByPattern(records)
                .filterValues { it < BadgeRules.FOUR_WINDS_PER_PATTERN }
            if (missing.size == 1) {
                val (pattern, done) = missing.entries.first()
                val left = BadgeRules.FOUR_WINDS_PER_PATTERN - done
                pluralStringResource(
                    R.plurals.badges_hint_four_winds_one_pattern, left, left, stringResource(pattern.title)
                )
            } else {
                pluralStringResource(R.plurals.badges_hint_four_winds, missing.size, missing.size)
            }
        }
        Badge.DeepWater -> pluralStringResource(R.plurals.badges_hint_deep_water, remaining, remaining)
        Badge.SquareMind -> pluralStringResource(R.plurals.badges_hint_square_mind, remaining, remaining)
    }
}

@Composable
private fun BadgeGrid(progress: List<BadgeProgress>) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        progress.chunked(GridColumns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { item ->
                    BadgeTile(item, badgeProgressText(item), Modifier.weight(1f))
                }
                repeat(GridColumns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
