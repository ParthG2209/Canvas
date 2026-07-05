package dev.canvas.multitask.ui.workspace

import android.annotation.SuppressLint
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import dev.canvas.multitask.domain.WindowLayout
import kotlin.math.roundToInt

@Composable
fun CanvasWorkspaceScreen(
    viewModel: WorkspaceViewModel = hiltViewModel()
) {
    val layout by viewModel.layout.collectAsState()
    val packages by viewModel.packages.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current

    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, _, _ ->
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Determine virtual display size
        val slotWidth = (screenWidth * 0.8f).roundToInt()
        val slotHeight = (screenHeight * 0.8f).roundToInt()
        
        // Define positions based on layout
        val positions = when (layout) {
            is WindowLayout.ThreeColumns -> listOf(
                IntOffset(0, 0),
                IntOffset(slotWidth, 0),
                IntOffset(slotWidth * 2, 0)
            )
            is WindowLayout.TwoOneStack -> listOf(
                IntOffset(0, 0),
                IntOffset(slotWidth, 0),
                IntOffset(slotWidth / 2, slotHeight)
            )
            is WindowLayout.SideBySide -> listOf(
                IntOffset(0, 0),
                IntOffset(slotWidth, 0)
            )
            else -> listOf(IntOffset(0, 0))
        }

        positions.forEachIndexed { index, position ->
            if (index < packages.size) {
                Box(
                    modifier = Modifier
                        .offset { 
                            IntOffset(
                                x = (position.x + offsetX).roundToInt(),
                                y = (position.y + offsetY).roundToInt()
                            )
                        }
                        .size(
                            width = with(density) { slotWidth.toDp() },
                            height = with(density) { slotHeight.toDp() }
                        )
                ) {
                    VirtualDisplayView(
                        slotIndex = index,
                        width = slotWidth,
                        height = slotHeight,
                        densityDpi = displayMetrics.densityDpi,
                        onSurfaceCreated = { surface ->
                            viewModel.onSurfaceCreated(index, slotWidth, slotHeight, displayMetrics.densityDpi, surface)
                        },
                        onSurfaceDestroyed = {
                            viewModel.onSurfaceDestroyed(index)
                        },
                        onTouchEvent = { event ->
                            viewModel.injectTouchEvent(index, event)
                        }
                    )
                }
            }
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun VirtualDisplayView(
    slotIndex: Int,
    width: Int,
    height: Int,
    densityDpi: Int,
    onSurfaceCreated: (Surface) -> Unit,
    onSurfaceDestroyed: () -> Unit,
    onTouchEvent: (MotionEvent) -> Unit
) {
    AndroidView(
        factory = { context ->
            SurfaceView(context).apply {
                holder.setFixedSize(width, height)
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        onSurfaceCreated(holder.surface)
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {}

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        onSurfaceDestroyed()
                    }
                })

                setOnTouchListener { _, event ->
                    // Make a copy of the event because the system might recycle it
                    val eventCopy = MotionEvent.obtain(event)
                    onTouchEvent(eventCopy)
                    
                    // We return false here so we don't consume the touch event 
                    // completely if it's meant for Compose panning?
                    // Actually, if we return true, we consume it and Compose won't get it.
                    // If we return false, Compose gets it, but the virtual display might not get follow-up events.
                    // To have both panning AND app interaction, we'd need complex touch routing.
                    // Let's just return true for now to allow app interaction.
                    true
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
