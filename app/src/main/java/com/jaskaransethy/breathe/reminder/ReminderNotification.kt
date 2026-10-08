package com.jaskaransethy.breathe.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jaskaransethy.breathe.MainActivity
import com.jaskaransethy.breathe.R
import com.jaskaransethy.breathe.data.BreatheStore
import java.util.Calendar

object ReminderNotification {
    const val ID = 1
    private const val CHANNEL_ID = "daily_reminder"
    /** After this many skips in a row, the reminder asks whether to change its time. */
    private const val SKIPS_BEFORE_ASKING = 3

    fun show(context: Context, store: BreatheStore) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        createChannel(context)

        // Session and length match the last one the user chose.
        val pattern = store.lastPattern
        val minutes = store.lastMinutes
        val patternName = context.getString(pattern.title)
        val start = MainActivity.startSessionIntent(context, pattern, minutes)
        val startPending = PendingIntent.getActivity(
            context, 0, start, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Inviting copy, never guilt. After repeated skips, gently offer a different time instead.
        val askToChangeTime = store.reminderSkipStreak >= SKIPS_BEFORE_ASKING
        val text: String
        val contentIntent: PendingIntent
        if (askToChangeTime) {
            store.reminderSkipStreak = 0
            val settings = store.reminder
            val time = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, settings.hour)
                set(Calendar.MINUTE, settings.minute)
            }
            text = context.getString(
                R.string.reminder_change_time_text,
                DateFormat.getTimeFormat(context).format(time.time)
            )
            contentIntent = PendingIntent.getActivity(
                context, 1, MainActivity.openSettingsIntent(context),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            text = context.getString(R.string.reminder_text, minutes, patternName)
            // Tapping goes straight into the session, skipping Home.
            contentIntent = startPending
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.mint))
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.reminder_action_start, patternName, minutes), startPending)
            .addAction(0, context.getString(R.string.reminder_action_snooze), receiverIntent(context, ReminderReceiver.ACTION_SNOOZE, 2))
            .addAction(0, context.getString(R.string.reminder_action_skip), receiverIntent(context, ReminderReceiver.ACTION_SKIP, 3))
            .build()

        NotificationManagerCompat.from(context).notify(ID, notification)
    }

    private fun receiverIntent(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, ReminderReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.reminder_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
