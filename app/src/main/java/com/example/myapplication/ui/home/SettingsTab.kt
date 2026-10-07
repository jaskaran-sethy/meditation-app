package com.example.myapplication.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import com.example.myapplication.R
import com.example.myapplication.data.BreatheStore
import com.example.myapplication.data.GuideStyle
import com.example.myapplication.data.ReduceMotion
import com.example.myapplication.data.ReminderSettings
import com.example.myapplication.data.SessionLengths
import com.example.myapplication.data.shouldReduceMotion
import com.example.myapplication.data.systemReducesMotion
import com.example.myapplication.reminder.ReminderScheduler
import com.example.myapplication.ui.icons.BreatheIcons
import com.example.myapplication.ui.session.guides.description
import com.example.myapplication.ui.session.guides.label
import com.example.myapplication.ui.theme.BreatheTheme
import com.example.myapplication.ui.theme.BreatheType
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

private enum class SettingsDialog { Length, Guide, Time, Days, ReduceMotion }

private val DialogTitleStyle = BreatheType.Tagline.copy(fontWeight = FontWeight.SemiBold)

/** The switch knob is white in both themes, as in the design. */
private val KnobColor = Color.White

/** Time and Days stay editable while the reminder is off, just quieter. */
private const val DimmedAlpha = 0.5f
private const val ToggleMillis = 180

private val EveryDay = (1..7).toSet()
private val Weekdays = (1..5).toSet()
private val Weekends = setOf(6, 7)

/**
 * Short on purpose: only the choices that change how a session feels, when you're reminded,
 * and accessibility. Every change writes straight through to [BreatheStore].
 */
