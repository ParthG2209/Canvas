package android.os;

/**
 * Stub for the hidden ServiceManager class.
 * Used to obtain raw IBinder references to system services like "activity_task".
 * At runtime, reflection or Shizuku's SystemServiceHelper is used instead.
 *
 * This file is compile-only and is NOT shipped in the APK.
 */
public class ServiceManager {
    public static IBinder getService(String name) {
        throw new UnsupportedOperationException("Stub!");
    }
}
