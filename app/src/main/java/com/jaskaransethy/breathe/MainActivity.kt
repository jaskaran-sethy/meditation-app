package com.jaskaransethy.breathe

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.NotificationManagerCompat
import com.jaskaransethy.breathe.data.BreathPattern
import com.jaskaransethy.breathe.data.BreatheStore
import com.jaskaransethy.breathe.reminder.ReminderNotification
import com.jaskaransethy.breathe.reminder.ReminderScheduler
import com.jaskaransethy.breathe.ui.theme.BreatheTheme

/** Where the app should open when launched from the reminder notification. */
sealed interface LaunchRequest {
    data class StartSession(val pattern: BreathPattern, val minutes: Int) : LaunchRequest
    data object OpenSettings : LaunchRequest
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Transparent bars whose icons follow the system light/dark setting, like the app's theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        val store = BreatheStore(applicationContext)
        // Only act on the launch intent once, not again after a configuration change.
        val launch = if (savedInstanceState == null) launchRequest(intent) else null
        if (launch != null) {
            store.onboardingComplete = true
            NotificationManagerCompat.from(this).cancel(ReminderNotification.ID)
        }
        // Cheap insurance in case the alarm was dropped (e.g. the app was force-stopped).
        if (store.reminder.enabled) ReminderScheduler.reschedule(this)

        setContent {
            BreatheTheme {
                BreatheApp(store, launch)
            }
        }
    }

    private fun launchRequest(intent: Intent?): LaunchRequest? = when (intent?.action) {
        ACTION_START_SESSION -> LaunchRequest.StartSession(
            BreatheStore(this).pattern(intent.getStringExtra(EXTRA_PATTERN)),
            intent.getIntExtra(EXTRA_MINUTES, 0).takeIf { it > 0 } ?: BreatheStore(this).lastMinutes
        )
        ACTION_OPEN_SETTINGS -> LaunchRequest.OpenSettings
        else -> null
    }

    companion object {
        private const val ACTION_START_SESSION = "com.jaskaransethy.breathe.START_SESSION"
        private const val ACTION_OPEN_SETTINGS = "com.jaskaransethy.breathe.OPEN_SETTINGS"
        private const val EXTRA_PATTERN = "pattern"
        private const val EXTRA_MINUTES = "minutes"

        fun startSessionIntent(context: Context, pattern: BreathPattern, minutes: Int): Intent =
            Intent(context, MainActivity::class.java)
                .setAction(ACTION_START_SESSION)
                .putExtra(EXTRA_PATTERN, pattern.id)
                .putExtra(EXTRA_MINUTES, minutes)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        fun openSettingsIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .setAction(ACTION_OPEN_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