@Composable
fun SettingsTab(store: BreatheStore) {
    val context = LocalContext.current
    val locale = currentLocale()

    var sound by remember { mutableStateOf(store.soundEnabled) }
    var haptics by remember { mutableStateOf(store.hapticsEnabled) }
    var minutes by remember { mutableStateOf(store.lastMinutes) }
    var guide by remember { mutableStateOf(store.guideStyle) }
    var reminder by remember { mutableStateOf(store.reminder) }
    var reduceMotion by remember { mutableStateOf(store.reduceMotion) }
    var keepAwake by remember { mutableStateOf(store.keepScreenAwake) }
    var notificationsDenied by rememberSaveable { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }

    // Re-read when the override changes; the OS setting itself is read when the tab is shown.
    val animateToggles = remember(reduceMotion) { !shouldReduceMotion(context, store) }

    fun updateReminder(value: ReminderSettings) {
        reminder = value
        store.reminder = value
        ReminderScheduler.reschedule(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsDenied = !granted
        if (granted) updateReminder(reminder.copy(enabled = true))
    }

    fun setReminderEnabled(enabled: Boolean) {
        when {
            !enabled -> updateReminder(reminder.copy(enabled = false))
            needsNotificationPermission(context) ->
                // Only switch on once the person has allowed notifications.
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> {
                notificationsDenied = false
                updateReminder(reminder.copy(enabled = true))
            }
        }
    }

    val version = remember { appVersion(context) }

    SettingsContent(
        values = SettingsValues(
            sound = sound,
            haptics = haptics,
            minutes = minutes,
            guide = guide,
            reminder = reminder,
            reminderTime = remember(reminder.hour, reminder.minute) {
                formatTime(context, reminder.hour, reminder.minute)
            },
            reduceMotion = reduceMotion,
            keepAwake = keepAwake,
            notificationsDenied = notificationsDenied,
            version = version
        ),
        locale = locale,
        animateToggles = animateToggles,
        onSound = { sound = it; store.soundEnabled = it },
        onHaptics = { haptics = it; store.hapticsEnabled = it },
        onLength = { dialog = SettingsDialog.Length },
        onGuide = { dialog = SettingsDialog.Guide },
        onReminder = { setReminderEnabled(it) },
        onTime = { dialog = SettingsDialog.Time },
        onDays = { dialog = SettingsDialog.Days },
        onReduceMotion = { dialog = SettingsDialog.ReduceMotion },
        onKeepAwake = { keepAwake = it; store.keepScreenAwake = it }
    )

    val dismiss = { dialog = null }
    when (dialog) {
        SettingsDialog.Length -> ChoiceDialog(
            title = stringResource(R.string.settings_default_length),
            options = SessionLengths,
            selected = minutes,
            label = { stringResource(R.string.minutes_short, it) },
            onDismiss = dismiss,
            onConfirm = {
                minutes = it
                store.lastMinutes = it
                dialog = null
            }
        )
        SettingsDialog.Guide -> ChoiceDialog(
            title = stringResource(R.string.settings_breathing_visual),
            options = GuideStyle.entries,
            selected = guide,
            label = { stringResource(it.label) },
            optionDescription = { stringResource(it.description) },
            onDismiss = dismiss,
            onConfirm = {
                guide = it
                store.guideStyle = it
                dialog = null
            }
        )
        SettingsDialog.Time -> TimeDialog(
            hour = reminder.hour,
            minute = reminder.minute,
            is24Hour = DateFormat.is24HourFormat(context),
            onDismiss = dismiss,
            onConfirm = { hour, minute ->
                updateReminder(reminder.copy(hour = hour, minute = minute))
                dialog = null
            }
        )
        SettingsDialog.Days -> DaysDialog(
            days = reminder.days,
            locale = locale,
            onDismiss = dismiss,
            onConfirm = {
                updateReminder(reminder.copy(days = it))
                dialog = null
            }
        )
        SettingsDialog.ReduceMotion -> {
            val systemOn = remember { systemReducesMotion(context) }
            ChoiceDialog(
                title = stringResource(R.string.settings_reduce_motion),
                description = stringResource(R.string.settings_reduce_motion_description),
                options = ReduceMotion.entries,
                selected = reduceMotion,
                label = { reduceMotionOptionLabel(it, systemOn) },
                onDismiss = dismiss,
                onConfirm = {
                    reduceMotion = it
                    store.reduceMotion = it
                    dialog = null
                }
            )
        }
        null -> Unit
    }
}

private data class SettingsValues(
    val sound: Boolean,
    val haptics: Boolean,
    val minutes: Int,
    val guide: GuideStyle,
    val reminder: ReminderSettings,
    val reminderTime: String,
    val reduceMotion: ReduceMotion,
    val keepAwake: Boolean,
    val notificationsDenied: Boolean,
    val version: String?
)

@Composable
private fun SettingsContent(
    values: SettingsValues,
    locale: Locale,
    animateToggles: Boolean,
    onSound: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onLength: () -> Unit,
    onGuide: () -> Unit,
    onReminder: (Boolean) -> Unit,
    onTime: () -> Unit,
    onDays: () -> Unit,
    onReduceMotion: () -> Unit,
    onKeepAwake: (Boolean) -> Unit
) {
    val colors = BreatheTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Text(
            stringResource(R.string.tab_settings),
            style = BreatheType.Heading,
            color = colors.ink,
            modifier = Modifier.semantics { heading() }
        )

        SettingsGroup(stringResource(R.string.settings_section_session), locale) {
            ToggleRow(
                icon = BreatheIcons.Volume,
                label = stringResource(R.string.settings_sound),
                checked = values.sound,
                animate = animateToggles,
                onCheckedChange = onSound
            )
            RowDivider()
            ToggleRow(
                icon = BreatheIcons.Vibrate,
                label = stringResource(R.string.settings_haptics),
                checked = values.haptics,
                animate = animateToggles,
                onCheckedChange = onHaptics
            )
            RowDivider()
            ValueRow(
                icon = BreatheIcons.Timer,
                label = stringResource(R.string.settings_default_length),
                value = stringResource(R.string.minutes_short, values.minutes),
                numeric = true,
                onClick = onLength
            )
            RowDivider()
            ValueRow(
                icon = BreatheIcons.Wind,
                label = stringResource(R.string.settings_breathing_visual),
                value = stringResource(values.guide.label),
                onClick = onGuide
            )
        }

        SettingsGroup(
            title = stringResource(R.string.settings_section_reminders),
            locale = locale,
            footnote = if (values.notificationsDenied) {
                stringResource(R.string.settings_notifications_denied)
            } else {
                null
            }
        ) {
            val dimmed = Modifier.alpha(if (values.reminder.enabled) 1f else DimmedAlpha)
            ToggleRow(
                icon = BreatheIcons.Bell,
                label = stringResource(R.string.settings_daily_reminder),
                checked = values.reminder.enabled,
                animate = animateToggles,
                onCheckedChange = onReminder
            )
            RowDivider()
            ValueRow(
                icon = BreatheIcons.Clock,
                label = stringResource(R.string.settings_reminder_time),
                value = values.reminderTime,
                numeric = true,
                onClick = onTime,
                modifier = dimmed
            )
            RowDivider()
            ValueRow(
                icon = BreatheIcons.Calendar,
                label = stringResource(R.string.settings_reminder_days),
                value = daysSummary(values.reminder.days, locale),
                onClick = onDays,
                modifier = dimmed
            )
        }

        SettingsGroup(stringResource(R.string.settings_section_accessibility), locale) {
            ValueRow(
                icon = BreatheIcons.EyeOff,
                label = stringResource(R.string.settings_reduce_motion),
                value = reduceMotionValue(values.reduceMotion),
                onClick = onReduceMotion
            )
            RowDivider()
            ToggleRow(
                icon = BreatheIcons.Sun,
                label = stringResource(R.string.settings_keep_screen_awake),
                checked = values.keepAwake,
                animate = animateToggles,
                onCheckedChange = onKeepAwake
            )
        }

        // The design also shows "Help & feedback · Privacy" here; those links are pending URLs,
        // so only the version is shown for now.
        if (values.version != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.settings_version, values.version),
                    style = BreatheType.Small,
                    color = colors.ink60
                )
            }
        }
    }
}

