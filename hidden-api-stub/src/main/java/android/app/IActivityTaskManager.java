package android.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

/**
 * Stub for the hidden IActivityTaskManager system service interface.
 * This interface is used to launch activities with specific windowing modes
 * (e.g., freeform) and manage task positioning.
 *
 * The actual method signatures are sourced from AOSP frameworks/base and are
 * stable across Android 13–15. Version-gated logic in CanvasWindowService
 * handles any differences in older/newer versions.
 *
 * This file is compile-only and is NOT shipped in the APK.
 */
public interface IActivityTaskManager {

    /**
     * Launch an activity as a specific user. The key method for freeform launching.
     * Signature is stable across Android 13 (API 33) through Android 15 (API 35).
     */
    int startActivityAsUser(
            IApplicationThread caller,
            String callingPackage,
            String callingFeatureId,
            Intent intent,
            String resolvedType,
            IBinder resultTo,
            String resultWho,
            int requestCode,
            int flags,
            ProfilerInfo profilerInfo,
            Bundle options,
            int userId
    );

    /**
     * Get the list of recent/running tasks.
     * Used to find taskId for a specific package after launch.
     */
    java.util.List<?> getTasks(int maxNum);

    /**
     * Remove a specific task by ID.
     */
    boolean removeTask(int taskId);

    abstract class Stub extends android.os.Binder implements IActivityTaskManager {
        public static IActivityTaskManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException("Stub!");
        }
    }
}
