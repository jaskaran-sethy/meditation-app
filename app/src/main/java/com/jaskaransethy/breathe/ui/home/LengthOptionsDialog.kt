package com.jaskaransethy.breathe.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaskaransethy.breathe.R
import com.jaskaransethy.breathe.data.SessionLengths
import com.jaskaransethy.breathe.ui.icons.BreatheIcons
import com.jaskaransethy.breathe.ui.theme.BreatheTheme
import com.jaskaransethy.breathe.ui.theme.BreatheType

/** "2 · 5 · 10 min": the lengths as one value, for the Settings row. */
@Composable
internal fun lengthsSummary(lengths: List<Int>): String = stringResource(
    R.string.settings_session_lengths_value,
    lengths.joinToString(stringResource(R.string.settings_session_lengths_separator))
)

/**
 * Edits the lengths on Home's length bar: one − / + stepper per slot. Each stepper skips the
 * lengths the other slots hold, so they always stay different. Saved shortest first.
 */
@Composable
internal fun LengthOptionsDialog(
    lengths: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit
) {
    val colors = BreatheTheme.colors
    // Not re-sorted while editing, so a row never jumps out from under the finger.
    var draft by rememberSaveable { mutableStateOf(lengths) }
    SettingsDialogFrame(
        title = stringResource(R.string.settings_session_lengths),
        onDismiss = onDismiss,
        onConfirm = { onConfirm(draft.sorted()) }
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(
                stringResource(R.string.settings_session_lengths_description),
                style = BreatheType.Caption,
                color = colors.ink70,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            draft.forEachIndexed { index, minutes ->
                val others = draft.filterIndexed { i, _ -> i != index }
                LengthStepper(
                    minutes = minutes,
                    shorter = SessionLengths.step(minutes, longer = false, taken = others),
                    longer = SessionLengths.step(minutes, longer = true, taken = others),
                    onChange = { value -> draft = draft.toMutableList().also { it[index] = value } }
                )
            }
            TextButton(
                onClick = { draft = SessionLengths.Default },
                enabled = draft.sorted() != SessionLengths.Default,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colors.accent,
                    disabledContentColor = colors.ink30
                ),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp)
            ) {
                Text(stringResource(R.string.settings_session_lengths_reset), style = BreatheType.Label)
            }
        }
    }
}

/** − 5 min +, with the value announced as it changes. A null step disables that button. */
@Composable
private fun LengthStepper(minutes: Int, shorter: Int?, longer: Int?, onChange: (Int) -> Unit) {
    val colors = BreatheTheme.colors
    val value = stringResource(R.string.minutes_short, minutes)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepButton(
            icon = BreatheIcons.Minus,
            description = stringResource(R.string.settings_session_lengths_shorter, value),
            target = shorter,
            onChange = onChange
        )
        Text(
            value,
            style = BreatheType.LabelStrong,
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .semantics { liveRegion = LiveRegionMode.Polite }
        )
        StepButton(
            icon = BreatheIcons.Plus,
            description = stringResource(R.string.settings_session_lengths_longer, value),
            target = longer,
            onChange = onChange
        )
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, target: Int?, onChange: (Int) -> Unit) {
    val colors = BreatheTheme.colors
    val enabled = target != null
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(colors.ink8)
            .clickable(enabled = enabled, role = Role.Button) { target?.let(onChange) }
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, Modifier.size(20.dp), tint = colors.ink)
    }
}