/** Uppercase section label over a rounded container of rows, with an optional note below. */
@Composable
private fun SettingsGroup(
    title: String,
    locale: Locale,
    footnote: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = BreatheTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title.uppercase(locale),
            style = BreatheType.SectionLabel,
            color = colors.ink60,
            // Read the label in sentence case so screen readers don't spell it out.
            modifier = Modifier.semantics {
                heading()
                contentDescription = title
            }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.ink12, shape),
            content = content
        )
        if (footnote != null) {
            Text(
                footnote,
                style = BreatheType.Caption,
                color = colors.ink70,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}

@Composable
private fun RowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(BreatheTheme.colors.ink8)
    )
}

/** Icon and label; the whole row is the touch target and grows with large text. */
@Composable
private fun SettingRow(
    icon: ImageVector,
    label: String,
    modifier: Modifier,
    trailing: @Composable () -> Unit
) {
    val colors = BreatheTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = colors.ink80)
        Text(label, style = BreatheType.Label, color = colors.ink, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    animate: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingRow(
        icon = icon,
        label = label,
        modifier = modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
    ) {
        BreatheSwitch(checked = checked, animate = animate)
    }
}

/**
 * Reads as "Default length, 5 min": clickable merges the label and value. Numbers ("5 min",
 * "9:00 pm") use the medium weight and words ("Every day") the regular one, as in the design.
 */
@Composable
private fun ValueRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    numeric: Boolean = false
) {
    val colors = BreatheTheme.colors
    val mirror = LocalLayoutDirection.current == LayoutDirection.Rtl
    SettingRow(
        icon = icon,
        label = label,
        modifier = modifier.clickable(role = Role.Button, onClick = onClick)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                value,
                style = if (numeric) BreatheType.Label else BreatheType.LabelRegular,
                color = colors.ink70,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(max = 168.dp)
            )
            Icon(
                BreatheIcons.ChevronRight,
                null,
                Modifier
                    .size(16.dp)
                    .graphicsLayer { if (mirror) scaleX = -1f },
                tint = colors.ink50
            )
        }
    }
}

/** 46×28 track; accent when on — the one accent. Visual only: the row carries the semantics. */
@Composable
private fun BreatheSwitch(checked: Boolean, animate: Boolean) {
    val colors = BreatheTheme.colors
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 18.dp else 0.dp,
        animationSpec = if (animate) tween(ToggleMillis) else snap(),
        label = "knob"
    )
    val track by animateColorAsState(
        targetValue = if (checked) colors.accent else colors.ink20,
        animationSpec = if (animate) tween(ToggleMillis) else snap(),
        label = "track"
    )
    Box(
        Modifier
            .size(width = 46.dp, height = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(track)
            .padding(3.dp)
    ) {
        Box(
            Modifier
                .offset(x = knobOffset)
                .size(22.dp)
                .background(KnobColor, CircleShape)
        )
    }
}

