package com.example.myapplication.ui.badges

import com.example.myapplication.ui.components.shareText
import com.example.myapplication.ui.icons.BreatheIcons
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.myapplication.R
import com.example.myapplication.data.Badge
import com.example.myapplication.data.BadgeProgress
import com.example.myapplication.data.BadgeRules
import com.example.myapplication.data.PresetPattern
import com.example.myapplication.data.BreatheStore
import com.example.myapplication.data.SessionRecord
import com.example.myapplication.data.shouldReduceMotion
import com.example.myapplication.ui.components.Medal
import com.example.myapplication.ui.components.PrimaryButton
import com.example.myapplication.ui.theme.BreatheTheme
import com.example.myapplication.ui.theme.BreatheType

/**
 * A calm sheet over the Complete screen, shown once after the session that earned [badges]. It
 * names the practice that earned each badge and shows the proof. Several badges are shown one
 * after another; Continue on the last calls [onDone]. Fills its parent and blocks touches below.
 */
@Composable
fun BadgeEarnedSheet(badges: List<Badge>, store: BreatheStore, onDone: () -> Unit, onViewAll: () -> Unit) {
    if (badges.isEmpty()) {
        LaunchedEffect(Unit) { onDone() }
    } else {
        BadgeEarnedOverlay(badges, store, onDone, onViewAll)
    }
}

@Composable
private fun BadgeEarnedOverlay(
    badges: List<Badge>,
    store: BreatheStore,
    onDone: () -> Unit,
    onViewAll: () -> Unit
) {
    val colors = BreatheTheme.colors
    val context = LocalContext.current
    val reduceMotion = remember { shouldReduceMotion(context, store) }
    val records = remember { store.sessionLog() }
    val today = remember { store.today() }
    // The badges shown here count as earned even if the caller hasn't stored them yet.
    val next = remember(badges) {
        val stored = store.earnedBadges
        val earned = stored + badges.filter { it.id !in stored }.associate { it.id to today }
        BadgeRules.closest(BadgeRules.evaluate(records, today, earned), 1).firstOrNull()
    }
    var index by rememberSaveable { mutableIntStateOf(0) }
    val current = badges[index.coerceIn(0, badges.lastIndex)]
    val shown = remember { MutableTransitionState(reduceMotion).apply { targetState = true } }

    val view = LocalView.current
    val announcement = stringResource(R.string.badges_sheet_announcement, stringResource(current.title))
    LaunchedEffect(current) { view.announceForAccessibility(announcement) }

    BackHandler(onBack = onDone)

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visibleState = shown, enter = fadeIn(tween(400))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colors.scrim(0.6f))
                    .pointerInput(Unit) {
                        // Swallow every touch so the Complete screen underneath stays still.
                        awaitPointerEventScope {
                            while (true) awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
            )
        }
        Box(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = 24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visibleState = shown,
                enter = fadeIn(tween(400)) + slideInVertically(tween(500)) { it / 4 }
            ) {
                Sheet(
                    badge = current,
                    records = records,
                    today = today,
                    next = next,
                    reduceMotion = reduceMotion,
                    onContinue = { if (index < badges.lastIndex) index++ else onDone() },
                    onViewAll = onViewAll
                )
            }
        }
    }
}

@Composable
private fun Sheet(
    badge: Badge,
    records: List<SessionRecord>,
    today: Long,
    next: BadgeProgress?,
    reduceMotion: Boolean,
    onContinue: () -> Unit,
    onViewAll: () -> Unit
) {
    val colors = BreatheTheme.colors
    val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(24.dp, shape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
            .clip(shape)
            .background(colors.sheet)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 28.dp, end = 28.dp, top = 10.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            Modifier
                .size(width = 40.dp, height = 5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.ink20)
        )
        Crossfade(
            targetState = badge,
            animationSpec = if (reduceMotion) snap() else tween(300),
            label = "badge"
        ) { shownBadge ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                MedalHalo(shownBadge)
                Words(shownBadge)
                ProofRow(proofFor(shownBadge, records, today))
            }
        }
        if (next != null) NextHint(next)
        val context = LocalContext.current
        val shareText = stringResource(
            R.string.share_badge_text,
            stringResource(badge.title),
            stringResource(badge.requirement).replaceFirstChar { it.lowercase() }
        )
        Actions(onContinue, onViewAll, onShare = { context.shareText(shareText) })
    }
}

@Composable
private fun MedalHalo(badge: Badge) {
    val colors = BreatheTheme.colors
    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(150.dp).background(colors.accentFaint, CircleShape))
        Box(Modifier.size(106.dp).background(colors.accentFaint, CircleShape))
        Medal(icon = badge.icon, earned = true, progress = 1f)
    }
}

@Composable
private fun Words(badge: Badge) {
    val colors = BreatheTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.badges_sheet_eyebrow),
            style = BreatheType.Eyebrow,
            color = colors.accent
        )
        Text(
            text = stringResource(badge.title),
            style = BreatheType.BadgeTitle,
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(badge.earnedMessage),
            style = BreatheType.LabelRegular.copy(lineHeight = 1.45.em),
            color = colors.ink80,
            textAlign = TextAlign.Center
        )
    }
}

