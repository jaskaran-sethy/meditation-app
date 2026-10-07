package com.jaskaransethy.breathe.ui.session

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import com.jaskaransethy.breathe.data.Phase

/**
 * Non-visual guidance so a session can be followed with eyes closed: a soft tone and a light tap
 * at the start of each inhale and exhale, and a bell with a success pattern at the end. Haptics
 * follow both the app's Haptics setting and the system setting; holds are silent.
 */
class SessionCues(
    private val view: View,
    private val sounds: SessionSounds,
    private val hapticsEnabled: Boolean
) {

    fun phaseStarted(phase: Phase, soundEnabled: Boolean) {
        when (phase) {
            Phase.Inhale -> if (soundEnabled) sounds.inhale()
            Phase.Exhale -> if (soundEnabled) sounds.exhale()
            Phase.HoldIn, Phase.HoldOut -> return
        }
        if (hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun sessionEnded(soundEnabled: Boolean) {
        if (soundEnabled) sounds.end()
        if (!hapticsEnabled) return
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(effect)
    }
}
