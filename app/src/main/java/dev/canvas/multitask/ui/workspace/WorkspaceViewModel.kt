package dev.canvas.multitask.ui.workspace

import android.content.Intent
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.canvas.multitask.data.apps.AppRepository
import dev.canvas.multitask.data.shizuku.ShizukuManager
import dev.canvas.multitask.data.window.CanvasVirtualDisplayManager
import dev.canvas.multitask.data.window.CanvasWindowManager
import dev.canvas.multitask.domain.WindowLayout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkspaceViewModel @Inject constructor(
    private val windowManager: CanvasWindowManager,
    private val shizukuManager: ShizukuManager,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _layout = MutableStateFlow<WindowLayout>(WindowLayout.Single)
    val layout: StateFlow<WindowLayout> = _layout.asStateFlow()

    private val _packages = MutableStateFlow<List<String>>(emptyList())
    val packages: StateFlow<List<String>> = _packages.asStateFlow()

    private val slotDisplayIds = mutableMapOf<Int, Int>()

    init {
        _layout.value = windowManager.getPendingLayout()
        _packages.value = windowManager.getPendingPackages()
    }

    fun onSurfaceCreated(slotIndex: Int, width: Int, height: Int, densityDpi: Int, surface: Surface) {
        viewModelScope.launch {
            val service = shizukuManager.windowService
            if (service != null) {
                val displayId = service.createVirtualDisplay(slotIndex, width, height, densityDpi, surface)
                if (displayId != -1) {
                    slotDisplayIds[slotIndex] = displayId
                    launchAppInSlot(slotIndex, displayId)
                } else {
                    Log.e("WorkspaceViewModel", "Failed to create virtual display for slot $slotIndex")
                }
            } else {
                Log.e("WorkspaceViewModel", "Shizuku service not connected")
            }
        }
    }

    fun onSurfaceDestroyed(slotIndex: Int) {
        viewModelScope.launch {
            shizukuManager.windowService?.releaseVirtualDisplay(slotIndex)
            slotDisplayIds.remove(slotIndex)
        }
    }

    fun injectTouchEvent(slotIndex: Int, event: MotionEvent) {
        val displayId = slotDisplayIds[slotIndex] ?: return
        viewModelScope.launch {
            shizukuManager.windowService?.injectMotionEvent(event, displayId)
        }
    }

    private fun launchAppInSlot(slotIndex: Int, displayId: Int) {
        viewModelScope.launch {
            val pkg = _packages.value.getOrNull(slotIndex) ?: return@launch
            val intent = appRepository.getLaunchIntent(pkg) ?: return@launch
            
            val service = shizukuManager.windowService
            if (service != null) {
                // User ID 0 is usually the primary user
                val result = service.launchInDisplay(intent, displayId, 0)
                Log.d("WorkspaceViewModel", "Launched $pkg in display $displayId. Result: $result")
            } else {
                Log.e("WorkspaceViewModel", "Shizuku service not connected")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            val service = shizukuManager.windowService
            slotDisplayIds.keys.forEach { slotIndex ->
                service?.releaseVirtualDisplay(slotIndex)
            }
            slotDisplayIds.clear()
        }
    }
}
