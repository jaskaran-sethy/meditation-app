package com.jaskaransethy.breathe.ui.components

import android.content.Context
import android.content.Intent
import com.jaskaransethy.breathe.R

/**
 * Opens the system share sheet with plain text. Sharing is always the user's choice: the app never
 * prompts for it (design principle "Quiet celebration — no pressure to share").
 */
fun Context.shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(send, getString(R.string.share_chooser_title)))
}