// ---- Dialogs ----

@Composable
private fun SettingsDialogFrame(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = BreatheTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = confirmEnabled,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colors.accent,
                    disabledContentColor = colors.ink30
                )
            ) {
                Text(stringResource(R.string.settings_save), style = BreatheType.LabelStrong)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.ink70)
            ) {
                Text(stringResource(R.string.settings_cancel), style = BreatheType.LabelStrong)
            }
        },
        title = {
            Text(
                title,
                style = DialogTitleStyle,
                color = colors.ink,
                modifier = Modifier.semantics { heading() }
            )
        },
        text = content,
        shape = RoundedCornerShape(24.dp),
        containerColor = colors.sheet,
        titleContentColor = colors.ink,
        textContentColor = colors.ink,
        tonalElevation = 0.dp
    )
}

/**
 * Single choice with real radio buttons, so selection doesn't rely on colour alone.
 * [optionDescription] adds a one-line explanation under each option.
 */
@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onDismiss: () -> Unit,
    onConfirm: (T) -> Unit,
    description: String? = null,
    optionDescription: (@Composable (T) -> String)? = null
) {
    val colors = BreatheTheme.colors
    var choice by remember { mutableStateOf(selected) }
    SettingsDialogFrame(title = title, onDismiss = onDismiss, onConfirm = { onConfirm(choice) }) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            if (description != null) {
                Text(
                    description,
                    style = BreatheType.Caption,
                    color = colors.ink70,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Column(Modifier.selectableGroup()) {
                options.forEach { option ->
                    val isSelected = option == choice
                    OptionRow(
                        modifier = Modifier.selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { choice = option }
                        ),
                        text = label(option),
                        supportingText = optionDescription?.invoke(option)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.accent,
                                unselectedColor = colors.ink60
                            )
                        )
                    }
                }
            }
        }
    }
}

/** Monday to Sunday with checkboxes; at least one day must stay selected. */
@Composable
private fun DaysDialog(
    days: Set<Int>,
    locale: Locale,
    onDismiss: () -> Unit,
    onConfirm: (Set<Int>) -> Unit
) {
    val colors = BreatheTheme.colors
    var chosen by remember { mutableStateOf(days) }
    val names = remember(locale) { DateFormatSymbols.getInstance(locale).weekdays }
    SettingsDialogFrame(
        title = stringResource(R.string.settings_days_dialog_title),
        onDismiss = onDismiss,
        onConfirm = { onConfirm(chosen) },
        confirmEnabled = chosen.isNotEmpty()
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            (1..7).forEach { day ->
                val checked = day in chosen
                OptionRow(
                    modifier = Modifier.toggleable(
                        value = checked,
                        role = Role.Checkbox,
                        onValueChange = { chosen = if (it) chosen + day else chosen - day }
                    ),
                    text = names[calendarDay(day)]
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = colors.accent,
                            uncheckedColor = colors.ink60,
                            checkmarkColor = colors.onAccent
                        )
                    )
                }
            }
            if (chosen.isEmpty()) {
                Text(
                    stringResource(R.string.settings_days_none),
                    style = BreatheType.Caption,
                    color = colors.ink70,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    hour: Int,
    minute: Int,
    is24Hour: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val colors = BreatheTheme.colors
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = is24Hour)
    SettingsDialogFrame(
        title = stringResource(R.string.settings_time_dialog_title),
        onDismiss = onDismiss,
        onConfirm = { onConfirm(state.hour, state.minute) }
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = colors.ink8,
                    clockDialSelectedContentColor = colors.onAccent,
                    clockDialUnselectedContentColor = colors.ink,
                    selectorColor = colors.accent,
                    containerColor = colors.sheet,
                    periodSelectorBorderColor = colors.ink20,
                    periodSelectorSelectedContainerColor = colors.accent,
                    periodSelectorUnselectedContainerColor = colors.sheet,
                    periodSelectorSelectedContentColor = colors.onAccent,
                    periodSelectorUnselectedContentColor = colors.ink70,
                    timeSelectorSelectedContainerColor = colors.accent,
                    timeSelectorUnselectedContainerColor = colors.ink10,
                    timeSelectorSelectedContentColor = colors.onAccent,
                    timeSelectorUnselectedContentColor = colors.ink
                )
            )
        }
    }
}

