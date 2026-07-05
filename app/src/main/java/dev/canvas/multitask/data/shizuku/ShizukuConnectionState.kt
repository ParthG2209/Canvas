package dev.canvas.multitask.data.shizuku

/**
 * Represents the current state of the Shizuku connection lifecycle.
 * Drives the onboarding flow and runtime status displays.
 */
sealed class ShizukuConnectionState {
    /** Shizuku app is not installed on the device */
    data object NotInstalled : ShizukuConnectionState()

    /** Shizuku is installed but its service is not running */
    data object NotRunning : ShizukuConnectionState()

    /** Shizuku service is running but this app hasn't been granted permission */
    data object PermissionNeeded : ShizukuConnectionState()

    /** Fully connected and authorized — ready to use privileged APIs */
    data object Connected : ShizukuConnectionState()

    /** An error occurred during connection */
    data class Error(val message: String, val cause: Throwable? = null) : ShizukuConnectionState()
}
