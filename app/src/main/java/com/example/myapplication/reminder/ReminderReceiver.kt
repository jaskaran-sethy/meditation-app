package com.example.myapplication.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.myapplication.data.BreatheStore
import com.example.myapplication.data.PracticeStats

/** Handles the reminder alarm and the notification's "Remind me in 1 hour" / "Skip today" actions. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val store = BreatheStore(context)
        val today = store.today()
        when (intent.action) {
            ACTION_SHOW -> {
                val settings = store.reminder
                val due = settings.enabled &&
                    PracticeStats.dayOfWeek(today) in settings.days &&
                    store.reminderHandledDay != today
                // No nudge on a day the user has already breathed.
                if (due && !store.practicedOn(today)) {
                    store.reminderHandledDay = today
                    ReminderNotification.show(context, store)
                }
                ReminderScheduler.reschedule(context)
            }
            ACTION_SHOW_SNOOZED -> {
                if (store.reminder.enabled && !store.practicedOn(today)) {
                    ReminderNotification.show(context, store)
                }
            }
            ACTION_SNOOZE -> {
                NotificationManagerCompat.from(context).cancel(ReminderNotification.ID)
                ReminderScheduler.snooze(context)
            }
            ACTION_SKIP -> {
                NotificationManagerCompat.from(context).cancel(ReminderNotification.ID)
                store.reminderHandledDay = today
                store.reminderSkipStreak = store.reminderSkipStreak + 1
                ReminderScheduler.reschedule(context)
            }
        }
    }

    companion object {
        const val ACTION_SHOW = "com.example.myapplication.reminder.SHOW"
        const val ACTION_SHOW_SNOOZED = "com.example.myapplication.reminder.SHOW_SNOOZED"
        const val ACTION_SNOOZE = "com.example.myapplication.reminder.SNOOZE"
        const val ACTION_SKIP = "com.example.myapplication.reminder.SKIP"
    }
}

/** Restores the reminder alarm after a reboot, an app update, or a clock or time-zone change. */
class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.reschedule(context)
    }
}
