package dev.canvas.multitask.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.canvas.multitask.data.prefs.PreferencesManager
import dev.canvas.multitask.data.shizuku.ShizukuManager
import dev.canvas.multitask.ui.navigation.CanvasNavGraph
import dev.canvas.multitask.ui.theme.CanvasTheme
import javax.inject.Inject

/**
 * Single activity for the Canvas app.
 * Uses Compose for all UI and Navigation Component for routing.
 * Initializes/cleans up Shizuku listeners with the activity lifecycle.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var shizukuManager: ShizukuManager

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Shizuku lifecycle listeners
        shizukuManager.initialize()

        enableEdgeToEdge()

        setContent {
            CanvasTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    CanvasNavGraph(
                        navController = navController,
                        preferencesManager = preferencesManager
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        shizukuManager.cleanup()
    }
}
