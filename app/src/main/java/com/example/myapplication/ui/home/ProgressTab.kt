package com.example.myapplication.ui.home

import com.example.myapplication.ui.components.shareText
import com.example.myapplication.ui.components.IconCircleButton
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.data.Badge
import com.example.myapplication.data.BadgeProgress
import com.example.myapplication.data.BadgeRules
import com.example.myapplication.data.BreatheStore
import com.example.myapplication.data.DayTotal
import com.example.myapplication.data.Mood
import com.example.myapplication.data.PracticeStats
import com.example.myapplication.data.WeekProgress
import com.example.myapplication.ui.badges.BadgeTile
import com.example.myapplication.ui.badges.badgeRemainingText
import com.example.myapplication.ui.icons.BreatheIcons
import com.example.myapplication.ui.theme.BreatheColorScheme
import com.example.myapplication.ui.theme.BreatheTheme
import com.example.myapplication.ui.theme.BreatheType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val ChartHeight = 150.dp
private val BarMaxHeight = ChartHeight - 24.dp // leaves room for the value label above the tallest bar
private val BarStubHeight = 4.dp
private val ColumnWidth = 32.dp
private const val MaxMoodSegments = 14
private const val ClosestBadgeCount = 3

private val MinTouchTarget = 48.dp
/**
 * The "See all" link is a 48dp touch target, but only about 20dp of it is visible. The badges
 * section pulls its top padding and gap in by the difference so it still reads as the design's
 * 20dp / 16dp spacing.
 */
private val SeeAllInset = 14.dp

/** One day of the week being shown, with its labels already resolved for the locale. */
private class WeekDay(
    val total: DayTotal,
    val isToday: Boolean,
    val isFuture: Boolean,
    val letter: String,
    val name: String
) {
    val practised get() = total.sessions > 0 || total.seconds > 0
    /** Rounded to the nearest minute so a two-minute session shows 2. */
    val minutes get() = (total.seconds + 30) / 60
}

@Composable
fun ProgressTab(store: BreatheStore, onOpenBadges: () -> Unit) {
    val progress = remember { store.weekProgress() }
    val badges = remember { BadgeRules.evaluate(store.sessionLog(), store.today(), store.earnedBadges) }
    ProgressContent(progress, badges, onOpenBadges)
}

/**
 * The week view rewards showing up: there are no scores, levels or comparisons, and a missed
 * day simply resets the streak without any warning.
 */
@Composable
private fun ProgressContent(progress: WeekProgress, badges: List<BadgeProgress>, onOpenBadges: () -> Unit) {
    val colors = BreatheTheme.colors
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val days = remember(progress, locale) { weekDays(progress, locale) }
    val range = remember(progress.weekStart, locale) {
        // Epoch day × MS_PER_DAY is that day's midnight UTC. End at Sunday noon so the range
        // closes on Sunday rather than spilling into the next Monday.
        val start = progress.weekStart * PracticeStats.MS_PER_DAY
        val end = (progress.weekStart + 6) * PracticeStats.MS_PER_DAY + PracticeStats.MS_PER_DAY / 2
        DateUtils.formatDateRange(
            context, start, end,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or
                DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_UTC
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.progress_week_range, range),
                style = BreatheType.Small,
                color = colors.ink70
            )
            Text(
                stringResource(R.string.tab_progress),
                style = BreatheType.Heading,
                color = colors.ink,
                modifier = Modifier.semantics { heading() }
            )
        }
        StreakCard(progress, days)
        MinutesChart(progress, days)
        MoodSection(progress.moods)
        ClosestBadgesSection(badges, onOpenBadges)
    }
}

