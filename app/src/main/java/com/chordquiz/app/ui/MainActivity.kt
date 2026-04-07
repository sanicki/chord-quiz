package com.chordquiz.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.chordquiz.app.ui.navigation.NavGraph
import com.chordquiz.app.ui.navigation.NotePlayQuizRoute
import com.chordquiz.app.ui.navigation.PlayQuizRoute
import com.chordquiz.app.ui.navigation.TunerRoute
import com.chordquiz.app.ui.theme.ChordQuizTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var navController: NavHostController? = null
    private var isPowerSaveModeActive = false

    private val powerSaveReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                isPowerSaveModeActive = pm.isPowerSaveMode
                updateScreenFlag()
            }
        }
    }

    private val destinationChangedListener =
        NavController.OnDestinationChangedListener { _, destination, _ ->
            updateScreenFlag(destination)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val controller = rememberNavController()
            navController = controller
            ChordQuizTheme {
                NavGraph(navController = controller)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        isPowerSaveModeActive = pm.isPowerSaveMode

        registerReceiver(powerSaveReceiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))

        navController?.addOnDestinationChangedListener(destinationChangedListener)

        updateScreenFlag(navController?.currentDestination)
    }

    override fun onStop() {
        super.onStop()
        navController?.removeOnDestinationChangedListener(destinationChangedListener)
        unregisterReceiver(powerSaveReceiver)
    }

    private fun updateScreenFlag(destination: NavDestination? = navController?.currentDestination) {
        val isWhitelisted = destination != null && (
            destination.hasRoute<PlayQuizRoute>() ||
            destination.hasRoute<NotePlayQuizRoute>() ||
            destination.hasRoute<TunerRoute>()
        )
        if (isWhitelisted && !isPowerSaveModeActive) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
