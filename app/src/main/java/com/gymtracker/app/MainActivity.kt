package com.gymtracker.app

import androidx.compose.material3.MaterialTheme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gymtracker.app.data.backup.BackupProvider
import com.gymtracker.app.data.backup.GoogleDriveBackup
import com.gymtracker.app.data.local.DatabaseProvider
import com.gymtracker.app.data.repository.BodyMetricsRepository
import com.gymtracker.app.data.repository.BodyMetricsRepositoryProvider
import com.gymtracker.app.data.repository.CustomExerciseRepository
import com.gymtracker.app.data.repository.CustomExerciseRepositoryProvider
import com.gymtracker.app.data.repository.CycleRepository
import com.gymtracker.app.data.repository.CycleRepositoryProvider
import com.gymtracker.app.data.repository.MachineSettingsRepository
import com.gymtracker.app.data.repository.MachineSettingsRepositoryProvider
import com.gymtracker.app.data.repository.RepositoryProvider
import com.gymtracker.app.data.repository.WorkoutRepository
import com.gymtracker.app.ui.screens.AddWorkoutScreen
import com.gymtracker.app.ui.screens.MainScreen
import com.gymtracker.app.ui.theme.GymTrackerTheme
import com.gymtracker.app.data.model.WorkoutTemplate
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Proveri da li postoji crash log od prethodnog pokretanja
        val crashFile = File(filesDir, "crash_log.txt")
        if (crashFile.exists()) {
            val crashLog = crashFile.readText()
            crashFile.delete()
            setContent {
                GymTrackerTheme { CrashLogScreen(crashLog) }
            }
            return
        }

        // Inicijalizacija repozitorijuma (posle crash log provere da bi
        // crash log mogao da se prikaže ako je prethodno pokretanje puklo)
        val db = DatabaseProvider.get(application)
        try {
            BackupProvider.get(application)
            RepositoryProvider.get(application, db)
            BodyMetricsRepositoryProvider.get(application, db)
            CycleRepositoryProvider.get(application, db)
            CustomExerciseRepositoryProvider.get(application, db)
            MachineSettingsRepositoryProvider.get(application, db)
        } catch (e: Exception) {
            val sw = java.io.StringWriter()
            e.printStackTrace(java.io.PrintWriter(sw))
            setContent {
                GymTrackerTheme { CrashLogScreen("INIT CRASH:\n${sw}") }
            }
            return
        }

        val workoutRepository = RepositoryProvider.get(application, db)
        val customExerciseRepository = CustomExerciseRepositoryProvider.get(application, db)
        val bodyMetricsRepository = BodyMetricsRepositoryProvider.get(application, db)
        val cycleRepository = CycleRepositoryProvider.get(application, db)
        val backup = BackupProvider.get(application)
        val machineSettingsRepository = MachineSettingsRepositoryProvider.get(application, db)

        setContent {
            GymTrackerTheme {
                AppNavigation(workoutRepository, customExerciseRepository, bodyMetricsRepository, cycleRepository, backup, machineSettingsRepository)
            }
        }
    }
}

@Composable
private fun CrashLogScreen(crashLog: String) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Aplikacija je pukla",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp, top = 40.dp)
        )
        androidx.compose.material3.Button(
            onClick = {
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(crashLog))
                copied = true
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Text(if (copied) "Kopirano!" else "Kopiraj crash log")
        }
        Text(
            text = crashLog,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.horizontalScroll(rememberScrollState())
        )
    }
}

@Composable
private fun AppNavigation(
    workoutRepository: WorkoutRepository,
    customExerciseRepository: CustomExerciseRepository,
    bodyMetricsRepository: BodyMetricsRepository,
    cycleRepository: CycleRepository,
    backup: GoogleDriveBackup,
    machineSettingsRepository: MachineSettingsRepository
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                navController = navController,
                workoutRepository = workoutRepository,
                customExerciseRepository = customExerciseRepository,
                bodyMetricsRepository = bodyMetricsRepository,
                cycleRepository = cycleRepository,
                backup = backup,
                machineSettingsRepository = machineSettingsRepository
            )
        }

        // Novi trening; template = VOLUME (podrazumevano) ili HIT, bira se na „+“
        composable(
            route = "add_workout?template={template}",
            arguments = listOf(
                navArgument("template") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            AddWorkoutScreen(
                navController = navController,
                repository = workoutRepository,
                customExerciseRepository = customExerciseRepository,
                machineSettingsRepository = machineSettingsRepository,
                workoutId = null,
                template = WorkoutTemplate.fromRoute(backStackEntry.arguments?.getString("template"))
            )
        }

        // Edit existing workout
        composable(
            route = "edit_workout/{workoutId}",
            arguments = listOf(
                navArgument("workoutId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getString("workoutId")
            AddWorkoutScreen(
                navController = navController,
                repository = workoutRepository,
                customExerciseRepository = customExerciseRepository,
                machineSettingsRepository = machineSettingsRepository,
                workoutId = workoutId
            )
        }

        // Repeat workout (copy exercises from existing workout)
        composable(
            route = "repeat_workout/{copyFromWorkoutId}",
            arguments = listOf(
                navArgument("copyFromWorkoutId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val copyFromWorkoutId = backStackEntry.arguments?.getString("copyFromWorkoutId")
            AddWorkoutScreen(
                navController = navController,
                repository = workoutRepository,
                customExerciseRepository = customExerciseRepository,
                machineSettingsRepository = machineSettingsRepository,
                workoutId = null,
                copyFromWorkoutId = copyFromWorkoutId
            )
        }
    }
}
