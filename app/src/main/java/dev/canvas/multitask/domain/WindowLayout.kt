package dev.canvas.multitask.domain

import android.graphics.Rect

/**
 * Layout configurations for multi-window sessions.
 */
sealed class WindowLayout {
    /** Two apps side-by-side (50/50 horizontal split) */
    data object SideBySide : WindowLayout()

    /** Three apps: left column split vertically (2 apps), right column full height (1 app) */
    data object TwoOneStack : WindowLayout()

    /** Three apps: three columns side-by-side (1+1+1) */
    data object ThreeColumns : WindowLayout()

    /** Single app in near-fullscreen freeform */
    data object Single : WindowLayout()
}

/**
 * Represents one app's window slot in a Canvas session.
 */
data class WindowSlot(
    val packageName: String,
    val bounds: Rect,
    val isParked: Boolean = false,
    val taskId: Int = -1
)

/**
 * An active Canvas multitasking session.
 */
data class CanvasSession(
    val slots: List<WindowSlot>,
    val layout: WindowLayout,
    val activeSlotIndex: Int = 0
) {
    val activeSlot: WindowSlot get() = slots[activeSlotIndex]
}
