package dev.canvas.multitask.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "canvas_prefs")

/**
 * DataStore-based preferences manager for Canvas.
 * Tracks onboarding completion and freeform support status.
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        private val KEY_FREEFORM_ENABLED = booleanPreferencesKey("freeform_enabled")
    }

    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_ONBOARDING_COMPLETE] ?: false }

    val isFreeformEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_FREEFORM_ENABLED] ?: false }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETE] = complete
        }
    }

    suspend fun setFreeformEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FREEFORM_ENABLED] = enabled
        }
    }
}
