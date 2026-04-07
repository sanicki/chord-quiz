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
import com.chordquiz.app.data.model.NoteMode
import com.chordquiz.app.ui.navigation.NoteDrawQuizRoute
import com.chordquiz.app.ui.navigation.NotePracticeSetupRoute
import com.chordquiz.app.ui.navigation.NotePlayQuizRoute
import com.chordquiz.app.ui.navigation.NoteQuizModeRoute
import com.chordquiz.app.ui.navigation.PracticeSetupRoute
import com.chordquiz.app.ui.navigation.PlayQuizRoute
import com.chordquiz.app.ui.navigation.ResultsRoute
import com.chordquiz.app.ui.navigation.SettingsRoute
import com.chordquiz.app.ui.navigation.TunerRoute
import com.chordquiz.app.ui.screen.tuner.TunerScreen
import com.chordquiz.app.ui.shared.SessionStore
import com.chordquiz.app.ui.screen.notemode.NoteQuizModeScreen
import com.chordquiz.app.ui.screen.notedrawquiz.NoteDrawQuizScreen
import com.chordquiz.app.ui.screen.noteplayquiz.NotePlayQuizScreen
import com.chordquiz.app.ui.screen.setup.NotePracticeSetupScreen
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
                onChordInstrumentSelected = { instrumentId ->
                    navController.navigate(ChordLibraryRoute(instrumentId))
                },
                onNoteInstrumentSelected = { instrumentId ->
                    navController.navigate(NoteQuizModeRoute(instrumentId))
                },
                onTunerInstrumentSelected = { instrumentId ->
                    navController.navigate(TunerRoute(instrumentId))
                },
                onNavigateToSettings = {
                    navController.navigate(SettingsRoute)
                }
            )
        }

        composable<TunerRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<TunerRoute>()
            TunerScreen(
                instrumentId = route.instrumentId,
                onNavigateHome = {
                    navController.navigate(InstrumentSelectionRoute) {
                        popUpTo(InstrumentSelectionRoute) { inclusive = true }
                    }
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
                    val quizType = SessionStore.lastQuizType
                    val isNoteQuiz = quizType == "NoteDrawQuizRoute" || quizType == "NotePlayQuizRoute"
                    if (instrumentId != null && isNoteQuiz) {
                        val noteModeStr = SessionStore.lastNoteMode
                        val noteMode = noteModeStr?.let { runCatching { NoteMode.valueOf(it) }.getOrNull() }
                        if (noteMode != null) {
                            navController.navigate(NotePracticeSetupRoute(
                                instrumentId = instrumentId,
                                noteMode = noteMode,
                                preserveSettings = true,
                                initialRepeatMissed = SessionStore.lastRepeatMissed
                            )) { popUpTo(InstrumentSelectionRoute) }
                        } else {
                            navController.navigate(InstrumentSelectionRoute) {
                                popUpTo(InstrumentSelectionRoute) { inclusive = true }
                            }
                        }
                    } else {
                        val chordIds = SessionStore.lastSelectedChordIds
                        if (instrumentId != null && chordIds != null) {
                            val modeStr = if (quizType == "PlayQuizRoute") "PLAY" else "DRAW"
                            navController.navigate(PracticeSetupRoute(
                                instrumentId = instrumentId,
                                selectedChordIds = chordIds,
                                preserveSettings = true,
                                initialMode = modeStr,
                                initialRepeatMissed = SessionStore.lastRepeatMissed
                            )) { popUpTo(InstrumentSelectionRoute) }
                        } else {
                            navController.navigate(InstrumentSelectionRoute) {
                                popUpTo(InstrumentSelectionRoute) { inclusive = true }
                            }
                        }
                    }
                },
                onRestartQuiz = {
                    val instrumentId = SessionStore.lastInstrumentId
                    val quizType = SessionStore.lastQuizType
                    val isNoteQuiz = quizType == "NoteDrawQuizRoute" || quizType == "NotePlayQuizRoute"
                    if (instrumentId != null && isNoteQuiz) {
                        val noteModeStr = SessionStore.lastNoteMode
                        val noteMode = noteModeStr?.let { runCatching { NoteMode.valueOf(it) }.getOrNull() }
                        if (noteMode != null) {
                            if (route.restartRoute == "NotePlayQuizRoute") {
                                navController.navigate(NotePlayQuizRoute(
                                    instrumentId = instrumentId,
                                    noteMode = noteMode,
                                    questionCount = SessionStore.lastQuestionCount,
                                    repeatMissed = SessionStore.lastRepeatMissed
                                )) { popUpTo(InstrumentSelectionRoute) }
                            } else {
                                navController.navigate(NoteDrawQuizRoute(
                                    instrumentId = instrumentId,
                                    noteMode = noteMode,
                                    questionCount = SessionStore.lastQuestionCount,
                                    repeatMissed = SessionStore.lastRepeatMissed
                                )) { popUpTo(InstrumentSelectionRoute) }
                            }
                        } else {
                            navController.navigate(InstrumentSelectionRoute) {
                                popUpTo(InstrumentSelectionRoute) { inclusive = true }
                            }
                        }
                    } else {
                        val chordIds = SessionStore.lastSelectedChordIds
                        if (instrumentId != null && chordIds != null) {
                            val quizMode = if (route.restartRoute == "PlayQuizRoute") "play" else "draw"
                            navController.navigate(ChordPreviewRoute(
                                instrumentId = instrumentId,
                                selectedChordIds = chordIds,
                                questionCount = SessionStore.lastQuestionCount,
                                repeatMissed = SessionStore.lastRepeatMissed,
                                quizMode = quizMode
                            )) { popUpTo(InstrumentSelectionRoute) }
                        } else {
                            navController.navigate(InstrumentSelectionRoute) {
                                popUpTo(InstrumentSelectionRoute) { inclusive = true }
                            }
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

        composable<NoteQuizModeRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<NoteQuizModeRoute>()
            NoteQuizModeScreen(
                instrumentId = route.instrumentId,
                onNavigateBack = { navController.popBackStack() },
                onModeSelected = { noteMode ->
                    navController.navigate(NotePracticeSetupRoute(route.instrumentId, noteMode))
                }
            )
        }

        composable<NotePracticeSetupRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<NotePracticeSetupRoute>()
            NotePracticeSetupScreen(
                instrumentId = route.instrumentId,
                noteMode = route.noteMode,
                onNavigateBack = { navController.popBackStack() },
                onStartDrawQuiz = { instrumentId, noteMode, count, repeat ->
                    navController.navigate(NoteDrawQuizRoute(instrumentId, noteMode, count, repeat))
                },
                onStartPlayQuiz = { instrumentId, noteMode, count, repeat ->
                    navController.navigate(NotePlayQuizRoute(instrumentId, noteMode, count, repeat))
                }
            )
        }

        composable<NoteDrawQuizRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<NoteDrawQuizRoute>()
            NoteDrawQuizScreen(
                instrumentId = route.instrumentId,
                noteMode = route.noteMode,
                questionCount = route.questionCount,
                repeatMissed = route.repeatMissed,
                onNavigateBack = { navController.popBackStack() },
                onQuizComplete = { sessionId ->
                    SessionStore.lastQuizType = "NoteDrawQuizRoute"
                    SessionStore.lastInstrumentId = route.instrumentId
                    SessionStore.lastNoteMode = route.noteMode.name
                    SessionStore.lastQuestionCount = route.questionCount
                    SessionStore.lastRepeatMissed = route.repeatMissed
                    navController.navigate(ResultsRoute(sessionId, "NoteDrawQuizRoute")) {
                        popUpTo(InstrumentSelectionRoute)
                    }
                }
            )
        }

        composable<NotePlayQuizRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<NotePlayQuizRoute>()
            NotePlayQuizScreen(
                instrumentId = route.instrumentId,
                noteMode = route.noteMode,
                questionCount = route.questionCount,
                repeatMissed = route.repeatMissed,
                onNavigateBack = { navController.popBackStack() },
                onQuizComplete = { sessionId ->
                    SessionStore.lastQuizType = "NotePlayQuizRoute"
                    SessionStore.lastInstrumentId = route.instrumentId
                    SessionStore.lastNoteMode = route.noteMode.name
                    SessionStore.lastQuestionCount = route.questionCount
                    SessionStore.lastRepeatMissed = route.repeatMissed
                    navController.navigate(ResultsRoute(sessionId, "NotePlayQuizRoute")) {
                        popUpTo(InstrumentSelectionRoute)
                    }
                }
            )
        }
    }
}
