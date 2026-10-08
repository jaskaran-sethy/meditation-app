package com.jaskaransethy.breathe.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.jaskaransethy.breathe.data.BreatheStore
import com.jaskaransethy.breathe.data.PracticeStats
import com.jaskaransethy.breathe.data.ReminderSettings
import java.util.Calendar

/**
 * Daily reminder alarms. Alarms are inexact on purpose (no exact-alarm permission): a nudge a few
 * minutes late is fine, and it lets the system batch wake-ups.
 */
object ReminderScheduler {
    private const val REQUEST_DAILY = 1
    private const val REQUEST_SNOOZE = 2
    const val SNOOZE_MS = 60 * 60 * 1000L

    /** Schedules (or cancels) the next daily reminder from the current settings. */
    fun reschedule(context: Context) {
        val store = BreatheStore(context)
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = alarmIntent(context, ReminderReceiver.ACTION_SHOW, REQUEST_DAILY)
        alarms.cancel(pending)

        val settings = store.reminder
        if (!settings.enabled || settings.days.isEmpty()) {
            alarms.cancel(alarmIntent(context, ReminderReceiver.ACTION_SHOW_SNOOZED, REQUEST_SNOOZE))
            NotificationManagerCompat.from(context).cancel(ReminderNotification.ID)
            return
        }
        val next = nextTrigger(settings, System.currentTimeMillis(), store.reminderHandledDay)
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending)
    }

    fun snooze(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        alarms.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + SNOOZE_MS,
            alarmIntent(context, ReminderReceiver.ACTION_SHOW_SNOOZED, REQUEST_SNOOZE)
        )
    }

    /**
     * The next reminder time after [nowMillis] on a selected day, skipping a day that has already
     * had its reminder (shown, snoozed or skipped) so it is sent at most once a day.
     */
    fun nextTrigger(settings: ReminderSettings, nowMillis: Long, handledDay: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, settings.hour)
            set(Calendar.MINUTE, settings.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        repeat(8) {
            val millis = calendar.timeInMillis
            val day = PracticeStats.epochDay(millis, calendar.timeZone.getOffset(millis))
            if (millis > nowMillis && PracticeStats.dayOfWeek(day) in settings.days && day != handledDay) {
                return millis
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    private fun alarmIntent(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, ReminderReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
}
