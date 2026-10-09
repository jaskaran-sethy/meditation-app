package com.jaskaransethy.breathe.ui.patterns

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaskaransethy.breathe.R
import com.jaskaransethy.breathe.data.CustomPattern
import com.jaskaransethy.breathe.data.Phase
import com.jaskaransethy.breathe.data.PhaseStep
import com.jaskaransethy.breathe.ui.components.IconCircleButton
import com.jaskaransethy.breathe.ui.components.PrimaryButton
import com.jaskaransethy.breathe.ui.components.Rhythm
import com.jaskaransethy.breathe.ui.components.fadingBottomEdge
import com.jaskaransethy.breathe.ui.icons.BreatheIcons
import com.jaskaransethy.breathe.ui.theme.BreatheTheme
import com.jaskaransethy.breathe.ui.theme.screenBackground
import com.jaskaransethy.breathe.ui.theme.BreatheType
import kotlin.math.roundToInt

// A new pattern starts as a gentle rhythm with a longer exhale, so Save works straight away.
private const val NewInhale = 4
private const val NewHoldIn = 2
private const val NewExhale = 6
private const val NewHoldOut = 0

/**
 * Builds a new pattern, or edits [existing]: a name and four counts, previewed as the same
 * phase strip the session shows. Saving hands back a clamped, ready-to-play [CustomPattern].
 */
@Composable
fun PatternEditorScreen(
    existing: CustomPattern?,
    onSave: (CustomPattern) -> Unit,
    onDelete: (CustomPattern) -> Unit,
    onBack: () -> Unit
) {
    val colors = BreatheTheme.colors
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var inhale by rememberSaveable { mutableIntStateOf(existing?.inhale ?: NewInhale) }
    var holdIn by rememberSaveable { mutableIntStateOf(existing?.holdIn ?: NewHoldIn) }
    var exhale by rememberSaveable { mutableIntStateOf(existing?.exhale ?: NewExhale) }
    var holdOut by rememberSaveable { mutableIntStateOf(existing?.holdOut ?: NewHoldOut) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val isNew = existing == null
    val defaultName = stringResource(R.string.pattern_editor_default_name)
    val focusManager = LocalFocusManager.current

    // Ids are only for storage; an unnamed pattern takes the placeholder name.
    val draft = CustomPattern.of(
        id = existing?.id ?: "",
        name = name.ifBlank { defaultName },
        inhale = inhale,
        holdIn = holdIn,
        exhale = exhale,
        holdOut = holdOut
    )

    BackHandler(onBack = onBack)

    Box(Modifier.fillMaxSize().screenBackground(colors)) {
        // Save stays pinned to the bottom (above the keyboard); everything else scrolls.
        val scroll = rememberScrollState()
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .fadingBottomEdge(scroll)
                    .verticalScroll(scroll)
                    .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                TopBar(
                    onBack = onBack,
                    onDelete = if (existing != null) ({ confirmDelete = true }) else null
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(if (isNew) R.string.pattern_editor_new_title else R.string.pattern_editor_edit_title),
                        style = BreatheType.Heading,
                        color = colors.ink,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(stringResource(R.string.pattern_editor_intro), style = BreatheType.Small, color = colors.ink70)
                }

                Section(stringResource(R.string.pattern_editor_name)) {
                    NameField(
                        value = name,
                        placeholder = defaultName,
                        onValueChange = { name = it.take(CustomPattern.MAX_NAME_LENGTH) },
                        onDone = { focusManager.clearFocus() }
                    )
                }

                Section(stringResource(R.string.pattern_editor_rhythm)) {
                    Group {
                        StepperRow(
                            label = stringResource(R.string.pattern_editor_inhale),
                            spoken = stringResource(R.string.pattern_editor_inhale),
                            value = inhale,
                            range = CustomPattern.BreathSeconds,
                            onChange = { inhale = it }
                        )
                        RowDivider()
                        StepperRow(
                            label = stringResource(R.string.pattern_editor_hold),
                            hint = stringResource(R.string.pattern_editor_hold_in_hint),
                            spoken = stringResource(R.string.pattern_editor_hold_in_spoken),
                            value = holdIn,
                            range = CustomPattern.HoldSeconds,
                            onChange = { holdIn = it }
                        )
                        RowDivider()
                        StepperRow(
                            label = stringResource(R.string.pattern_editor_exhale),
                            spoken = stringResource(R.string.pattern_editor_exhale),
                            value = exhale,
                            range = CustomPattern.BreathSeconds,
                            onChange = { exhale = it }
                        )
                        RowDivider()
                        StepperRow(
                            label = stringResource(R.string.pattern_editor_hold),
                            hint = stringResource(R.string.pattern_editor_hold_out_hint),
                            spoken = stringResource(R.string.pattern_editor_hold_out_spoken),
                            value = holdOut,
                            range = CustomPattern.HoldSeconds,
                            onChange = { holdOut = it }
                        )
                    }
                }

                Preview(draft)
            }
            PrimaryButton(
                text = stringResource(R.string.pattern_editor_save),
                showArrow = false,
                onClick = { onSave(draft.copy(id = existing?.id ?: CustomPattern.newId())) },
                modifier = Modifier.padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 24.dp)
            )
        }
    }

    if (confirmDelete && existing != null) {
        DeleteDialog(
            name = existing.name,
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                onDelete(existing)
            }
        )
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, onDelete: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircleButton(
            icon = BreatheIcons.ChevronLeft,
            contentDescription = stringResource(R.string.pattern_editor_back),
            onClick = onBack,
            iconSize = 22.dp
        )
        Box(Modifier.weight(1f))
        if (onDelete != null) {
            IconCircleButton(
                icon = BreatheIcons.Trash,
                contentDescription = stringResource(R.string.pattern_editor_delete),
                onClick = onDelete,
                iconSize = 18.dp
            )
        }
    }
}

