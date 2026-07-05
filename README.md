# Canvas: Open Canvas Multitasking for Android

Canvas is a revolutionary multitasking environment for Android devices, deeply inspired by the "Open Canvas" feature introduced on modern foldables like the OnePlus Open. It brings desktop-class window management to your Android tablet or phone, allowing you to run multiple apps simultaneously in a continuous, pannable workspace.

By default, Android's multi-window capabilities are highly restrictive: apps are paused when out of focus, window sizes are rigidly constrained, and the OS stubbornly resists letting you have more than two apps on screen at once. Canvas shatters these limitations.

## Table of Contents
- [Features](#features)
- [How It Works (Under the Hood)](#how-it-works-under-the-hood)
- [Architecture Deep Dive](#architecture-deep-dive)
  - [The Virtual Display Engine](#the-virtual-display-engine)
  - [Shizuku Integration](#shizuku-integration)
  - [Input Injection](#input-injection)
- [Project Structure](#project-structure)
- [Technical Stack](#technical-stack)
- [Getting Started](#getting-started)
- [Known Limitations & Quirks](#known-limitations--quirks)
- [License](#license)

## Features

- **True Open Canvas Experience**: Run up to 3 applications simultaneously. They do not pause, freeze, or get killed by the system when you interact with another app.
- **Infinite Workspace**: Your physical screen is just a viewport into a larger digital desk. You can freely pan horizontally and vertically to reveal apps waiting just off-screen.
- **Dynamic Grid Layouts**: 
  - **1+1+1 (Three Columns)**: Apps are laid out in a straight horizontal line.
  - **2+1 (Stacked)**: Two apps side-by-side, with a third app positioned below them.
- **No Root Required**: Instead of requiring Magisk or KernelSU, Canvas uses **Shizuku** to temporarily elevate its permissions via ADB/Wireless Debugging.
- **Native App Rendering**: Canvas isn't just streaming a static image of the app; it natively intercepts and injects `MotionEvents` so apps remain as responsive and interactive as if they were running normally.
- **Seamless Modern UI**: Built entirely with Jetpack Compose and Material 3 design paradigms.

## How It Works (Under the Hood)

At its core, Canvas works by tricking Android into thinking that you have plugged in multiple external monitors, launching apps onto those monitors, and then projecting the video output of those monitors onto a 2D canvas in the main app.

### 1. The Virtual Display Trick
Android has a concept called `VirtualDisplay`, which is typically used for Screen Recording or Chromecast. If an app tries to launch another app onto a Virtual Display, Android enforces severe security restrictions. Most notably, Android will refuse to launch non-resizeable apps onto a Virtual Display unless that display is marked as **TRUSTED** (`VIRTUAL_DISPLAY_FLAG_TRUSTED`).

### 2. Privilege Escalation via Shizuku
To create a "Trusted" Virtual Display, an app must have the `ADD_TRUSTED_DISPLAY` permission, which is strictly reserved for System Apps. This is where **Shizuku** comes in. Shizuku allows apps to run a background service under the `com.android.shell` (UID 2000) context. 

Canvas spawns a Shizuku background service (`CanvasWindowService`). This service uses a specialized system context to interact with the `DisplayManager` and create Virtual Displays with the `TRUSTED` flag.

### 3. Rendering the Workspace
When the `CanvasWindowService` creates a Virtual Display, it requires a `Surface` to draw to. 
- In our main app, Jetpack Compose creates several `SurfaceView` instances (one for each app slot in the grid).
- The memory pointers to these `Surfaces` are serialized and sent over IPC (Inter-Process Communication via Binder) to the Shizuku Service.
- The Virtual Displays draw directly onto these Compose `SurfaceViews`.

### 4. Input Routing
Because the apps are technically running on a separate "monitor", tapping on the Compose `SurfaceView` would normally do nothing. Canvas intercepts every touch event on the `SurfaceView`, adjusts the X/Y coordinates to match the virtual display's resolution, and sends the touch event over IPC to the Shizuku Service. The Shizuku Service then uses hidden Android APIs (`InputManager.injectInputEvent`) to inject the touch event directly into the OS at a hardware level, allowing you to interact with the app.

## Architecture Deep Dive

### The Virtual Display Engine
The core logic for managing displays is handled entirely outside of the main app sandbox.
- **Creation**: `CanvasWindowService.createVirtualDisplay()` uses a spoofed package context (`com.android.shell`) to bypass `SecurityException` checks when calling `DisplayManager`.
- **Flags**: Displays are instantiated with `VIRTUAL_DISPLAY_FLAG_PUBLIC | VIRTUAL_DISPLAY_FLAG_PRESENTATION | VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | VIRTUAL_DISPLAY_FLAG_SUPPORTS_TOUCH | VIRTUAL_DISPLAY_FLAG_TRUSTED`.
- **Launching Apps**: Apps are launched onto these displays using the hidden `ActivityOptions.setLaunchDisplayId()` API, combined with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_MULTIPLE_TASK` to force a new instance.

### Shizuku Integration
Canvas utilizes a custom AIDL (`ICanvasWindowService.aidl`) to define the contract between the Main App and the Shell Service.
- The `ShizukuProvider` initializes the connection.
- `WorkspaceViewModel` acts as the orchestrator, binding the Compose UI states to the Shizuku service commands.

### Input Injection
Android's `InputManager` is a hidden system API. Canvas uses Java Reflection to access `InputManager.getInstance()` and its `injectInputEvent` method. Touch events injected this way carry the `INJECT_INPUT_EVENT_MODE_ASYNC` flag for zero-latency interactivity.

## Project Structure

```
Canvas/
├── app/                                 # Main Android Application
│   ├── src/main/java/dev/canvas/multitask/
│   │   ├── data/                        # Repositories, ShizukuManager, WindowManager
│   │   ├── domain/                      # Layout Models (WindowLayout.kt)
│   │   ├── ui/                          # Jetpack Compose UI (WorkspaceScreen, AppPicker)
│   │   └── CanvasApp.kt                 # Application Entry Point & Hilt setup
├── shizuku-service/                     # Elevated Privilege Module (UID 2000)
│   ├── src/main/aidl/.../service/       # IPC Contract (ICanvasWindowService.aidl)
│   └── src/main/java/.../service/       # CanvasWindowService.kt (Virtual Display & Injection logic)
├── hidden-api-stub/                     # Compilation stubs for @hide Android System APIs
└── build.gradle.kts                     # Root build configuration
```

## Technical Stack

- **Kotlin 1.9+**
- **Jetpack Compose** for all UI rendering and touch handling (`detectTransformGestures`).
- **Dagger Hilt** for Dependency Injection.
- **Shizuku API** for shell execution.
- **Kotlin Coroutines / StateFlow** for reactive UI updates.

## Getting Started

### Prerequisites
1. An Android device running **Android 14 or higher**.
2. **Shizuku** installed and activated. (You can activate it via Wireless Debugging without a PC).

### Building from Source
1. Clone this repository.
2. Open the project in Android Studio.
3. Because this app utilizes specialized testing features, debug builds must be installed with the `-t` flag via ADB:
   ```bash
   adb install -t -r app/build/outputs/apk/debug/app-debug.apk
   ```

### Usage Instructions
1. Open the Canvas app.
2. If prompted, grant Canvas permission to use Shizuku.
3. You will be presented with an App Picker. Select up to 3 apps.
4. Choose your desired layout (1+1+1 or 2+1).
5. Click **Launch Workspace**.
6. You can now use two fingers to pan the workspace around, or one finger to interact with the apps natively!

## Known Limitations & Quirks

- **DRM Content**: Video streaming apps that use hardware DRM (like Netflix or Hulu) may render as a black screen. This is a deliberate security feature of Android's Virtual Display API to prevent screen recording piracy.
- **System Dialogs**: Popups generated by the System UI (like Permission Dialogs or the IME Keyboard) might occasionally spawn on your physical display rather than the virtual display.
- **App Reloads**: Some poorly optimized apps may crash or reload when their host virtual display is resized or released.
- **Back Navigation**: Android's native back gesture routes to the host app. Implementing per-app back navigation inside the virtual display requires on-screen navigation buttons.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
