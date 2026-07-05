package dev.canvas.multitask.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for querying installed/launchable apps from the system PackageManager.
 * Results are cached in memory for fast subsequent access.
 */
@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var cachedApps: List<AppInfo>? = null

    /**
     * Get all launchable apps (excluding this app itself).
     * Results are cached after the first query.
     */
    suspend fun getLaunchableApps(): List<AppInfo> {
        return cachedApps ?: withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfoList: List<ResolveInfo> = pm.queryIntentActivities(mainIntent, 0)

            val apps = resolveInfoList
                .filter { it.activityInfo.packageName != context.packageName }
                .map { resolveInfo ->
                    AppInfo(
                        packageName = resolveInfo.activityInfo.packageName,
                        label = resolveInfo.loadLabel(pm).toString(),
                        icon = resolveInfo.loadIcon(pm)
                    )
                }
                .sortedBy { it.label.lowercase() }
                .distinctBy { it.packageName }

            cachedApps = apps
            apps
        }
    }

    /**
     * Search apps by name.
     */
    suspend fun searchApps(query: String): List<AppInfo> {
        val allApps = getLaunchableApps()
        if (query.isBlank()) return allApps
        return allApps.filter {
            it.label.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
        }
    }

    /**
     * Get the launch intent for a specific package.
     */
    fun getLaunchIntent(packageName: String): Intent? {
        return context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
    }

    /**
     * Clear the cache to force a refresh.
     */
    fun clearCache() {
        cachedApps = null
    }
}