@Composable
private fun StreakCard(progress: WeekProgress, days: List<WeekDay>) {
    val colors = BreatheTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.ink12, shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            val streakDescription = pluralStringResource(
                R.plurals.progress_streak_description, progress.streakDays, progress.streakDays
            )
            Row(
                modifier = Modifier.clearAndSetSemantics { contentDescription = streakDescription },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(progress.streakDays.toString(), style = BreatheType.StatLarge, color = colors.ink)
                Row(
                    modifier = Modifier.padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(BreatheIcons.Leaf, null, Modifier.size(16.dp), tint = colors.accent)
                    Text(
                        pluralStringResource(R.plurals.progress_day_streak_label, progress.streakDays),
                        style = BreatheType.Label,
                        color = colors.ink
                    )
                }
            }
            Row(
                modifier = Modifier.padding(start = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    pluralStringResource(
                        R.plurals.progress_best_streak, progress.bestStreakDays, progress.bestStreakDays
                    ),
                    style = BreatheType.Small,
                    color = colors.ink70
                )
                // Sharing is offered, never prompted; only once there is a streak to share.
                if (progress.streakDays > 0) {
                    val context = LocalContext.current
                    val shareText = pluralStringResource(
                        R.plurals.share_streak_text, progress.streakDays, progress.streakDays
                    )
                    IconCircleButton(
                        icon = BreatheIcons.Share,
                        contentDescription = stringResource(R.string.share_streak_action),
                        onClick = { context.shareText(shareText) },
                        size = 40.dp,
                        iconSize = 18.dp
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { day -> WeekDayMark(day) }
        }
    }
}

@Composable
private fun WeekDayMark(day: WeekDay) {
    val colors = BreatheTheme.colors
    val description = when {
        day.isToday && day.practised -> stringResource(R.string.progress_day_today_practised, day.name)
        day.isToday -> stringResource(R.string.progress_day_today_not_yet, day.name)
        day.practised -> stringResource(R.string.progress_day_practised, day.name)
        day.isFuture -> day.name
        else -> stringResource(R.string.progress_day_not_practised, day.name)
    }
    Column(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (day.practised) colors.accent else colors.surface)
                // Today is always outlined, practised or not.
                .then(
                    if (day.isToday) {
                        Modifier.border(2.dp, colors.ink, CircleShape)
                    } else {
                        Modifier.border(1.dp, colors.ink12, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (day.practised) {
                Icon(BreatheIcons.Check, null, Modifier.size(16.dp), tint = colors.onAccent)
            }
        }
        DayLetterText(day)
    }
}

@Composable
private fun DayLetterText(day: WeekDay) {
    val colors = BreatheTheme.colors
    Text(
        day.letter,
        style = if (day.isToday) BreatheType.CaptionStrong else BreatheType.CaptionMedium,
        color = if (day.isToday) colors.ink else colors.ink60
    )
}

@Composable
private fun MinutesChart(progress: WeekProgress, days: List<WeekDay>) {
    val colors = BreatheTheme.colors
    val maxMinutes = days.maxOf { it.minutes }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                stringResource(R.string.progress_minutes_breathed),
                style = BreatheType.LabelStrong,
                color = colors.ink,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                pluralStringResource(
                    R.plurals.progress_week_total,
                    progress.sessionsThisWeek,
                    // Sum the per-day rounded minutes so the total matches the bars.
                    stringResource(R.string.minutes_short, days.sumOf { it.minutes }),
                    progress.sessionsThisWeek
                ),
                style = BreatheType.Small,
                color = colors.ink70,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        // The labels row is part of the visual chart only; each bar column carries its own
        // spoken description including the day name.
        Column {
            val baseline = colors.ink12
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ChartHeight)
                    .drawBehind {
                        val y = size.height - 0.5.dp.toPx()
                        drawLine(
                            baseline,
                            Offset(0f, y),
                            Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .padding(start = 4.dp, end = 4.dp, bottom = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEach { day -> MinutesBar(day, maxMinutes) }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .clearAndSetSemantics { },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEach { day ->
                    Box(Modifier.width(ColumnWidth), contentAlignment = Alignment.Center) {
                        DayLetterText(day)
                    }
                }
            }
        }
    }
}

@Composable
private fun MinutesBar(day: WeekDay, maxMinutes: Int) {
    val colors = BreatheTheme.colors
    val description = if (day.isToday) {
        pluralStringResource(R.plurals.progress_today_minutes, day.minutes, day.name, day.minutes)
    } else {
        pluralStringResource(R.plurals.progress_day_minutes, day.minutes, day.name, day.minutes)
    }
    val barHeight: Dp = if (day.minutes > 0 && maxMinutes > 0) {
        (BarMaxHeight * (day.minutes.toFloat() / maxMinutes)).coerceAtLeast(BarStubHeight)
    } else {
        BarStubHeight
    }
    val barColor = when {
        day.minutes == 0 -> colors.ink10
        day.isToday -> colors.accent
        else -> colors.accentSoft
    }
    Column(
        modifier = Modifier
            .width(ColumnWidth)
            .fillMaxHeight()
            .clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (day.minutes > 0) {
            Text(
                day.minutes.toString(),
                style = BreatheType.Micro,
                color = if (day.isToday) colors.ink else colors.ink60,
                maxLines = 1,
                softWrap = false
            )
        }
        Box(
            Modifier
                .width(24.dp)
                .height(barHeight)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(barColor)
        )
    }
}

/** A section below the chart, separated from what's above by a hairline. */
private fun Modifier.topDivider(color: Color): Modifier = drawBehind {
    val y = 0.5.dp.toPx()
    drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
}

@Composable
private fun MoodSection(moods: List<Mood>) {
    val colors = BreatheTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .topDivider(colors.ink12)
            .padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                stringResource(R.string.progress_mood_title),
                style = BreatheType.LabelStrong,
                color = colors.ink,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .semantics { heading() }
            )
            if (moods.isNotEmpty()) {
                val calmer = moods.count { it == Mood.Calmer }
                Text(
                    if (calmer > 0) {
                        pluralStringResource(R.plurals.progress_mood_calmer_summary, moods.size, calmer, moods.size)
                    } else {
                        pluralStringResource(R.plurals.progress_mood_checkins, moods.size, moods.size)
                    },
                    style = BreatheType.Small,
                    color = colors.ink70,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }

        // Early returns inside composable lambdas break Compose's group tracking, so branch instead.
        if (moods.isEmpty()) {
            Text(
                stringResource(R.string.progress_mood_empty),
                style = BreatheType.Caption,
                color = colors.ink70
            )
        } else {
            MoodBreakdown(moods)
        }
    }
}

@Composable
private fun MoodBreakdown(moods: List<Mood>) {
    val colors = BreatheTheme.colors
    val shown = moods.takeLast(MaxMoodSegments)
    val breakdown = stringResource(
        R.string.progress_mood_breakdown,
        shown.count { it == Mood.Calmer },
        shown.count { it == Mood.Same },
        shown.count { it == Mood.StillTense }
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = breakdown },
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val shape = RoundedCornerShape(4.dp)
        shown.forEach { mood ->
            Box(
                Modifier
                    .weight(1f)
                    .height(8.dp)
                    .moodFill(mood, shape, colors)
            )
        }
    }
    // The bar above already speaks the counts, so the legend stays visual only.
    Row(
        modifier = Modifier.clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Mood.entries.filter { it in shown }.forEach { mood ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).moodFill(mood, CircleShape, colors))
                Text(
                    stringResource(mood.label()),
                    style = BreatheType.Caption,
                    color = colors.ink70
                )
            }
        }
    }
}