private data class Proof(val value: String, val label: String)

/**
 * Three stats that show the practice behind [badge]. Window badges count the window that earned
 * them (the last 7, 14 or 60 days), falling back to all-time if that window no longer qualifies.
 */
@Composable
private fun proofFor(badge: Badge, records: List<SessionRecord>, today: Long): List<Proof> {
    val full = records.filter { it.isFull }
    val days = stringResource(R.string.badges_stat_days)
    val breathed = stringResource(R.string.badges_stat_breathed)
    val sessions = stringResource(R.string.badges_stat_full_sessions)

    fun dayCount(list: List<SessionRecord>) = list.map { it.day }.distinct().size

    return when (badge) {
        Badge.SteadyStart, Badge.SevenDays, Badge.Mountain -> {
            val window = BadgeRules.windowDays(badge)?.let { BadgeRules.inLastDays(full, today, it) }.orEmpty()
            val target = BadgeRules.target(badge)
            val qualifies = if (badge == Badge.SteadyStart) window.size >= target else dayCount(window) >= target
            val shown = if (qualifies) window else full
            listOf(
                Proof(dayCount(shown).toString(), days),
                Proof(minutesValue(shown), breathed),
                Proof(shown.size.toString(), sessions)
            )
        }
        Badge.Unbroken -> listOf(
            Proof(stringResource(R.string.badges_minutes_value, BadgeRules.UNBROKEN_MINUTES), stringResource(R.string.badges_stat_length)),
            Proof("0", stringResource(R.string.badges_stat_pauses)),
            Proof(full.size.toString(), sessions)
        )
        Badge.NightTide -> {
            val night = full.filter { it.patternId == PresetPattern.Sleep.id && BadgeRules.isNight(it) }
            listOf(
                Proof(night.size.toString(), stringResource(R.string.badges_stat_night_sessions)),
                Proof(minutesValue(night), breathed),
                Proof(dayCount(night).toString(), days)
            )
        }
        Badge.FourWinds -> listOf(
            Proof(PresetPattern.entries.size.toString(), stringResource(R.string.badges_stat_patterns)),
            Proof(full.size.toString(), sessions),
            Proof(minutesValue(full), breathed)
        )
        Badge.DeepWater -> listOf(
            Proof(minutesValue(full), breathed),
            Proof(full.size.toString(), sessions),
            Proof(dayCount(full).toString(), days)
        )
        Badge.SquareMind -> {
            val box = full.filter { it.patternId == PresetPattern.Box.id }
            listOf(
                Proof(box.size.toString(), stringResource(R.string.badges_stat_box_sessions)),
                Proof(minutesValue(box), breathed),
                Proof(dayCount(box).toString(), days)
            )
        }
    }
}

@Composable
private fun minutesValue(records: List<SessionRecord>): String =
    stringResource(R.string.badges_minutes_value, BadgeRules.fullMinutes(records))

@Composable
private fun ProofRow(stats: List<Proof>) {
    val line = BreatheTheme.colors.ink12
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(line, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), stroke)
                drawLine(line, Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
            }
            .padding(vertical = 14.dp)
    ) {
        stats.forEachIndexed { i, stat ->
            ProofCell(stat, divider = i > 0, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ProofCell(stat: Proof, divider: Boolean, modifier: Modifier = Modifier) {
    val colors = BreatheTheme.colors
    val line = colors.ink12
    val description = stringResource(R.string.badges_stat_description, stat.value, stat.label)
    Column(
        modifier = modifier
            .drawBehind {
                if (divider) {
                    val stroke = 1.dp.toPx()
                    drawLine(line, Offset(stroke / 2, 0f), Offset(stroke / 2, size.height), stroke)
                }
            }
            .clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(stat.value, style = BreatheType.StatSmall, color = colors.ink, textAlign = TextAlign.Center)
        Text(stat.label, style = BreatheType.Caption, color = colors.ink70, textAlign = TextAlign.Center)
    }
}

@Composable
private fun NextHint(next: BadgeProgress) {
    val colors = BreatheTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(next.badge.icon, null, Modifier.size(16.dp), tint = colors.accent)
        Text(
            text = stringResource(R.string.badges_sheet_next, stringResource(next.badge.title), badgeRemainingText(next)),
            style = BreatheType.Small,
            color = colors.ink70
        )
    }
}

@Composable
private fun Actions(onContinue: () -> Unit, onViewAll: () -> Unit, onShare: () -> Unit) {
    val colors = BreatheTheme.colors
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PrimaryButton(
            text = stringResource(R.string.badges_sheet_continue),
            onClick = onContinue,
            showArrow = false
        )
        Row(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button, onClick = onViewAll),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.badges_sheet_view_all), style = BreatheType.Label, color = colors.ink80)
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.share_badge_action), onClick = onShare),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(BreatheIcons.Share, null, Modifier.size(16.dp), tint = colors.ink80)
                Text(stringResource(R.string.share_short), style = BreatheType.Label, color = colors.ink80)
            }
        }
    }
}
