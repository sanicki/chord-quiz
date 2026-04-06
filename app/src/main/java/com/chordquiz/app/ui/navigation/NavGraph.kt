package com.chordquiz.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.chordquiz.app.ui.screen.instrument.InstrumentSelectionScreen
import com.chordquiz.app.ui.screen.library.ChordLibraryScreen
import com.chordquiz.app.ui.navigation.ChordLibraryRoute
import com.chordquiz.app.ui.navigation.ChordPreviewRoute
import com.chordquiz.app.ui.navigation.DrawQuizRoute
import com.chordquiz.app.ui.navigation.InstrumentSelectionRoute
import com.chordquiz.app.ui.navigation.PracticeSetupRoute
import com.chordquiz.app.ui.navigation.PlayQuizRoute
import com.chordquiz.app.ui.navigation.ResultsRoute
import com.chordquiz.app.ui.navigation.SettingsRoute
import com.chordquiz.app.ui.shared.SessionStore
import com.chordquiz.app.ui.screen.preview.ChordPreviewScreen
import com.chordquiz.app.ui.screen.setup.PracticeSetupScreen
import com.chordquiz.app.ui.screen.quizdraw.DrawQuizScreen
import com.chordquiz.app.ui.screen.quizplay.PlayQuizScreen
import com.chordquiz.app.ui.screen.results.ResultsScreen
import com.chordquiz.app.ui.screen.settings.SettingsScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = InstrumentSelectionRoute) {

        composable<InstrumentSelectionRoute> {
            InstrumentSelectionScreen(
                onInstrumentSelected = { instrumentId ->
                    navController.navigate(ChordLibraryRoute(instrumentId))
                },
                onNavigateToSettings = {
                    navController.navigate(SettingsRoute)
                }
            )
        }

        composable<ChordLibraryRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ChordLibraryRoute>()
            ChordLibraryScreen(
                instrumentId = route.instrumentId,
                onNavigateBack = { navController.popBackStack() },
                onStartPractice = { instrumentId, chordIds ->
                    navController.navigate(PracticeSetupRoute(instrumentId, chordIds))
                }
            )
        }

        composable<PracticeSetupRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PracticeSetupRoute>()
            PracticeSetupScreen(
                instrumentId = route.instrumentId,
                selectedChordIds = route.selectedChordIds,
                onNavigateBack = { navController.popBackStack() },
                onStartDrawQuiz = { instrumentId, chordIds, count, repeat ->
                    navController.navigate(ChordPreviewRoute(instrumentId, chordIds, count, repeat, "draw"))
                },
                onStartPlayQuiz = { instrumentId, chordIds, count, repeat ->
                    navController.navigate(ChordPreviewRoute(instrumentId, chordIds, count, repeat, "play"))
                }
            )
        }

        composable<ChordPreviewRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ChordPreviewRoute>()
            ChordPreviewScreen(
                instrumentId = route.instrumentId,
                selectedChordIds = route.selectedChordIds,
                onBack = { navController.popBackStack() },
                onBegin = {
                    if (route.quizMode == "play") {
                        navController.navigate(PlayQuizRoute(route.instrumentId, route.selectedChordIds, route.questionCount, route.repeatMissed))
                    } else {
                        navController.navigate(DrawQuizRoute(route.instrumentId, route.selectedChordIds, route.questionCount, route.repeatMissed))
                    }
                }
            )
        }

        composable<DrawQuizRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<DrawQuizRoute>()
            DrawQuizScreen(
                instrumentId = route.instrumentId,
                selectedChordIds = route.selectedChordIds,
                questionCount = route.questionCount,
                repeatMissed = route.repeatMissed,
                onNavigateBack = { navController.popBackStack() },
                onQuizComplete = { sessionId ->
                    SessionStore.lastQuizType = "DrawQuizRoute"
                    SessionStore.lastInstrumentId = route.instrumentId
                    SessionStore.lastSelectedChordIds = route.selectedChordIds
                    SessionStore.lastQuestionCount = route.questionCount
                    SessionStore.lastRepeatMissed = route.repeatMissed
                    navController.navigate(ResultsRoute(sessionId, "DrawQuizRoute")) {
                        popUpTo(InstrumentSelectionRoute)
                    }
                }
            )
        }

        composable<PlayQuizRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PlayQuizRoute>()
            PlayQuizScreen(
                instrumentId = route.instrumentId,
                selectedChordIds = route.selectedChordIds,
                questionCount = route.questionCount,
                repeatMissed = route.repeatMissed,
                onNavigateBack = { navController.popBackStack() },
                onQuizComplete = { sessionId ->
                    SessionStore.lastQuizType = "PlayQuizRoute"
                    SessionStore.lastInstrumentId = route.instrumentId
                    SessionStore.lastSelectedChordIds = route.selectedChordIds
                    SessionStore.lastQuestionCount = route.questionCount
                    SessionStore.lastRepeatMissed = route.repeatMissed
                    navController.navigate(ResultsRoute(sessionId, "PlayQuizRoute")) {
                        popUpTo(InstrumentSelectionRoute)
                    }
                }
            )
        }

        composable<ResultsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ResultsRoute>()
            ResultsScreen(
                sessionId = route.sessionId,
                onNavigateHome = {
                    navController.navigate(InstrumentSelectionRoute) {
                        popUpTo(InstrumentSelectionRoute) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    val instrumentId = SessionStore.lastInstrumentId
                    val chordIds = SessionStore.lastSelectedChordIds
                    if (instrumentId != null && chordIds != null) {
                        val modeStr = if (SessionStore.lastQuizType == "PlayQuizRoute") "PLAY" else "DRAW"
                        navController.navigate(PracticeSetupRoute(
                            instrumentId = instrumentId,
                            selectedChordIds = chordIds,
                            preserveSettings = true,
                            initialMode = modeStr,
                            initialRepeatMissed = SessionStore.lastRepeatMissed
                        )) {
                            popUpTo(InstrumentSelectionRoute)
                        }
                    } else {
                        navController.navigate(InstrumentSelectionRoute) {
                            popUpTo(InstrumentSelectionRoute) { inclusive = true }
                        }
                    }
                },
                onRestartQuiz = {
                    val instrumentId = SessionStore.lastInstrumentId
                    val chordIds = SessionStore.lastSelectedChordIds
                    if (instrumentId != null && chordIds != null) {
                        val quizMode = if (route.restartRoute == "PlayQuizRoute") "play" else "draw"
                        navController.navigate(ChordPreviewRoute(
                            instrumentId = instrumentId,
                            selectedChordIds = chordIds,
                            questionCount = SessionStore.lastQuestionCount,
                            repeatMissed = SessionStore.lastRepeatMissed,
                            quizMode = quizMode
                        )) {
                            popUpTo(InstrumentSelectionRoute)
                        }
                    } else {
                        navController.navigate(InstrumentSelectionRoute) {
                            popUpTo(InstrumentSelectionRoute) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
