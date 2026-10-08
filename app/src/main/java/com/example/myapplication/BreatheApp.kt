package com.example.myapplication

import com.example.myapplication.ui.walkthrough.WalkthroughScreen
import com.example.myapplication.ui.walkthrough.FirstSessionMinutes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.data.Badge
import com.example.myapplication.data.BadgeRules
import com.example.myapplication.data.BreathPattern
import com.example.myapplication.data.BreatheStore
import com.example.myapplication.data.DefaultSessionMinutes
import com.example.myapplication.data.shouldReduceMotion
import com.example.myapplication.ui.badges.BadgeEarnedSheet
import com.example.myapplication.ui.badges.BadgesScreen
import com.example.myapplication.ui.complete.CompleteScreen
import com.example.myapplication.ui.home.MainScreen
import com.example.myapplication.ui.home.MainTab
import com.example.myapplication.ui.patterns.PatternEditorScreen
import com.example.myapplication.ui.session.SessionResult
import com.example.myapplication.ui.session.SessionScreen
import com.example.myapplication.ui.welcome.WelcomeScreen

private object Routes {
    const val WELCOME = "welcome"
    const val MAIN = "main"
    const val SESSION = "session/{pattern}/{minutes}"
    const val COMPLETE = "complete/{pattern}/{minutes}/{breaths}?badges={badges}"
    const val BADGES = "badges"
    const val WALKTHROUGH = "walkthrough"
    const val PATTERN = "pattern?id={id}"

    fun pattern(id: String?) = if (id == null) "pattern" else "pattern?id=$id"
    fun session(pattern: BreathPattern, minutes: Int) = "session/${pattern.id}/$minutes"
    fun complete(pattern: BreathPattern, minutes: Int, breaths: Int, badges: List<Badge>) =
        "complete/${pattern.id}/$minutes/$breaths?badges=${badges.joinToString(",") { it.id }}"
}

// Every screen change is a slow crossfade: no slides, bounces or springs.
private const val SCREEN_FADE_MS = 400