/** Uppercase section label over its content, read in sentence case by screen readers. */
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title.uppercase(),
            style = BreatheType.SectionLabel,
            color = BreatheTheme.colors.ink60,
            modifier = Modifier.semantics {
                heading()
                contentDescription = title
            }
        )
        content()
    }
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    val colors = BreatheTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.ink12, shape)
    ) { content() }
}

@Composable
private fun RowDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(BreatheTheme.colors.ink8))
}

@Composable
private fun NameField(value: String, placeholder: String, onValueChange: (String) -> Unit, onDone: () -> Unit) {
    val colors = BreatheTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val label = stringResource(R.string.pattern_editor_name)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = BreatheType.Body.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.ink),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = label },
        decorationBox = { field ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(shape)
                    .background(colors.surface)
                    .border(1.dp, colors.ink12, shape)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, style = BreatheType.Body, color = colors.ink40)
                }
                field()
            }
        }
    )
}

/**
 * One count with − and + buttons. The buttons are labelled with the full phase name
 * ([spoken]), and the value is announced as it changes.
 */
@Composable
private fun StepperRow(
    label: String,
    spoken: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    hint: String? = null
) {
    val colors = BreatheTheme.colors
    val off = value == 0
    val shown = if (off) stringResource(R.string.pattern_editor_off) else stringResource(R.string.pattern_editor_seconds, value)
    val announced = if (off) shown else pluralStringResource(R.plurals.pattern_editor_seconds_spoken, value, value)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = BreatheType.Label, color = colors.ink)
            if (hint != null) Text(hint, style = BreatheType.Caption, color = colors.ink60)
        }
        StepButton(
            icon = BreatheIcons.Minus,
            contentDescription = stringResource(R.string.pattern_editor_shorter, spoken),
            enabled = value > range.first,
            onClick = { onChange(value - 1) }
        )
        Text(
            shown,
            style = if (off) BreatheType.LabelRegular else BreatheType.Count,
            color = if (off) colors.ink60 else colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 52.dp)
                .semantics {
                    contentDescription = "$spoken, $announced"
                    liveRegion = LiveRegionMode.Polite
                }
        )
        StepButton(
            icon = BreatheIcons.Plus,
            contentDescription = stringResource(R.string.pattern_editor_longer, spoken),
            enabled = value < range.last,
            onClick = { onChange(value + 1) }
        )
    }
}

@Composable
private fun StepButton(icon: ImageVector, contentDescription: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = BreatheTheme.colors
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (enabled) colors.surfaceRaised else colors.surface)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = contentDescription, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = if (enabled) colors.ink else colors.ink30)
    }
}

/** What one breath will look like in the session: the rhythm, its phases, and its pace. */
@Composable
private fun Preview(pattern: CustomPattern) {
    val colors = BreatheTheme.colors
    val perMinute = (60f / pattern.cycleSeconds).roundToInt().coerceAtLeast(1)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Rhythm(pattern.counts, BreatheType.Stat, colors.ink, gap = 10.dp)
        PhaseStrip(pattern.steps)
        Text(
            pluralStringResource(R.plurals.pattern_editor_summary, perMinute, pattern.cycleSeconds, perMinute),
            style = BreatheType.Small,
            color = colors.ink70,
            textAlign = TextAlign.Center
        )
    }
}

/** The session's phase strip, every phase shown as upcoming. */
@Composable
private fun PhaseStrip(steps: List<PhaseStep>) {
    val colors = BreatheTheme.colors
    Row(
        modifier = Modifier.clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        steps.forEach { step ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .width(40.dp)
                        .height(3.dp)
                        .background(colors.ink25, RoundedCornerShape(2.dp))
                )
                Text(
                    stringResource(
                        when (step.phase) {
                            Phase.Inhale -> R.string.phase_short_in
                            Phase.HoldIn, Phase.HoldOut -> R.string.phase_short_hold
                            Phase.Exhale -> R.string.phase_short_out
                        },
                        step.seconds
                    ),
                    style = BreatheType.SmallMedium,
                    color = colors.ink60
                )
            }
        }
    }
}

@Composable
private fun DeleteDialog(name: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val colors = BreatheTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) {
                Text(stringResource(R.string.pattern_editor_delete_confirm), style = BreatheType.LabelStrong)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = colors.ink70)) {
                Text(stringResource(R.string.pattern_editor_cancel), style = BreatheType.LabelStrong)
            }
        },
        title = {
            Text(
                stringResource(R.string.pattern_editor_delete_title, name),
                style = BreatheType.Tagline.copy(fontWeight = FontWeight.SemiBold),
                color = colors.ink,
                modifier = Modifier.semantics { heading() }
            )
        },
        text = { Text(stringResource(R.string.pattern_editor_delete_text), style = BreatheType.Body, color = colors.ink80) },
        shape = RoundedCornerShape(24.dp),
        containerColor = colors.sheet,
        titleContentColor = colors.ink,
        textContentColor = colors.ink,
        tonalElevation = 0.dp
    )
}
