package com.jaskaransethy.breathe.data

import android.content.Context
import android.provider.Settings

/** True when motion should be reduced: the Settings override wins, otherwise the OS decides. */
fun shouldReduceMotion(context: Context, store: BreatheStore): Boolean = when (store.reduceMotion) {
    ReduceMotion.On -> true
    ReduceMotion.Off -> false
    ReduceMotion.System -> systemReducesMotion(context)
}

/** Android's "Remove animations" accessibility setting sets the animator scale to 0. */
fun systemReducesMotion(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
