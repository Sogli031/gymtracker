package com.gymtracker.app.ui.screens

import android.app.Activity
import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.gymtracker.app.data.backup.BackupProvider
import com.gymtracker.app.data.backup.GoogleDriveBackup
import com.gymtracker.app.data.repository.BodyMetricsRepository
import com.gymtracker.app.data.repository.CustomExerciseRepository
import com.gymtracker.app.data.repository.CycleRepository
import com.gymtracker.app.data.repository.MachineSettingsRepository
import com.gymtracker.app.data.repository.WorkoutRepository
import com.gymtracker.app.ui.components.BackupDialogs
import com.gymtracker.app.ui.theme.AppGradient
import com.gymtracker.app.ui.theme.UiTokens
data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun MainScreen(
    navController: NavController,
    workoutRepository: WorkoutRepository,
    customExerciseRepository: CustomExerciseRepository,
    bodyMetricsRepository: BodyMetricsRepository,
    cycleRepository: CycleRepository,
    backup: GoogleDriveBackup,
    machineSettingsRepository: MachineSettingsRepository
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showWelcomeDialog by remember { mutableStateOf(backup.isFirstLaunch() && !backup.isSignedIn()) }

    // Koordinator živi na nivou aplikacije: backup/vraćanje ne prekida promena taba.
    val application = LocalContext.current.applicationContext as Application
    val coordinator = remember { BackupProvider.coordinator(application) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.result
                backup.handleSignInResult(account)
                // Ako u oblaku već postoji backup, odmah ponudi vraćanje.
                if (account != null) coordinator.onSignedIn()
            } catch (e: Exception) {
                backup.handleSignInResult(null)
            }
        }
        backup.markFirstLaunchDone()
        showWelcomeDialog = false
    }

    // Welcome dialog for first launch
    if (showWelcomeDialog) {
        WelcomeDialog(
            onSignIn = {
                signInLauncher.launch(backup.getSignInIntent())
            },
            onSkip = {
                backup.markFirstLaunchDone()
                showWelcomeDialog = false
            }
        )
    }

    // Pitanja i rezultat backup-a/vraćanja — vidljivi sa bilo kog taba.
    BackupDialogs(coordinator)

    val navItems = listOf(
        BottomNavItem("Istorija", Icons.Filled.History, Icons.Outlined.History),
        BottomNavItem("Napredak", Icons.Filled.BarChart, Icons.Outlined.BarChart),
        BottomNavItem("Telo", Icons.Filled.MonitorWeight, Icons.Outlined.MonitorWeight),
        BottomNavItem("Opcije", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = UiTokens.Surface,
                contentColor = UiTokens.TextPrimary
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = UiTokens.Accent,
                            selectedTextColor = UiTokens.Accent,
                            unselectedIconColor = UiTokens.TextMuted,
                            unselectedTextColor = UiTokens.TextMuted,
                            indicatorColor = UiTokens.Accent.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppGradient)
        ) {
            // Content based on selected tab
            when (selectedTab) {
                0 -> LogContent(
                    navController = navController,
                    repository = workoutRepository,
                    modifier = Modifier.padding(paddingValues)
                )
                1 -> StatisticsContent(
                    repository = workoutRepository,
                    modifier = Modifier.padding(paddingValues)
                )
                2 -> BodyContent(
                    bodyMetricsRepository = bodyMetricsRepository,
                    modifier = Modifier.padding(paddingValues)
                )
                3 -> SettingsContent(
                    backup = backup,
                    coordinator = coordinator,
                    repository = workoutRepository,
                    customExerciseRepository = customExerciseRepository,
                    bodyMetricsRepository = bodyMetricsRepository,
                    cycleRepository = cycleRepository,
                    machineSettingsRepository = machineSettingsRepository,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun WelcomeDialog(
    onSignIn: () -> Unit,
    onSkip: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onSkip,
        containerColor = UiTokens.Surface,
        icon = {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                tint = UiTokens.Accent,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Dobrodošli!",
                color = UiTokens.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text(
                    text = "Povežite Google nalog da biste automatski čuvali svoje treninge u cloud-u.",
                    color = UiTokens.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Možete ovo uraditi i kasnije u podešavanjima.",
                    color = UiTokens.TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSignIn,
                colors = ButtonDefaults.buttonColors(
                    containerColor = UiTokens.Accent,
                    contentColor = UiTokens.TextPrimary
                )
            ) {
                Text("Poveži Google")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text(
                    text = "Preskoči",
                    color = UiTokens.TextMuted
                )
            }
        }
    )
}
