package dev.canvas.multitask.service

import android.app.IActivityTaskManager
import android.content.Intent
import android.graphics.Rect
import android.os.IBinder
import android.os.ServiceManager
import android.util.Log
import android.view.InputEvent
import android.view.MotionEvent
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Shizuku User Service implementation that runs in a separate process with
 * shell (UID 2000) identity. All privileged system API calls happen here.
 *
 * This service accesses the hidden IActivityTaskManager interface through
 * ShizukuBinderWrapper to launch apps in freeform mode, resize/move tasks,
 * and manage windowing modes.
 *
 * Lifecycle:
 * - Bound via Shizuku.bindUserService() from the app process
 * - Runs until explicitly unbound or replaced (destroy() is called)
 * - No hidden API restrictions apply in this process context
 */
class CanvasWindowService : ICanvasWindowService.Stub() {

    companion object {
        private const val TAG = "CanvasWindowService"

        // Windowing mode constants from android.app.WindowConfiguration
        const val WINDOWING_MODE_UNDEFINED = 0
        const val WINDOWING_MODE_FULLSCREEN = 1
        const val WINDOWING_MODE_FREEFORM = 5

        // System service name for ActivityTaskManager
        private const val SERVICE_ACTIVITY_TASK = "activity_task"
    }

    /**
     * Lazily obtain the IActivityTaskManager proxy via ShizukuBinderWrapper.
     * This wraps the raw system service binder so that all calls are made
     * with shell identity (UID 2000).
     */
    private val activityTaskManager: IActivityTaskManager by lazy {
        try {
            val rawBinder = ServiceManager.getService(SERVICE_ACTIVITY_TASK)
            // We are already in the Shizuku User Service (UID 2000 context),
            // so we don't need ShizukuBinderWrapper to proxy IPC calls.
            IActivityTaskManager.Stub.asInterface(rawBinder)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to obtain IActivityTaskManager", e)
            throw RuntimeException("Cannot connect to ActivityTaskManager service", e)
        }
    }

    // -------------------------------------------------------------------------
    // ICanvasWindowService implementation
    // -------------------------------------------------------------------------

    private val virtualDisplays = mutableMapOf<Int, android.hardware.display.VirtualDisplay>()

