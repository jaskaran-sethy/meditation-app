package com.example.myapplication.ui.session

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.myapplication.R

/**
 * The three short sound cues from the design: a soft rising tone on the inhale, a falling tone on
 * the exhale, and a single low bell at the end. Files are CC0; see assets/licenses/SOUNDS.txt.
 */
class SessionSounds(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val loaded = mutableSetOf<Int>()
    // Loading is asynchronous; a session starts straight away, so the first cue may need to wait.
    private var pending: Pair<Int, Float>? = null

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status != 0) return@setOnLoadCompleteListener
            loaded += sampleId
            pending?.takeIf { it.first == sampleId }?.let { (sound, volume) ->
                pending = null
                play(sound, volume)
            }
        }
    }

    private val inhale = pool.load(context, R.raw.breath_in, 1)
    private val exhale = pool.load(context, R.raw.breath_out, 1)
    private val end = pool.load(context, R.raw.session_end, 1)

    fun inhale() = play(inhale, CUE_VOLUME)
    fun exhale() = play(exhale, CUE_VOLUME)
    fun end() = play(end, 1f)

    fun release() = pool.release()

    private fun play(sound: Int, volume: Float) {
        if (sound in loaded) {
            pool.play(sound, volume, volume, 1, 0, 1f)
        } else {
            pending = sound to volume
        }
    }

    private companion object {
        // Phase cues sit quietly under the breath; the closing bell is a little more present.
        const val CUE_VOLUME = 0.6f
    }
}
