package dev.canvas.multitask.ui.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.canvas.multitask.data.apps.AppInfo
import dev.canvas.multitask.data.apps.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppPickerUiState(
    val apps: List<AppInfo> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showLayoutChooser: Boolean = false
) {
    val canLaunch: Boolean get() = selectedPackages.size in 2..3
    val selectedCount: Int get() = selectedPackages.size
}

@HiltViewModel
class AppPickerViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val canvasWindowManager: dev.canvas.multitask.data.window.CanvasWindowManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppPickerUiState())
    val uiState: StateFlow<AppPickerUiState> = _uiState.asStateFlow()

    private val _sessionLaunched = MutableSharedFlow<Unit>()
    val sessionLaunched = _sessionLaunched.asSharedFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val apps = appRepository.getLaunchableApps()
                _uiState.update { it.copy(apps = apps, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to load apps: ${e.message}")
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            val results = appRepository.searchApps(query)
            _uiState.update { it.copy(apps = results) }
        }
    }

    fun onAppToggled(packageName: String) {
        _uiState.update { state ->
            val newSelection = if (packageName in state.selectedPackages) {
                state.selectedPackages - packageName
            } else {
                if (state.selectedPackages.size >= 3) {
                    // Max 3 apps — don't add more
                    return@update state.copy(errorMessage = "Maximum 3 apps can be selected")
                }
                state.selectedPackages + packageName
            }
            state.copy(selectedPackages = newSelection, errorMessage = null)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun launchSelectedApps() {
        val pkgs = _uiState.value.selectedPackages.toList()
        if (pkgs.size !in 2..3) return
        
        if (pkgs.size == 3) {
            // Ask user for layout
            _uiState.update { it.copy(showLayoutChooser = true) }
            return
        }
        
        launchSelectedAppsWithLayout(null)
    }
    
    fun hideLayoutChooser() {
        _uiState.update { it.copy(showLayoutChooser = false) }
    }

    fun launchSelectedAppsWithLayout(layout: dev.canvas.multitask.domain.WindowLayout?) {
        val pkgs = _uiState.value.selectedPackages.toList()
        if (pkgs.size !in 2..3) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showLayoutChooser = false) }
            val success = canvasWindowManager.prepareSession(pkgs, layout)
            _uiState.update { it.copy(isLoading = false) }
            
            if (success) {
                _sessionLaunched.emit(Unit)
            } else {
                _uiState.update { it.copy(errorMessage = "Failed to launch apps. Ensure Shizuku is running and apps are valid.") }
            }
        }
    }
}
