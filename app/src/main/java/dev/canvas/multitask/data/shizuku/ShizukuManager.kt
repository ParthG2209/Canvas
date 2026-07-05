package dev.canvas.multitask.data.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.canvas.multitask.service.CanvasWindowService
import dev.canvas.multitask.service.ICanvasWindowService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the entire Shizuku lifecycle:
 * - Installation detection
 * - Service running status
 * - Permission request/check
 * - Binding the CanvasWindowService as a Shizuku User Service
 * - Emitting connection state changes as a Flow
 *
 * All privileged operations go through [windowService] once connected.
 */
@Singleton
class ShizukuManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "ShizukuManager"
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val REQUEST_CODE_PERMISSION = 1001
    }

    private val _connectionState = MutableStateFlow<ShizukuConnectionState>(
        ShizukuConnectionState.NotInstalled
    )
    val connectionState: StateFlow<ShizukuConnectionState> = _connectionState.asStateFlow()

    /** The bound User Service proxy, or null if not yet connected */
    private var _windowService: ICanvasWindowService? = null
    val windowService: ICanvasWindowService?
        get() = _windowService

    private var isBinderAlive = false

    // ─── Shizuku Listeners ──────────────────────────────────────────────────

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.i(TAG, "Shizuku binder received")
        isBinderAlive = true
        refreshConnectionState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.w(TAG, "Shizuku binder dead")
        isBinderAlive = false
        _windowService = null
        _connectionState.value = ShizukuConnectionState.NotRunning
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE_PERMISSION) {
                Log.i(TAG, "Permission result: ${grantResult == PackageManager.PERMISSION_GRANTED}")
                refreshConnectionState()
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    bindUserService()
                }
            }
        }

    private val userServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(TAG, "CanvasWindowService connected")
            _windowService = ICanvasWindowService.Stub.asInterface(service)
            _connectionState.value = ShizukuConnectionState.Connected
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "CanvasWindowService disconnected")
            _windowService = null
            refreshConnectionState()
        }
    }

    // ─── Public API ─────────────────────────────────────────────────────────

    /**
     * Initialize Shizuku listeners. Call once from the main activity's onCreate.
     */
    fun initialize() {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        refreshConnectionState()
    }

    /**
     * Clean up listeners. Call from the main activity's onDestroy.
     */
    fun cleanup() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        unbindUserService()
    }

    /**
     * Check if Shizuku is installed on the device.
     */
    fun isShizukuInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Check if the Shizuku service is currently running.
     */
    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if this app has been granted Shizuku permission.
     */
    fun hasPermission(): Boolean {
        return try {
            if (Shizuku.isPreV11()) return false
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Request Shizuku permission from the user.
     */
    fun requestPermission() {
        try {
            if (Shizuku.isPreV11()) {
                Log.w(TAG, "Pre-v11 Shizuku not supported")
                return
            }
            if (!Shizuku.shouldShowRequestPermissionRationale()) {
                Shizuku.requestPermission(REQUEST_CODE_PERMISSION)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request Shizuku permission", e)
            _connectionState.value = ShizukuConnectionState.Error(
                "Failed to request permission", e
            )
        }
    }

    /**
     * Bind the CanvasWindowService as a Shizuku User Service.
     * The service runs in a separate process with shell identity.
     */
    fun bindUserService() {
        try {
            val args = UserServiceArgs(
                ComponentName(
                    context.packageName,
                    CanvasWindowService::class.java.name
                )
            )
                .daemon(false)
                .processNameSuffix("canvas_window_service")
                .debuggable(true)
                .version(1)

            Shizuku.bindUserService(args, userServiceConnection)
            Log.i(TAG, "Binding CanvasWindowService...")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind user service", e)
            _connectionState.value = ShizukuConnectionState.Error(
                "Failed to bind window service", e
            )
        }
    }

    /**
     * Unbind the User Service.
     */
    fun unbindUserService() {
        try {
            Shizuku.unbindUserService(
                UserServiceArgs(
                    ComponentName(
                        context.packageName,
                        CanvasWindowService::class.java.name
                    )
                ),
                userServiceConnection,
                true
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error unbinding user service", e)
        }
        _windowService = null
    }

    /**
     * Re-evaluate the connection state based on current conditions.
     */
    fun refreshConnectionState() {
        val newState = when {
            !isShizukuInstalled() -> ShizukuConnectionState.NotInstalled
            !isShizukuRunning() && !isBinderAlive -> ShizukuConnectionState.NotRunning
            !hasPermission() -> ShizukuConnectionState.PermissionNeeded
            _windowService != null -> ShizukuConnectionState.Connected
            else -> {
                // Has permission but service not bound — bind it
                bindUserService()
                ShizukuConnectionState.PermissionNeeded // transitional
            }
        }
        _connectionState.value = newState
        Log.d(TAG, "Connection state: $newState")
    }
}
