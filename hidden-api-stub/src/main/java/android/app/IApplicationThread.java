package android.app;

import android.os.IBinder;

/**
 * Stub for the hidden IApplicationThread interface.
 * Required as the first parameter of IActivityTaskManager.startActivityAsUser().
 * We always pass null for this parameter when calling from Shizuku context.
 *
 * This file is compile-only and is NOT shipped in the APK.
 */
public interface IApplicationThread extends android.os.IInterface {

    abstract class Stub extends android.os.Binder implements IApplicationThread {
        public static IApplicationThread asInterface(IBinder obj) {
            throw new UnsupportedOperationException("Stub!");
        }
    }
}