/** A ≥48 dp dialog option: control first, then its label and an optional line below it. */
@Composable
private fun OptionRow(
    modifier: Modifier,
    text: String,
    supportingText: String? = null,
    control: @Composable () -> Unit
) {
    val colors = BreatheTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(modifier)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        control()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text, style = BreatheType.Body, color = colors.ink)
            if (supportingText != null) {
                Text(supportingText, style = BreatheType.Caption, color = colors.ink70)
            }
        }
    }
}

// ---- Formatting ----

@Composable
private fun currentLocale(): Locale =
    ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()

@Composable
private fun reduceMotionValue(value: ReduceMotion): String = stringResource(
    when (value) {
        ReduceMotion.System -> R.string.settings_reduce_motion_system
        ReduceMotion.On -> R.string.settings_reduce_motion_on
        ReduceMotion.Off -> R.string.settings_reduce_motion_off
    }
)

/** The System option says what the OS currently does, e.g. "System (off)". */
@Composable
private fun reduceMotionOptionLabel(value: ReduceMotion, systemOn: Boolean): String = when (value) {
    ReduceMotion.System -> stringResource(
        if (systemOn) R.string.settings_reduce_motion_system_on else R.string.settings_reduce_motion_system_off
    )
    else -> reduceMotionValue(value)
}

/** "Every day", "Weekdays", "Weekends", or short day names Monday first, e.g. "Mon, Wed, Fri". */
@Composable
private fun daysSummary(days: Set<Int>, locale: Locale): String = when (days) {
    EveryDay -> stringResource(R.string.settings_days_every_day)
    Weekdays -> stringResource(R.string.settings_days_weekdays)
    Weekends -> stringResource(R.string.settings_days_weekends)
    else -> {
        val separator = stringResource(R.string.settings_days_separator)
        val names = remember(locale) { DateFormatSymbols.getInstance(locale).shortWeekdays }
        days.sorted().joinToString(separator) { names[calendarDay(it)] }
    }
}

/** ISO day (1 = Monday … 7 = Sunday) to [Calendar] day (1 = Sunday … 7 = Saturday). */
private fun calendarDay(isoDay: Int): Int = isoDay % 7 + 1

/** Follows the device's 12/24-hour setting, e.g. "9:00 pm" or "21:00". */
private fun formatTime(context: Context, hour: Int, minute: Int): String {
    val time = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return DateFormat.getTimeFormat(context).format(time.time)
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

@Suppress("DEPRECATION")
private fun appVersion(context: Context): String? = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
} catch (e: PackageManager.NameNotFoundException) {
    null
}

@Composable
private fun SettingsPreviewContent() {
    Box(Modifier.fillMaxSize().background(BreatheTheme.colors.background)) {
        SettingsContent(
            values = SettingsValues(
                sound = true,
                haptics = true,
                minutes = 5,
                guide = GuideStyle.Default,
                reminder = ReminderSettings(enabled = true, hour = 21, minute = 0),
                reminderTime = "9:00 pm",
                reduceMotion = ReduceMotion.System,
                keepAwake = true,
                notificationsDenied = false,
                version = "1.0"
            ),
            locale = Locale.UK,
            animateToggles = true,
            onSound = {},
            onHaptics = {},
            onLength = {},
            onGuide = {},
            onReminder = {},
            onTime = {},
            onDays = {},
            onReduceMotion = {},
            onKeepAwake = {}
        )
    }
}

@Preview(name = "Dark", widthDp = 390, heightDp = 844)
@Composable
private fun SettingsContentPreview() {
    BreatheTheme(darkTheme = true) { SettingsPreviewContent() }
}

@Preview(name = "Light", widthDp = 390, heightDp = 844)
@Composable
private fun SettingsContentLightPreview() {
    BreatheTheme(darkTheme = false) { SettingsPreviewContent() }
}