/** Mood colours: Calmer is the accent, The same a soft ink, Still tense a faint outline. */
private fun Modifier.moodFill(mood: Mood, shape: Shape, colors: BreatheColorScheme): Modifier {
    val fill: Color = when (mood) {
        Mood.Calmer -> colors.accent
        Mood.Same -> colors.ink20
        Mood.StillTense -> colors.ink10
    }
    val base = clip(shape).background(fill)
    return if (mood == Mood.StillTense) base.border(1.dp, colors.ink20, shape) else base
}

private fun Mood.label(): Int = when (this) {
    Mood.Calmer -> R.string.mood_calmer
    Mood.Same -> R.string.mood_same
    Mood.StillTense -> R.string.mood_still_tense
}

/** The unearned badges nearest completion, with a link to the full Badges screen. */
@Composable
private fun ClosestBadgesSection(badges: List<BadgeProgress>, onOpenBadges: () -> Unit) {
    val colors = BreatheTheme.colors
    val closest = remember(badges) { BadgeRules.closest(badges, ClosestBadgeCount) }
    val earnedCount = badges.count { it.earned }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .topDivider(colors.ink12)
            .padding(top = 20.dp - SeeAllInset),
        verticalArrangement = Arrangement.spacedBy(16.dp - SeeAllInset)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.progress_badges_title),
                style = BreatheType.LabelStrong,
                color = colors.ink,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .semantics { heading() }
            )
            SeeAllBadgesLink(earnedCount, badges.size, onOpenBadges)
        }
        if (closest.isEmpty()) {
            Text(
                stringResource(R.string.progress_badges_all_earned),
                style = BreatheType.Caption,
                color = colors.ink70
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                closest.forEach { badge ->
                    BadgeTile(
                        progress = badge,
                        subtitle = badgeRemainingText(badge),
                        modifier = Modifier.weight(1f)
                    )
                }
                // Keep tiles a third of the width each when fewer than three remain.
                repeat(ClosestBadgeCount - closest.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SeeAllBadgesLink(earned: Int, total: Int, onClick: () -> Unit) {
    val colors = BreatheTheme.colors
    val description = stringResource(R.string.progress_badges_see_all_description, earned, total)
    val actionLabel = stringResource(R.string.progress_badges_open)
    Row(
        modifier = Modifier
            .heightIn(min = MinTouchTarget)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClickLabel = actionLabel, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(start = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The row's description replaces the visual "3 of 8 · See all" for screen readers.
        Text(
            stringResource(R.string.progress_badges_see_all, earned, total),
            style = BreatheType.Caption,
            color = colors.ink70,
            modifier = Modifier.clearAndSetSemantics { }
        )
        Icon(BreatheIcons.ChevronRight, null, Modifier.size(16.dp), tint = colors.ink50)
    }
}

private fun weekDays(progress: WeekProgress, locale: Locale): List<WeekDay> {
    // Epoch days map to midnight UTC, so the formatters must use UTC too.
    val utc = TimeZone.getTimeZone("UTC")
    val letterFormat = SimpleDateFormat("EEEEE", locale).apply { timeZone = utc }
    val nameFormat = SimpleDateFormat("EEEE", locale).apply { timeZone = utc }
    return progress.days.mapIndexed { index, total ->
        val day = progress.weekStart + index
        val date = Date(day * PracticeStats.MS_PER_DAY)
        WeekDay(
            total = total,
            isToday = day == progress.today,
            isFuture = day > progress.today,
            letter = letterFormat.format(date),
            name = nameFormat.format(date)
        )
    }
}

// Week of Monday 6 Oct 2025 (epoch day 20367), viewed on the Thursday.
private val PreviewProgress = WeekProgress(
    weekStart = 20367,
    today = 20370,
    days = listOf(
        DayTotal(300, 1), DayTotal(600, 2), DayTotal(300, 1), DayTotal(300, 1),
        DayTotal(0, 0), DayTotal(0, 0), DayTotal(0, 0)
    ),
    streakDays = 4,
    bestStreakDays = 9,
    minutesThisWeek = 25,
    sessionsThisWeek = 5,
    moods = listOf(Mood.Calmer, Mood.Calmer, Mood.Same, Mood.Calmer, Mood.Calmer)
)

private val PreviewBadges = listOf(
    BadgeProgress(Badge.SteadyStart, 1, 1, earned = true, earnedOn = 20360),
    BadgeProgress(Badge.Unbroken, 3, 3, earned = true, earnedOn = 20362),
    BadgeProgress(Badge.SevenDays, 6, 7, earned = false, earnedOn = null),
    BadgeProgress(Badge.NightTide, 5, 5, earned = true, earnedOn = 20365),
    BadgeProgress(Badge.FourWinds, 3, 4, earned = false, earnedOn = null),
    BadgeProgress(Badge.DeepWater, 85, 100, earned = false, earnedOn = null),
    BadgeProgress(Badge.SquareMind, 2, 10, earned = false, earnedOn = null),
    BadgeProgress(Badge.Mountain, 0, 1, earned = false, earnedOn = null)
)

@Preview(widthDp = 390, heightDp = 1100)
@Composable
private fun ProgressContentPreview() {
    BreatheTheme {
        Box(Modifier.fillMaxSize().background(BreatheTheme.colors.background)) {
            ProgressContent(PreviewProgress, PreviewBadges, onOpenBadges = {})
        }
    }
}

@Preview(widthDp = 390, heightDp = 1100)
@Composable
private fun ProgressContentLightPreview() {
    BreatheTheme(darkTheme = false) {
        Box(Modifier.fillMaxSize().background(BreatheTheme.colors.background)) {
            ProgressContent(PreviewProgress, PreviewBadges, onOpenBadges = {})
        }
    }
}
