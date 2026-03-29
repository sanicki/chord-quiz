package com.chordquiz.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.chordquiz.app.ui.screen.instrument.InstrumentSelectionScreen
import com.chordquiz.app.ui.screen.library.ChordLibraryScreen
import com.chordquiz.app.ui.screen.setup.PracticeSetupScreen
import com.chordquiz.app.ui.screen.quizdraw.DrawQuizScreen
import com.chordquiz.app.ui.screen.quizplay.PlayQuizScreen
import com.chordquiz.app.ui.screen.results.ResultsScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = InstrumentSelectionRoute) {

        composable<InstrumentSelectionRoute> {
            InstrumentSelectionScreen(
                onInstrumentSelected = { instrumentId ->
                    navController.navigate(ChordLibraryRoute(instrumentId))
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
                    navController.navigate(DrawQuizRoute(instrumentId, chordIds, count, repeat))
                },
                onStartPlayQuiz = { instrumentId, chordIds, count, repeat ->
                    navController.navigate(PlayQuizRoute(instrumentId, chordIds, count, repeat))
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
                    navController.navigate(ResultsRoute(sessionId)) {
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
                    navController.navigate(ResultsRoute(sessionId)) {
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
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
