package dev.canvas.multitask.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.canvas.multitask.data.prefs.PreferencesManager
import dev.canvas.multitask.data.shizuku.ShizukuConnectionState
import dev.canvas.multitask.data.shizuku.ShizukuManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Onboarding step in the setup wizard.
 */
enum class OnboardingStep {
    INSTALL_SHIZUKU,
    START_SERVICE,
    GRANT_PERMISSION,
    ENABLE_FREEFORM,
    COMPLETE
}

/**
 * UI state for the onboarding screen.
 */
data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.INSTALL_SHIZUKU,
    val connectionState: ShizukuConnectionState = ShizukuConnectionState.NotInstalled,
    val isFreeformEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel for the onboarding flow.
 * Observes Shizuku connection state and drives the step-by-step wizard.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val shizukuManager: ShizukuManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        // Observe Shizuku connection state and update the current step
        viewModelScope.launch {
            shizukuManager.connectionState.collect { state ->
                _uiState.update { current ->
                    val newStep = determineStep(state, current.isFreeformEnabled)
                    current.copy(
                        connectionState = state,
                        currentStep = newStep,
                        errorMessage = if (state is ShizukuConnectionState.Error) state.message else null
                    )
                }
            }
        }
    }

    /**
     * Called when user taps the action button for the current step.
     */
    fun onStepAction() {
        when (_uiState.value.currentStep) {
            OnboardingStep.INSTALL_SHIZUKU -> {
                // Deep link handled by UI
            }
            OnboardingStep.START_SERVICE -> {
                // Guide handled by UI; refresh after user returns
                shizukuManager.refreshConnectionState()
            }
            OnboardingStep.GRANT_PERMISSION -> {
                shizukuManager.requestPermission()
            }
            OnboardingStep.ENABLE_FREEFORM -> {
                enableFreeform()
            }
            OnboardingStep.COMPLETE -> {
                completeOnboarding()
            }
        }
    }

    /**
     * Refresh the connection state (e.g., after returning from Shizuku app).
     */
    fun refreshState() {
        shizukuManager.refreshConnectionState()
    }

    /**
     * Enable freeform support via the Shizuku User Service.
     */
    private fun enableFreeform() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val service = shizukuManager.windowService
                if (service != null) {
                    service.enableFreeformSupport()
                    val isEnabled = service.isFreeformEnabled()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isFreeformEnabled = isEnabled,
                            currentStep = if (isEnabled) OnboardingStep.COMPLETE else it.currentStep,
                            errorMessage = if (!isEnabled) "Failed to enable freeform support" else null
                        )
                    }
                    if (isEnabled) {
                        preferencesManager.setFreeformEnabled(true)
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Window service not connected"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Mark onboarding as complete and persist the state.
     */
    private fun completeOnboarding() {
        viewModelScope.launch {
            preferencesManager.setOnboardingComplete(true)
        }
    }

    /**
     * Determine which onboarding step we should be on based on the connection state.
     */
    private fun determineStep(
        state: ShizukuConnectionState,
        isFreeformEnabled: Boolean
    ): OnboardingStep {
        return when (state) {
            is ShizukuConnectionState.NotInstalled -> OnboardingStep.INSTALL_SHIZUKU
            is ShizukuConnectionState.NotRunning -> OnboardingStep.START_SERVICE
            is ShizukuConnectionState.PermissionNeeded -> OnboardingStep.GRANT_PERMISSION
            is ShizukuConnectionState.Connected -> {
                if (isFreeformEnabled) OnboardingStep.COMPLETE
                else OnboardingStep.ENABLE_FREEFORM
            }
            is ShizukuConnectionState.Error -> OnboardingStep.INSTALL_SHIZUKU
        }
    }
}
