package com.jaskaransethy.breathe.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jaskaransethy.breathe.R
import com.jaskaransethy.breathe.data.BreathPattern
import com.jaskaransethy.breathe.data.BreatheStore
import com.jaskaransethy.breathe.data.shouldReduceMotion
import com.jaskaransethy.breathe.ui.icons.BreatheIcons
import com.jaskaransethy.breathe.ui.theme.BreatheTheme
import com.jaskaransethy.breathe.ui.theme.screenBackground
import com.jaskaransethy.breathe.ui.theme.BreatheType

enum class MainTab(val label: Int, val icon: ImageVector) {
    Breathe(R.string.tab_breathe, BreatheIcons.Wind),
    Progress(R.string.tab_progress, BreatheIcons.Chart),
    Settings(R.string.tab_settings, BreatheIcons.Settings)
}

@Composable
fun MainScreen(
    store: BreatheStore,
    onBegin: (BreathPattern, Int) -> Unit,
    onOpenBadges: () -> Unit,
    onReplayWalkthrough: () -> Unit,
    initialTab: MainTab = MainTab.Breathe
) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    val context = LocalContext.current
    val fadeMs = remember { if (shouldReduceMotion(context, store)) 0 else 400 }

    Box(Modifier.fillMaxSize().screenBackground(BreatheTheme.colors)) {
        Column(Modifier.fillMaxSize()) {
            Crossfade(
                targetState = tab,
                animationSpec = tween(fadeMs),
                label = "tab",
                modifier = Modifier.weight(1f)
            ) { current ->
                when (current) {
                    MainTab.Breathe -> HomeTab(store, onBegin)
                    MainTab.Progress -> ProgressTab(store, onOpenBadges)
                    MainTab.Settings -> SettingsTab(store, onReplayWalkthrough)
                }
            }
            TabBar(selected = tab, onSelect = { tab = it })
        }
    }
}

@Composable
private fun TabBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    val colors = BreatheTheme.colors
    val shape = RoundedCornerShape(30.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(shape)
                .background(colors.surfaceRaised)
                .border(1.dp, colors.ink12, shape)
                .padding(6.dp)
                .selectableGroup()
        ) {
            MainTab.entries.forEach { tab ->
                val isSelected = tab == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isSelected) colors.tabSelected else Color.Transparent)
                        .selectable(selected = isSelected, role = Role.Tab) { onSelect(tab) },
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val color = if (isSelected) colors.ink else colors.ink60
                    Icon(tab.icon, null, Modifier.size(22.dp), tint = color)
                    Text(
                        stringResource(tab.label),
                        style = if (isSelected) BreatheType.TabSelected else BreatheType.Tab,
                        color = color
                    )
                }
            }
        }
    }
}
