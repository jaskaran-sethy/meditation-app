package com.example.myapplication.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.data.BreathPattern
import com.example.myapplication.data.BreatheStore
import com.example.myapplication.data.CustomPattern
import com.example.myapplication.data.PresetPattern
import com.example.myapplication.data.PracticeSummary
import com.example.myapplication.data.SessionLengths
import com.example.myapplication.ui.components.CreatePatternCard
import com.example.myapplication.ui.components.LengthControl
import com.example.myapplication.ui.components.PatternCard
import com.example.myapplication.ui.components.PrimaryButton
import com.example.myapplication.ui.components.displayDescription
import com.example.myapplication.ui.components.displayName
import com.example.myapplication.ui.components.fadingBottomEdge
import com.example.myapplication.ui.icons.BreatheIcons
import com.example.myapplication.ui.theme.BreatheTheme
import com.example.myapplication.ui.theme.BreatheType
import java.util.Calendar

@Composable
fun HomeTab(
    store: BreatheStore,
    onBegin: (BreathPattern, Int) -> Unit,
    /** Opens the pattern builder: null for a new pattern, or a custom pattern's id to edit it. */
    onEditPattern: (String?) -> Unit
) {
    // The last choice is pre-selected so returning users can tap Begin straight away.
    var pattern by remember { mutableStateOf(store.lastPattern) }
    var minutes by remember { mutableStateOf(store.lastMinutes) }
    val customPatterns = remember { store.customPatterns }
    val summary = remember { store.summary() }
    val patternTitle = pattern.displayName()

    // The controls stay pinned to the bottom so Begin is always in reach; the patterns above
    // scroll once the user's own patterns (or large text) no longer fit.
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .fadingBottomEdge(scroll)
                .verticalScroll(scroll)
                .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(greeting()), style = BreatheType.Small, color = BreatheTheme.colors.ink70)
                Text(
                    stringResource(R.string.home_title),
                    style = BreatheType.Heading,
                    color = BreatheTheme.colors.ink,
                    modifier = Modifier.semantics { heading() }
                )
            }
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                (PresetPattern.entries + customPatterns).forEach { option ->
                    val title = option.displayName()
                    PatternCard(
                        title = title,
                        description = option.displayDescription(),
                        rhythm = option.counts,
                        selected = option == pattern,
                        onClick = {
                            pattern = option
                            store.lastPattern = option
                        },
                        onEdit = if (option is CustomPattern) ({ onEditPattern(option.id) }) else null,
                        editLabel = stringResource(R.string.pattern_edit, title)
                    )
                }
                CreatePatternCard(
                    title = stringResource(R.string.pattern_create),
                    description = stringResource(R.string.pattern_create_description),
                    onClick = { onEditPattern(null) }
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StreakLine(summary)
            LengthControl(
                options = SessionLengths,
                selected = minutes,
                label = { stringResource(R.string.minutes_short, it) },
                onSelect = {
                    minutes = it
                    store.lastMinutes = it
                }
            )
            PrimaryButton(
                text = stringResource(R.string.home_begin, patternTitle, minutes),
                onClick = { onBegin(pattern, minutes) }
            )
        }
    }
}

@Composable
private fun StreakLine(summary: PracticeSummary) {
    val text = when {
        summary.streakDays > 0 -> pluralStringResource(
            R.plurals.streak_and_week, summary.streakDays, summary.streakDays, summary.minutesThisWeek
        )
        summary.minutesThisWeek > 0 -> stringResource(R.string.week_minutes, summary.minutesThisWeek)
        else -> stringResource(R.string.streak_none)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(BreatheIcons.Leaf, null, Modifier.size(16.dp), tint = BreatheTheme.colors.accent)
        Text(text, style = BreatheType.Small, color = BreatheTheme.colors.ink80)
    }
}

private fun greeting(): Int = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> R.string.greeting_morning
    in 12..17 -> R.string.greeting_afternoon
    else -> R.string.greeting_evening
}
