package dev.canvas.multitask.data.apps

import android.graphics.drawable.Drawable

/**
 * Represents an installed app available for selection in the app picker.
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable
)
