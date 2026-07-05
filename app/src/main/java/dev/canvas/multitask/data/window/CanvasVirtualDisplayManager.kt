package dev.canvas.multitask.data.window

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.view.Surface
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class CanvasVirtualDisplayManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val virtualDisplays = mutableMapOf<Int, VirtualDisplay>()

    companion object {
        private const val TAG = "VirtualDisplayManager"
    }

    /**
     * Create or update a Virtual Display for a given slot.
     */
    fun createVirtualDisplay(
        slotIndex: Int,
        width: Int,
        height: Int,
        densityDpi: Int,
        surface: Surface
    ): VirtualDisplay {
        releaseVirtualDisplay(slotIndex)

        val flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or 
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION or 
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY

        val virtualDisplay = displayManager.createVirtualDisplay(
            "CanvasSlot_$slotIndex",
            width,
            height,
            densityDpi,
            surface,
            flags
        )
        
        virtualDisplays[slotIndex] = virtualDisplay
        Log.d(TAG, "Created VirtualDisplay for slot $slotIndex with displayId ${virtualDisplay.display.displayId}")
        return virtualDisplay
    }

    fun getVirtualDisplay(slotIndex: Int): VirtualDisplay? {
        return virtualDisplays[slotIndex]
    }

    fun releaseVirtualDisplay(slotIndex: Int) {
        virtualDisplays[slotIndex]?.release()
        virtualDisplays.remove(slotIndex)
        Log.d(TAG, "Released VirtualDisplay for slot $slotIndex")
    }

    fun releaseAll() {
        virtualDisplays.values.forEach { it.release() }
        virtualDisplays.clear()
        Log.d(TAG, "Released all VirtualDisplays")
    }
}
