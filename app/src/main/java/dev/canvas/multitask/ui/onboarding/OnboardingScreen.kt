package dev.canvas.multitask.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.canvas.multitask.R
import dev.canvas.multitask.ui.theme.CanvasPrimary
import dev.canvas.multitask.ui.theme.CanvasSecondary

/**
 * Full-screen onboarding wizard for Shizuku setup.
 * Guides the user through: Install Shizuku → Start Service → Grant Permission → Enable Freeform.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Auto-navigate when onboarding is complete
    LaunchedEffect(uiState.currentStep) {
        if (uiState.currentStep == OnboardingStep.COMPLETE) {
            viewModel.onStepAction() // persist completion
            onComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Title
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Step indicators
            StepIndicators(currentStep = uiState.currentStep)

            Spacer(modifier = Modifier.height(32.dp))

            // Current step card
            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = {
                    fadeIn() + slideInVertically { it / 4 } togetherWith
                            fadeOut() + slideOutVertically { -it / 4 }
                },
                label = "step_transition"
            ) { step ->
                StepCard(
                    step = step,
                    isLoading = uiState.isLoading,
                    errorMessage = uiState.errorMessage,
                    onAction = {
                        when (step) {
                            OnboardingStep.INSTALL_SHIZUKU -> {
                                // Open Shizuku on Play Store
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=moe.shizuku.privileged.api")
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback to browser
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")
                                        )
                                    )
                                }
                            }
                            OnboardingStep.START_SERVICE -> {
                                // Open Shizuku app
                                val intent = context.packageManager.getLaunchIntentForPackage(
                                    "moe.shizuku.privileged.api"
                                )
                                if (intent != null) {
                                    context.startActivity(intent)
                                }
                            }
                            else -> viewModel.onStepAction()
                        }
                    },
                    onRefresh = { viewModel.refreshState() }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Skip / Help text
            TextButton(
                onClick = { /* TODO: Show FAQ/help dialog */ }
            ) {
                Icon(
                    imageVector = Icons.Outlined.HelpOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("What is Shizuku?")
            }
        }
    }
}

@Composable
private fun StepIndicators(currentStep: OnboardingStep) {
    val steps = listOf(
        OnboardingStep.INSTALL_SHIZUKU,
        OnboardingStep.START_SERVICE,
        OnboardingStep.GRANT_PERMISSION,
        OnboardingStep.ENABLE_FREEFORM
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            val isActive = step == currentStep
            val isComplete = steps.indexOf(currentStep) > index

            Box(
                modifier = Modifier
                    .size(if (isActive) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isComplete -> CanvasSecondary
                            isActive -> CanvasPrimary
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        }
                    )
            )
        }
    }
}

@Composable
private fun StepCard(
    step: OnboardingStep,
    isLoading: Boolean,
    errorMessage: String?,
    onAction: () -> Unit,
    onRefresh: () -> Unit
) {
    val (icon, title, description, buttonText) = when (step) {
        OnboardingStep.INSTALL_SHIZUKU -> StepData(
            icon = Icons.Filled.GetApp,
            title = "Install Shizuku",
            description = "Canvas uses Shizuku for advanced window management.\nInstall it from the Google Play Store.",
            buttonText = "Install Shizuku"
        )
        OnboardingStep.START_SERVICE -> StepData(
            icon = Icons.Filled.PlayCircle,
            title = "Start Shizuku Service",
            description = "Open the Shizuku app and follow its setup instructions.\n\nFor most devices:\n1. Go to Settings → Developer Options\n2. Enable Wireless Debugging\n3. Return to Shizuku and tap \"Start\"",
            buttonText = "Open Shizuku"
        )
        OnboardingStep.GRANT_PERMISSION -> StepData(
            icon = Icons.Filled.Security,
            title = "Grant Permission",
            description = "Allow Canvas to use Shizuku for managing app windows.\nThis is required for the multitasking feature.",
            buttonText = "Grant Permission"
        )
        OnboardingStep.ENABLE_FREEFORM -> StepData(
            icon = Icons.Filled.Widgets,
            title = "Enable Freeform Windows",
            description = "One-time setup: Canvas will enable the hidden freeform window mode on your device.\nThis lets apps run in resizable windows.",
            buttonText = "Enable Freeform"
        )
        OnboardingStep.COMPLETE -> StepData(
            icon = Icons.Filled.CheckCircle,
            title = "All Set!",
            description = "Canvas is ready. Let's start multitasking.",
            buttonText = "Get Started"
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(CanvasPrimary, CanvasSecondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )

            // Error message
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action button
            Button(
                onClick = onAction,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CanvasPrimary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Refresh button for steps where user returns from another app
            if (step == OnboardingStep.INSTALL_SHIZUKU || step == OnboardingStep.START_SERVICE) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("I've done this, check again")
                }
            }
        }
    }
}

private data class StepData(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val buttonText: String
)
