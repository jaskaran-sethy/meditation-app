package com.jaskaransethy.breathe.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaskaransethy.breathe.R
import com.jaskaransethy.breathe.ui.components.PhotoBackground
import com.jaskaransethy.breathe.ui.components.PrimaryButton
import com.jaskaransethy.breathe.ui.icons.BreatheIcons
import com.jaskaransethy.breathe.ui.theme.BreatheTheme
import com.jaskaransethy.breathe.ui.theme.BreatheType

@Composable
fun WelcomeScreen(onBegin: () -> Unit) {
    val colors = BreatheTheme.colors
    // Covers the top behind the wordmark and the bottom behind the call to action:
    // Night in dark mode, a pale morning mist in light mode.
    val scrim = Brush.verticalGradient(
        0.00f to colors.scrim(0.90f),
        0.20f to colors.scrim(0.70f),
        0.38f to colors.scrim(0f),
        0.45f to colors.scrim(0f),
        0.70f to colors.scrim(0.80f),
        1.00f to colors.scrim(0.96f)
    )
    PhotoBackground(overlay = scrim) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(start = 28.dp, top = 56.dp, end = 28.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.wordmark),
                    style = BreatheType.Wordmark.copy(
                        shadow = Shadow(colors.scrim(0.7f), Offset(0f, 2f), 12f)
                    ),
                    color = colors.ink,
                    // Always one line: on narrow phones or large font settings it shrinks to fit.
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 32.sp,
                        maxFontSize = BreatheType.Wordmark.fontSize,
                        stepSize = 1.sp
                    ),
                    maxLines = 1,
                    softWrap = false,
                    // Letter spacing trails the last letter; nudge right so the word looks centred.
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .semantics { heading() }
                )
                Box(Modifier.width(32.dp).height(1.dp).background(colors.ink))
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.welcome_tagline),
                    style = BreatheType.Tagline,
                    color = colors.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                PrimaryButton(text = stringResource(R.string.begin), onClick = onBegin, elevated = true)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(BreatheIcons.Timer, null, Modifier.size(15.dp), tint = colors.ink80)
                    Text(
                        stringResource(R.string.welcome_session_length),
                        style = BreatheType.Small,
                        color = colors.ink80
                    )
                }
            }
        }
    }
}
