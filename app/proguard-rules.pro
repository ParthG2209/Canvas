# Canvas ProGuard rules

# Shizuku — keep the provider and API classes
-keep class rikka.shizuku.** { *; }
-keep class dev.canvas.multitask.service.** { *; }

# Hidden API Bypass
-keep class org.lsposed.hiddenapibypass.** { *; }

# AIDL generated classes
-keep class dev.canvas.multitask.service.ICanvasWindowService { *; }
-keep class dev.canvas.multitask.service.ICanvasWindowService$* { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }

# Hilt
-keep class dagger.hilt.** { *; }

# Keep reflection targets used by CanvasWindowService
-keep class android.app.ActivityOptions { *; }
-keep class android.app.IActivityTaskManager { *; }
-keep class android.app.IActivityTaskManager$Stub { *; }
