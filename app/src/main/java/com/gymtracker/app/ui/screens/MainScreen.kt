package com.gymtracker.app.ui.screens

import android.app.Activity
import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import com.gymtracker.app.ui.theme.MaxContentWidth
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

    // M3: navigation bar na uskim ekranima, navigation rail od 600dp (tablet, landscape).
    val useRail = LocalConfiguration.current.screenWidthDp >= 600

    val tabContent: @Composable (Modifier) -> Unit = { contentModifier ->
        Box(
            modifier = contentModifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            Crossfade(
                targetState = selectedTab,
                animationSpec = tween(durationMillis = 300),
                label = "tab",
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = MaxContentWidth)
            ) { tab ->
                when (tab) {
                    0 -> LogContent(
                        navController = navController,
                        repository = workoutRepository
                    )
                    1 -> StatisticsContent(
                        repository = workoutRepository
                    )
                    2 -> BodyContent(
                        bodyMetricsRepository = bodyMetricsRepository
                    )
                    3 -> SettingsContent(
                        backup = backup,
                        coordinator = coordinator,
                        repository = workoutRepository,
                        customExerciseRepository = customExerciseRepository,
                        bodyMetricsRepository = bodyMetricsRepository,
                        cycleRepository = cycleRepository,
                        machineSettingsRepository = machineSettingsRepository
                    )
                }
            }
        }
    }

    if (useRail) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Vertical + WindowInsetsSides.Start
                )
            ) {
                Spacer(modifier = Modifier.weight(1f))
                navItems.forEachIndexed { index, item ->
                    NavigationRailItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = null
                            )
                        },
                        label = { Text(item.label, maxLines = 1) }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            tabContent(
                Modifier
                    .weight(1f)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    )
            )
        }
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = null
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            tabContent(Modifier.padding(paddingValues))
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
        icon = {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Dobrodošli!",
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text(
                    text = "Povežite Google nalog da biste automatski čuvali svoje treninge u cloud-u.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Možete ovo uraditi i kasnije u podešavanjima.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSignIn
            ) {
                Text("Poveži Google")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text(
                    text = "Preskoči",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
