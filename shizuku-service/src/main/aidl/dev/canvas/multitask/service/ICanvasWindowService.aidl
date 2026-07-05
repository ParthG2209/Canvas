// ICanvasWindowService.aidl
// AIDL contract between the Canvas app process and the Shizuku User Service.
// The User Service runs in a separate process with shell (UID 2000) identity,
// allowing it to call hidden system APIs like IActivityTaskManager.

package dev.canvas.multitask.service;

import android.content.Intent;
import android.graphics.Rect;

interface ICanvasWindowService {

    /**
     * Launch an app into a freeform window with specific bounds.
     *
     * @param intent  Launch intent for the target app
     * @param bounds  Window bounds (can be off-screen for "parked" state)
     * @param userId  Android user ID (typically 0 for primary user)
     * @return Activity start result code, or -1 on failure
     */
    int launchInFreeform(in Intent intent, in Rect bounds, int userId);

    /**
     * Resize/move an existing task to new bounds.
     * Used for park/recall animation (sliding windows on/off screen).
     *
     * @param taskId    The task ID to resize
     * @param newBounds New window bounds
     */
    void resizeTask(int taskId, in Rect newBounds);

    /**
     * Change the windowing mode of a task.
     * E.g., switch from freeform (5) to fullscreen (1) or vice versa.
     *
     * @param taskId        The task ID to modify
     * @param windowingMode WINDOWING_MODE_FREEFORM=5, WINDOWING_MODE_FULLSCREEN=1
     */
    void setTaskWindowingMode(int taskId, int windowingMode);

    /**
     * Find the task ID for a running app by its package name.
     * Searches the recent tasks stack for a matching base activity.
     *
     * @param packageName Package name to search for
     * @return Task ID if found, -1 otherwise
     */
    int getTaskIdForPackage(String packageName);

    /**
     * Enable freeform window support by writing the global system setting.
     * Equivalent to: settings put global enable_freeform_support 1
     */
    void enableFreeformSupport();

    /**
     * Check whether freeform support is currently enabled.
     *
     * @return true if the enable_freeform_support setting is "1"
     */
    boolean isFreeformEnabled();

    /**
     * Launch an intent in a specific Virtual Display.
     */
    int launchInDisplay(in Intent intent, int displayId, int userId);

    /**
     * Inject a MotionEvent to a specific display.
     */
    void injectMotionEvent(in android.view.MotionEvent event, int displayId);

    /**
     * Create a virtual display using shell privileges to ensure it is trusted.
     */
    int createVirtualDisplay(int slotIndex, int width, int height, int densityDpi, in android.view.Surface surface);

    /**
     * Release a virtual display.
     */
    void releaseVirtualDisplay(int slotIndex);

    /**
     * Shizuku User Service destroy callback.
     * Transaction code: 16777115 (AIDL: 16777114).
     * Called when the service is being replaced or explicitly unbound.
     */
    void destroy();
}