    private val systemContext: android.content.Context by lazy {
        val activityThreadClass = Class.forName("android.app.ActivityThread")
        var activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null)
        if (activityThread == null) {
            activityThread = activityThreadClass.getMethod("systemMain").invoke(null)
        }
        activityThreadClass.getMethod("getSystemContext").invoke(activityThread) as android.content.Context
    }

    private val displayManager: android.hardware.display.DisplayManager by lazy {
        val shellContext = systemContext.createPackageContext("com.android.shell", 0)
        shellContext.getSystemService(android.content.Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
    }

    override fun launchInFreeform(intent: Intent, bounds: Rect, userId: Int): Int {
        return try {
            Log.d(TAG, "launchInFreeform: ${intent.component ?: intent.`package`}, bounds=$bounds, userId=$userId")

            // Configure ActivityOptions for freeform windowing
            val options = android.app.ActivityOptions.makeBasic()

            // Set freeform windowing mode via reflection (hidden API)
            setLaunchWindowingMode(options, WINDOWING_MODE_FREEFORM)
            setLaunchBounds(options, bounds)

            // Ensure it launches as a new task
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

            // Call IActivityTaskManager.startActivityAsUser
            val result = activityTaskManager.startActivityAsUser(
                null,                    // caller (IApplicationThread) — null for shell
                "com.android.shell",     // callingPackage — shell identity
                null,                    // callingFeatureId
                intent,                  // the intent to launch
                intent.type,             // resolvedType
                null,                    // resultTo
                null,                    // resultWho
                0,                       // requestCode
                0,                       // flags
                null,                    // profilerInfo
                options.toBundle(),      // options bundle with freeform config
                userId                   // userId (0 for primary)
            )

            Log.d(TAG, "startActivityAsUser result: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch in freeform", e)
            -1
        }
    }

    override fun resizeTask(taskId: Int, newBounds: Rect) {
        try {
            Log.d(TAG, "resizeTask: taskId=$taskId, newBounds=$newBounds")

            // Use shell command as the most reliable cross-version approach
            // The am task-resize command works across Android 10-15
            val cmd = "am task resize $taskId ${newBounds.left} ${newBounds.top} ${newBounds.right} ${newBounds.bottom}"
            executeShellCommand(cmd)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resize task $taskId", e)
        }
    }

    override fun setTaskWindowingMode(taskId: Int, windowingMode: Int) {
        try {
            Log.d(TAG, "setTaskWindowingMode: taskId=$taskId, mode=$windowingMode")

            // Use the wm command to set windowing mode
            val cmd = "am task set-windowing-mode $taskId $windowingMode"
            executeShellCommand(cmd)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set windowing mode for task $taskId", e)
        }
    }

    override fun getTaskIdForPackage(packageName: String): Int {
        return try {
            Log.d(TAG, "getTaskIdForPackage: $packageName")

            // Use am stack list to find the task ID
            // Parse output: taskId=N ... A=<component>
            val output = executeShellCommand("am stack list")
            val lines = output.split("\n")

            for (line in lines) {
                if (line.contains(packageName)) {
                    val taskIdMatch = Regex("taskId=(\\d+)").find(line)
                    if (taskIdMatch != null) {
                        val taskId = taskIdMatch.groupValues[1].toIntOrNull() ?: -1
                        Log.d(TAG, "Found taskId=$taskId for $packageName")
                        return taskId
                    }
                }
            }

            // Fallback: try dumpsys activity recents
            val recentsOutput = executeShellCommand("dumpsys activity recents")
            val recentsLines = recentsOutput.split("\n")
            for (line in recentsLines) {
                if (line.contains(packageName) && line.contains("id=")) {
                    val idMatch = Regex("id=(\\d+)").find(line)
                    if (idMatch != null) {
                        return idMatch.groupValues[1].toIntOrNull() ?: -1
                    }
                }
            }

            Log.w(TAG, "No task found for $packageName")
            -1
        } catch (e: Exception) {
            Log.e(TAG, "Failed to find task for $packageName", e)
            -1
        }
    }

    override fun enableFreeformSupport() {
        try {
            Log.d(TAG, "Enabling freeform support")
            executeShellCommand("settings put global enable_freeform_support 1")

            // Also try to enable force_resizable_activities for better compatibility
            executeShellCommand("settings put global force_resizable_activities 1")

            Log.i(TAG, "Freeform support enabled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable freeform support", e)
        }
    }

    override fun isFreeformEnabled(): Boolean {
        return try {
            val result = executeShellCommand("settings get global enable_freeform_support").trim()
            Log.d(TAG, "Freeform support setting: '$result'")
            result == "1"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check freeform status", e)
            false
        }
    }

    override fun launchInDisplay(intent: Intent, displayId: Int, userId: Int): Int {
        return try {
            Log.d(TAG, "launchInDisplay: ${intent.component ?: intent.`package`}, displayId=$displayId, userId=$userId")

            val options = android.app.ActivityOptions.makeBasic()
            options.setLaunchDisplayId(displayId)
            
            // We launch in fullscreen mode since each display is dedicated to one app
            setLaunchWindowingMode(options, WINDOWING_MODE_FULLSCREEN)

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

            val result = activityTaskManager.startActivityAsUser(
                null, "com.android.shell", null, intent, intent.type, null, null, 0, 0, null, options.toBundle(), userId
            )

            Log.d(TAG, "startActivityAsUser result: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch in virtual display", e)
            -1
        }
    }

    override fun injectMotionEvent(event: MotionEvent, displayId: Int) {
        try {
            try {
                val setDisplayIdMethod = MotionEvent::class.java.getMethod("setDisplayId", Int::class.javaPrimitiveType)
                setDisplayIdMethod.invoke(event, displayId)
            } catch (e: Exception) {
                Log.e(TAG, "Could not set displayId on MotionEvent via reflection", e)
            }
            val inputManager = Class.forName("android.hardware.input.InputManager")
                .getMethod("getInstance").invoke(null)
            
            val injectMethod = inputManager.javaClass.getMethod(
                "injectInputEvent", 
                InputEvent::class.java, 
                Int::class.javaPrimitiveType
            )
            
            // 0 = INJECT_INPUT_EVENT_MODE_ASYNC
            injectMethod.invoke(inputManager, event, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inject motion event", e)
        }
    }

    override fun createVirtualDisplay(
        slotIndex: Int,
        width: Int,
        height: Int,
        densityDpi: Int,
        surface: android.view.Surface
    ): Int {
        try {
            Log.d(TAG, "createVirtualDisplay in shell process: slot=$slotIndex, w=$width, h=$height, dpi=$densityDpi")
            
            // 1: PUBLIC, 2: PRESENTATION, 8: OWN_CONTENT_ONLY, 64: SUPPORTS_TOUCH, 1024: TRUSTED
            val flags = 1 or 2 or 8 or 64 or 1024
            
            val virtualDisplay = displayManager.createVirtualDisplay(
                "Canvas_VirtualDisplay_$slotIndex",
                width,
                height,
                densityDpi,
                surface,
                flags
            )
            
            if (virtualDisplay != null) {
                virtualDisplays[slotIndex] = virtualDisplay
                val displayId = virtualDisplay.display.displayId
                Log.d(TAG, "Successfully created shell virtual display with id=$displayId")
                return displayId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create shell virtual display", e)
        }
        return -1
    }

    override fun releaseVirtualDisplay(slotIndex: Int) {
        try {
            Log.d(TAG, "releaseVirtualDisplay: slot=$slotIndex")
            virtualDisplays[slotIndex]?.release()
            virtualDisplays.remove(slotIndex)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release shell virtual display", e)
        }
    }

    override fun destroy() {
        Log.i(TAG, "CanvasWindowService destroyed")
        System.exit(0)
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Set the launch windowing mode on ActivityOptions via reflection.
     * The setLaunchWindowingMode method is hidden but available on all
     * Android versions that support freeform (Android 7+).
     */
    private fun setLaunchWindowingMode(options: android.app.ActivityOptions, mode: Int) {
        try {
            val method = android.app.ActivityOptions::class.java
                .getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
            method.invoke(options, mode)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set launch windowing mode via reflection", e)
            // Fallback: try setting through Bundle directly
            try {
                val bundle = options.toBundle()
                bundle.putInt("android.activity.windowingMode", mode)
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback also failed", e2)
            }
        }
    }

    /**
     * Set the launch bounds on ActivityOptions.
     * This is a public API since Android 7 (API 24).
     */
    private fun setLaunchBounds(options: android.app.ActivityOptions, bounds: Rect) {
        try {
            val method = android.app.ActivityOptions::class.java
                .getMethod("setLaunchBounds", Rect::class.java)
            method.invoke(options, bounds)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set launch bounds", e)
        }
    }

    /**
     * Execute a shell command and return the output.
     * In the Shizuku user service context, we already have shell (UID 2000) identity,
     * so Runtime.exec() runs with elevated privileges.
     */
    private fun executeShellCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            var errorLine: String?
            while (errorReader.readLine().also { errorLine = it } != null) {
                Log.w(TAG, "Shell stderr: $errorLine")
            }
            process.waitFor()
            output.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Shell command failed: $command", e)
            ""
        }
    }
}
