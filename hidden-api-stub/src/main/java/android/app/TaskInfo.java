package android.app;

import android.os.IBinder;

/**
 * Stub for the hidden TaskInfo class.
 * Represents information about a running task returned by getTasks().
 *
 * This file is compile-only and is NOT shipped in the APK.
 */
public class TaskInfo {
    public int taskId;
    public android.content.ComponentName baseActivity;
    public android.content.ComponentName topActivity;
}
