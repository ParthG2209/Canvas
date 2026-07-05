package dev.canvas.multitask.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.canvas.multitask.data.prefs.PreferencesManager
import dev.canvas.multitask.data.shizuku.ShizukuConnectionState
import dev.canvas.multitask.data.shizuku.ShizukuManager
import dev.canvas.multitask.ui.theme.CanvasSecondary
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val shizukuManager: ShizukuManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    val connectionState: StateFlow<ShizukuConnectionState> = shizukuManager.connectionState
    val isFreeformEnabled = preferencesManager.isFreeformEnabled

    fun refreshConnection() {
        shizukuManager.refreshConnectionState()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onReOnboard: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val isFreeformEnabled by viewModel.isFreeformEnabled.collectAsStateWithLifecycle(initialValue = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Shizuku Status
            item {
                SettingsSection(title = "Connection") {
                    SettingsItem(
                        icon = Icons.Filled.Link,
                        title = "Shizuku Status",
                        subtitle = when (connectionState) {
                            is ShizukuConnectionState.Connected -> "Connected"
                            is ShizukuConnectionState.NotInstalled -> "Not installed"
                            is ShizukuConnectionState.NotRunning -> "Service not running"
                            is ShizukuConnectionState.PermissionNeeded -> "Permission needed"
                            is ShizukuConnectionState.Error -> "Error"
                        },
                        trailing = {
                            Icon(
                                imageVector = if (connectionState is ShizukuConnectionState.Connected)
                                    Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                contentDescription = null,
                                tint = if (connectionState is ShizukuConnectionState.Connected)
                                    CanvasSecondary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )

                    SettingsItem(
                        icon = Icons.Filled.Widgets,
                        title = "Freeform Windows",
                        subtitle = if (isFreeformEnabled) "Enabled" else "Disabled"
                    )
                }
            }

            // Actions
            item {
                SettingsSection(title = "Actions") {
                    SettingsItem(
                        icon = Icons.Filled.Replay,
                        title = "Re-run Setup",
                        subtitle = "Go through the Shizuku setup again",
                        onClick = onReOnboard
                    )

                    SettingsItem(
                        icon = Icons.Filled.Refresh,
                        title = "Refresh Connection",
                        subtitle = "Re-check Shizuku service status",
                        onClick = { viewModel.refreshConnection() }
                    )
                }
            }

            // About
            item {
                SettingsSection(title = "About") {
                    SettingsItem(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        title = "Help & FAQ",
                        subtitle = "What is Shizuku? How does Canvas work?"
                    )

                    SettingsItem(
                        icon = Icons.Filled.Info,
                        title = "About Canvas",
                        subtitle = "Version 0.1.0"
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth()
    }

    Surface(
        modifier = modifier,
        onClick = onClick ?: {},
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailing()
            }
        }
    }
}
