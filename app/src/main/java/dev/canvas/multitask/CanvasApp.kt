package dev.canvas.multitask

import android.app.Application
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import org.lsposed.hiddenapibypass.HiddenApiBypass
import android.util.Log

/**
 * Canvas Application class.
 *
 * Responsibilities:
 * - Initialize Hilt DI
 * - Bypass hidden API restrictions (Android 9+) via LSPosed HiddenApiBypass
 *   so that reflection into IActivityTaskManager and friends works from the app process
 */
@HiltAndroidApp
class CanvasApp : Application() {

    companion object {
        private const val TAG = "CanvasApp"
    }

    override fun onCreate() {
        super.onCreate()

        // Bypass hidden API restrictions for Android 9+ (API 28+)
        // This allows reflection into hidden system APIs like ActivityOptions.setLaunchWindowingMode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                HiddenApiBypass.addHiddenApiExemptions("")
                Log.i(TAG, "Hidden API bypass initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize hidden API bypass", e)
            }
        }
    }
}
