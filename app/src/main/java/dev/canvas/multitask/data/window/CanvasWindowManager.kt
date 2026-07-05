package dev.canvas.multitask.data.window

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.canvas.multitask.data.apps.AppRepository
import dev.canvas.multitask.data.shizuku.ShizukuManager
import dev.canvas.multitask.domain.*
import dev.canvas.multitask.service.ICanvasWindowService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates freeform window launches and session management.
 * Coordinates between AppRepository (intents), ShizukuManager (service),
 * and BoundsCalculator (layout geometry).
 */
@Singleton
class CanvasWindowManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shizukuManager: ShizukuManager,
    private val appRepository: AppRepository
) {

    companion object {
        private const val TAG = "CanvasWindowManager"
        private const val LAUNCH_DELAY_MS = 500L // delay between sequential launches
    }

    private var currentSession: CanvasSession? = null

    /**
     * Get the current display dimensions.
     */
    fun getDisplayMetrics(): DisplayMetrics {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        return metrics
    }

    private var pendingPackages: List<String> = emptyList()
    private var pendingLayout: WindowLayout = WindowLayout.Single

    /**
     * Launch a multi-window session with the selected apps.
     * In Virtual Display mode, this just stores the pending packages and layout,
     * and the UI will navigate to the Workspace screen.
     *
     * @param packageNames List of 2-3 package names to launch
     * @param preferredLayout The layout to use (optional, required if 3 apps)
     * @return The created CanvasSession, or null on failure
     */
    suspend fun prepareSession(packageNames: List<String>, preferredLayout: WindowLayout? = null): Boolean {
        val service = shizukuManager.windowService ?: run {
            Log.e(TAG, "Window service not connected")
            return false
        }

        val layout = preferredLayout ?: when (packageNames.size) {
            1 -> WindowLayout.Single
            2 -> WindowLayout.SideBySide
            3 -> WindowLayout.ThreeColumns
            else -> {
                Log.e(TAG, "Invalid app count: ${packageNames.size}")
                return false
            }
        }

        pendingPackages = packageNames
        pendingLayout = layout
        return true
    }

    fun getPendingPackages(): List<String> = pendingPackages
    fun getPendingLayout(): WindowLayout = pendingLayout

    /**
     * Switch focus to a different app in the current session (park/recall).
     */
    suspend fun switchFocus(targetSlotIndex: Int) {
        val session = currentSession ?: return
        val service = shizukuManager.windowService ?: return

        if (targetSlotIndex == session.activeSlotIndex) return
        if (targetSlotIndex !in session.slots.indices) return

        val metrics = getDisplayMetrics()
        val displayWidth = metrics.widthPixels
        val displayHeight = metrics.heightPixels

        val outgoing = session.activeSlot
        val incoming = session.slots[targetSlotIndex]

        // Not used in Virtual Display architecture.
        // Sliders/panning are handled by Compose UI.
    }

    fun getCurrentSession(): CanvasSession? = currentSession

    /**
     * Helper methods for static bounds are now in BoundsCalculator.computeSessionBounds
     */
}
