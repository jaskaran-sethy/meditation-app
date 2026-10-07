package com.example.myapplication.ui.complete

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.data.Mood
import com.example.myapplication.ui.components.MoodChip
import com.example.myapplication.ui.components.PhotoBackground
import com.example.myapplication.ui.components.PrimaryButton
import com.example.myapplication.ui.icons.BreatheIcons
import com.example.myapplication.ui.theme.BreatheTheme
import com.example.myapplication.ui.theme.BreatheType

@Composable
fun CompleteScreen(
    minutes: Int,
    breaths: Int,
    streakDays: Int,
    onDone: (Mood?) -> Unit,
    onBreatheAgain: (Mood?) -> Unit
) {
    var mood by rememberSaveable { mutableStateOf<Mood?>(null) }
    val colors = BreatheTheme.colors
    // Light at the top, clear through the middle, near-solid behind the summary at the bottom.
    val scrim = Brush.verticalGradient(
        0.00f to colors.scrim(0.60f),
        0.22f to colors.scrim(0f),
        0.45f to colors.scrim(0.70f),
        0.68f to colors.scrim(0.96f),
        1.00f to colors.bgBottom
    )

    PhotoBackground(overlay = scrim) {
        // Pinned to the bottom, but scrolls on short screens or with large text instead of clipping.
        BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.Bottom)
            ) {
                Headline(minutes)
                Stats(minutes, breaths, streakDays)
                MoodCheck(selected = mood, onSelect = { mood = if (mood == it) null else it })
                Actions(onDone = { onDone(mood) }, onBreatheAgain = { onBreatheAgain(mood) })
            }
        }
    }
}

@Composable
private fun Headline(minutes: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .border(1.5.dp, BreatheTheme.colors.ink70, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(BreatheIcons.Check, null, Modifier.size(24.dp), tint = BreatheTheme.colors.ink)
        }
        Text(
            text = stringResource(R.string.complete_title),
            style = BreatheType.Title,
            color = BreatheTheme.colors.ink,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(R.string.complete_subtitle, minutesInWords(minutes)),
            style = BreatheType.Body,
            color = BreatheTheme.colors.ink80,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun minutesInWords(minutes: Int): String = when (minutes) {
    2 -> stringResource(R.string.number_two)
    5 -> stringResource(R.string.number_five)
    10 -> stringResource(R.string.number_ten)
    else -> minutes.toString()
}

@Composable
private fun Stats(minutes: Int, breaths: Int, streakDays: Int) {
    val divider = BreatheTheme.colors.ink15
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(divider, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), stroke)
                drawLine(
                    divider,
                    Offset(0f, size.height - stroke / 2),
                    Offset(size.width, size.height - stroke / 2),
                    stroke
                )
            }
            .padding(vertical = 18.dp)
    ) {
        Stat(
            value = stringResource(R.string.minutes_short, minutes),
            label = stringResource(R.string.stat_breathed),
            divider = false,
            modifier = Modifier.weight(1f)
        )
        Stat(
            value = breaths.toString(),
            label = stringResource(R.string.stat_breaths),
            divider = true,
            modifier = Modifier.weight(1f)
        )
        Stat(
            value = pluralStringResource(R.plurals.days, streakDays, streakDays),
            label = stringResource(R.string.stat_streak),
            divider = true,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun Stat(value: String, label: String, divider: Boolean, modifier: Modifier = Modifier) {
    val colors = BreatheTheme.colors
    val dividerColor = colors.ink15
    Column(
        modifier = modifier
            .then(
                if (divider) {
                    Modifier.drawBehind {
                        val stroke = 1.dp.toPx()
                        drawLine(
                            dividerColor,
                            Offset(stroke / 2, 0f),
                            Offset(stroke / 2, size.height),
                            stroke
                        )
                    }
                } else {
                    Modifier
                }
            )
            // Read value and label together, e.g. "5 min, Breathed".
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, style = BreatheType.Stat, color = colors.ink, textAlign = TextAlign.Center)
        Text(label, style = BreatheType.Caption, color = colors.ink70, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MoodCheck(selected: Mood?, onSelect: (Mood) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.mood_question),
            style = BreatheType.LabelStrong,
            color = BreatheTheme.colors.ink
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Mood.entries.forEach { option ->
                MoodChip(
                    text = stringResource(option.label),
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private val Mood.label: Int
    get() = when (this) {
        Mood.Calmer -> R.string.mood_calmer
        Mood.Same -> R.string.mood_same
        Mood.StillTense -> R.string.mood_still_tense
    }

@Composable
private fun Actions(onDone: () -> Unit, onBreatheAgain: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PrimaryButton(text = stringResource(R.string.done), onClick = onDone, showArrow = false)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable(role = Role.Button, onClick = onBreatheAgain),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(BreatheIcons.RotateCcw, null, Modifier.size(16.dp), tint = BreatheTheme.colors.ink80)
            Text(
                stringResource(R.string.breathe_again),
                style = BreatheType.Label,
                color = BreatheTheme.colors.ink80
            )
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun CompleteScreenPreview() {
    BreatheTheme {
        CompleteScreen(minutes = 5, breaths = 32, streakDays = 4, onDone = {}, onBreatheAgain = {})
    }
}
