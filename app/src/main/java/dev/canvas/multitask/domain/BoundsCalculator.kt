package dev.canvas.multitask.domain

import android.graphics.Rect

/**
 * Computes window bounds for each layout configuration given display dimensions.
 * Bounds can extend off-screen for the "park" effect.
 */
object BoundsCalculator {

    /**
     * Compute bounds for all slots in a session, given the current layout, display dimensions,
     * and which slot is currently "active" (focused).
     *
     * In Open Canvas multi-window, windows are larger than their naive divisions
     * (e.g., 2 apps take 85% width each, 3 apps take 70% width each).
     * The focused app is fully visible, and adjacent apps are pushed partially
     * off-screen, leaving a "sliver" (e.g., 15% width) that can be tapped.
     */
    fun computeSessionBounds(
        layout: WindowLayout,
        displayWidth: Int,
        displayHeight: Int,
        activeSlotIndex: Int
    ): List<Rect> {
        return when (layout) {
            is WindowLayout.Single -> {
                listOf(fullscreen(displayWidth, displayHeight))
            }
            is WindowLayout.SideBySide -> {
                val f = (displayWidth * 0.85).toInt()
                val s = (displayWidth * 0.15).toInt()
                
                if (activeSlotIndex == 0) {
                    // App 0 active: App 0 [0, F], App 1 [F, 2F] (sliver is S on right)
                    listOf(
                        Rect(0, 0, f, displayHeight),
                        Rect(f, 0, f * 2, displayHeight)
                    )
                } else {
                    // App 1 active: App 0 [-F+S, S] (sliver is S on left), App 1 [S, S+F]
                    listOf(
                        Rect(-f + s, 0, s, displayHeight),
                        Rect(s, 0, s + f, displayHeight)
                    )
                }
            }
            is WindowLayout.TwoOneStack -> {
                val f = (displayWidth * 0.85).toInt()
                val s = (displayWidth * 0.15).toInt()
                val halfH = displayHeight / 2
                
                if (activeSlotIndex == 0 || activeSlotIndex == 1) {
                    // Left column active
                    listOf(
                        Rect(0, 0, f, halfH),                  // top-left
                        Rect(0, halfH, f, displayHeight),      // bottom-left
                        Rect(f, 0, f * 2, displayHeight)       // right full (sliver S)
                    )
                } else {
                    // Right column active
                    listOf(
                        Rect(-f + s, 0, s, halfH),             // top-left (sliver)
                        Rect(-f + s, halfH, s, displayHeight), // bottom-left (sliver)
                        Rect(s, 0, s + f, displayHeight)       // right full
                    )
                }
            }
            is WindowLayout.ThreeColumns -> {
                val f = (displayWidth * 0.70).toInt()
                val s = (displayWidth * 0.15).toInt()
                
                when (activeSlotIndex) {
                    0 -> {
                        // App 0 active (Left aligned)
                        listOf(
                            Rect(0, 0, f, displayHeight),
                            Rect(f, 0, f * 2, displayHeight),
                            Rect(f * 2, 0, f * 3, displayHeight) // Off screen
                        )
                    }
                    1 -> {
                        // App 1 active (Centered)
                        listOf(
                            Rect(-f + s, 0, s, displayHeight),
                            Rect(s, 0, s + f, displayHeight),
                            Rect(s + f, 0, s + f * 2, displayHeight)
                        )
                    }
                    else -> {
                        // App 2 active (Right aligned)
                        listOf(
                            Rect(-f * 2 + s * 2, 0, s * 2, displayHeight), // Off screen (Wait, left edge of app 1 is S)
                            // Let's re-verify logic:
                            // App 2 at [W-F, W] => [0.3W, 1.0W]
                            // App 1 at [0.3W - F, 0.3W] => [-0.4W, 0.3W]
                            // App 0 at [0.3W - 2F, 0.3W - F] => [-1.1W, -0.4W]
                            Rect((0.3 * displayWidth).toInt() - f * 2, 0, (0.3 * displayWidth).toInt() - f, displayHeight),
                            Rect((0.3 * displayWidth).toInt() - f, 0, (0.3 * displayWidth).toInt(), displayHeight),
                            Rect((0.3 * displayWidth).toInt(), 0, (0.3 * displayWidth).toInt() + f, displayHeight)
                        )
                    }
                }
            }
        }
    }

    /**
     * Near-fullscreen bounds (small margin for the dock).
     */
    fun fullscreen(displayWidth: Int, displayHeight: Int, dockHeight: Int = 0): Rect {
        return Rect(0, 0, displayWidth, displayHeight - dockHeight)
    }

    /**
     * Parked off-screen to the right.
     */
    fun parkedRight(displayWidth: Int, displayHeight: Int): Rect {
        return Rect(displayWidth, 0, displayWidth * 2, displayHeight)
    }

    /**
     * Parked off-screen to the left.
     */
    fun parkedLeft(displayWidth: Int, displayHeight: Int): Rect {
        return Rect(-displayWidth, 0, 0, displayHeight)
    }

    /**
     * Linearly interpolate between two Rects.
     */
    fun lerp(start: Rect, end: Rect, fraction: Float): Rect {
        return Rect(
            lerpInt(start.left, end.left, fraction),
            lerpInt(start.top, end.top, fraction),
            lerpInt(start.right, end.right, fraction),
            lerpInt(start.bottom, end.bottom, fraction)
        )
    }

    private fun lerpInt(start: Int, end: Int, fraction: Float): Int {
        return (start + (end - start) * fraction).toInt()
    }
}