@Composable
fun BreatheApp(store: BreatheStore, launch: LaunchRequest? = null) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val startDestination = remember { if (store.onboardingComplete) Routes.MAIN else Routes.WELCOME }
    // With reduced motion, screens change instantly instead of crossfading.
    val fadeMs = if (shouldReduceMotion(context, store)) 0 else SCREEN_FADE_MS

    /** Saves a counted session and returns any badges it earned (each is shown only once). */
    fun record(pattern: BreathPattern, minutes: Int, result: SessionResult): List<Badge> {
        store.recordSession(
            store.sessionRecord(pattern, minutes, result.completedSeconds, result.paused, result.startMillis)
        )
        val earned = BadgeRules.newlyEarned(store.sessionLog(), store.today(), store.earnedBadges)
        store.markEarned(earned)
        return earned
    }

    // The reminder notification opens straight into a session (skipping Home) or into Settings.
    LaunchedEffect(launch) {
        if (launch is LaunchRequest.StartSession) {
            navController.navigate(Routes.session(launch.pattern, launch.minutes))
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(tween(fadeMs)) },
        exitTransition = { fadeOut(tween(fadeMs)) },
        popEnterTransition = { fadeIn(tween(fadeMs)) },
        popExitTransition = { fadeOut(tween(fadeMs)) }
    ) {
        composable(Routes.WELCOME) {
            // Begin opens the 3-step walkthrough, which leads into a short first session.
            WelcomeScreen(onBegin = { navController.navigate(Routes.WALKTHROUGH) })
        }

        composable(Routes.WALKTHROUGH) {
            WalkthroughScreen(store = store, onStart = { pattern ->
                // Shown once: from here on the app opens on Home. Home sits under the session.
                store.onboardingComplete = true
                store.lastPattern = pattern
                navController.navigate(Routes.MAIN) { popUpTo(0) }
                navController.navigate(Routes.session(pattern, FirstSessionMinutes))
            })
        }

        composable(Routes.MAIN) {
            MainScreen(
                store = store,
                initialTab = if (launch == LaunchRequest.OpenSettings) MainTab.Settings else MainTab.Breathe,
                onBegin = { pattern, minutes -> navController.navigate(Routes.session(pattern, minutes)) },
                onEditPattern = { id -> navController.navigate(Routes.pattern(id)) },
                onOpenBadges = { navController.navigate(Routes.BADGES) },
                onReplayWalkthrough = { navController.navigate(Routes.WALKTHROUGH) }
            )
        }

        composable(
            Routes.PATTERN,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            val id = entry.arguments?.getString("id")
            val existing = remember(id) { store.customPatterns.firstOrNull { it.id == id } }
            // Act once: a double-tapped Save must not store the pattern twice or pop Home too.
            fun close(action: () -> Unit) {
                if (navController.currentBackStackEntry != entry) return
                action()
                navController.popBackStack()
            }
            PatternEditorScreen(
                existing = existing,
                onSave = { pattern ->
                    close {
                        // A saved pattern is selected on Home, ready to Begin.
                        store.saveCustomPattern(pattern)
                        store.lastPattern = pattern
                    }
                },
                onDelete = { pattern -> close { store.deleteCustomPattern(pattern.id) } },
                onBack = { close { } }
            )
        }

        composable(Routes.BADGES) {
            BadgesScreen(store = store, onBack = { navController.popBackStack() })
        }

        composable(
            Routes.SESSION,
            arguments = listOf(
                navArgument("pattern") { type = NavType.StringType },
                navArgument("minutes") { type = NavType.IntType }
            )
        ) { entry ->
            val pattern = remember { store.pattern(entry.arguments?.getString("pattern")) }
            val minutes = entry.arguments?.getInt("minutes") ?: DefaultSessionMinutes
            // Read per session so a change in Settings applies; the session's own toggle saves too.
            var soundEnabled by remember { mutableStateOf(store.soundEnabled) }
            SessionScreen(
                pattern = pattern,
                minutes = minutes,
                guideStyle = store.guideStyle,
                soundEnabled = soundEnabled,
                hapticsEnabled = store.hapticsEnabled,
                keepScreenAwake = store.keepScreenAwake,
                reduceMotion = shouldReduceMotion(context, store),
                onToggleSound = {
                    soundEnabled = !soundEnabled
                    store.soundEnabled = soundEnabled
                },
                onClose = { result ->
                    // Closing after 90% still counts; any badge it earns is shown after the next session.
                    if (result != null) record(pattern, minutes, result)
                    navController.popBackStack()
                },
                onFinished = { result ->
                    val earned = record(pattern, minutes, result)
                    navController.navigate(Routes.complete(pattern, minutes, result.breaths, earned)) {
                        popUpTo(Routes.SESSION) { inclusive = true }
                    }
                }
            )
        }

        composable(
            Routes.COMPLETE,
            arguments = listOf(
                navArgument("pattern") { type = NavType.StringType },
                navArgument("minutes") { type = NavType.IntType },
                navArgument("breaths") { type = NavType.IntType },
                navArgument("badges") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { entry ->
            val pattern = remember { store.pattern(entry.arguments?.getString("pattern")) }
            val minutes = entry.arguments?.getInt("minutes") ?: DefaultSessionMinutes
            val streakDays = remember { store.summary().streakDays }
            val earned = remember {
                entry.arguments?.getString("badges").orEmpty().split(',').mapNotNull(Badge::fromId)
            }
            // The badge sheet is shown once, over Complete, right after the session that earned it.
            var showEarned by rememberSaveable { mutableStateOf(earned.isNotEmpty()) }
            Box {
                // While the sheet covers Complete, keep screen readers on the sheet.
                Box(if (showEarned) Modifier.clearAndSetSemantics { } else Modifier) {
                CompleteScreen(
                    minutes = minutes,
                    breaths = entry.arguments?.getInt("breaths") ?: 0,
                    streakDays = streakDays,
                    onDone = { mood ->
                        mood?.let(store::recordMood)
                        navController.returnHome()
                    },
                    onBreatheAgain = { mood ->
                        mood?.let(store::recordMood)
                        navController.navigate(Routes.session(pattern, minutes)) {
                            popUpTo(Routes.COMPLETE) { inclusive = true }
                        }
                    }
                )
                }
                if (showEarned) {
                    BadgeEarnedSheet(
                        badges = earned,
                        store = store,
                        onDone = { showEarned = false },
                        onViewAll = {
                            showEarned = false
                            navController.navigate(Routes.BADGES)
                        }
                    )
                }
            }
        }
    }
}

private fun NavHostController.returnHome() {
    if (!popBackStack(Routes.MAIN, inclusive = false)) {
        navigate(Routes.MAIN) { popUpTo(0) }
    }
}
