package com.gymtracker.app.data.model

data class Exercise(
    val name: String,
    val category: ExerciseCategory,
    val isCompound: Boolean = true
) {
    companion object {
        val ALL_EXERCISES: List<Exercise> = listOf(
            // Ramena (compound prvo, pa izolacija)
            Exercise("Overhead Press", ExerciseCategory.RAMENA, true),
            Exercise("Dumbbell Shoulder Press", ExerciseCategory.RAMENA, true),
            Exercise("Arnold Press", ExerciseCategory.RAMENA, true),
            Exercise("Shoulder Press Machine", ExerciseCategory.RAMENA, true),
            Exercise("Upright Row", ExerciseCategory.RAMENA, true),
            Exercise("Barbell Shrugs", ExerciseCategory.RAMENA, false),
            Exercise("Dumbbell Shrugs", ExerciseCategory.RAMENA, false),
            Exercise("Lateral Raise", ExerciseCategory.RAMENA, false),
            Exercise("Cable Lateral Raise", ExerciseCategory.RAMENA, false),
            Exercise("Front Raise", ExerciseCategory.RAMENA, false),
            Exercise("Rear Delt Fly", ExerciseCategory.RAMENA, false),
            Exercise("Reverse Pec Deck", ExerciseCategory.RAMENA, false),

            // Grudi (compound prvo, pa izolacija)
            Exercise("Bench Press", ExerciseCategory.GRUDI, true),
            Exercise("Incline Barbell Press", ExerciseCategory.GRUDI, true),
            Exercise("Dumbbell Press", ExerciseCategory.GRUDI, true),
            Exercise("Incline Dumbbell Press", ExerciseCategory.GRUDI, true),
            Exercise("Decline Press", ExerciseCategory.GRUDI, true),
            Exercise("Chest Dips", ExerciseCategory.GRUDI, true),
            Exercise("Push-ups", ExerciseCategory.GRUDI, true),
            Exercise("Diamond Push-ups", ExerciseCategory.GRUDI, true),
            Exercise("Chest Press Machine", ExerciseCategory.GRUDI, true),
            Exercise("Cable Crossover", ExerciseCategory.GRUDI, false),
            Exercise("High Cable Crossover", ExerciseCategory.GRUDI, false),
            Exercise("Low Cable Crossover", ExerciseCategory.GRUDI, false),
            Exercise("Pec Deck", ExerciseCategory.GRUDI, false),
            Exercise("Dumbbell Pullover", ExerciseCategory.GRUDI, false),

            // Leđa (compound prvo, pa izolacija)
            Exercise("Deadlift", ExerciseCategory.LEDJA, true),
            Exercise("Pull-ups", ExerciseCategory.LEDJA, true),
            Exercise("Chin-ups", ExerciseCategory.LEDJA, true),
            Exercise("Barbell Row", ExerciseCategory.LEDJA, true),
            Exercise("T-Bar Row", ExerciseCategory.LEDJA, true),
            Exercise("Dumbbell Row", ExerciseCategory.LEDJA, true),
            Exercise("Cable Row", ExerciseCategory.LEDJA, true),
            Exercise("Inverted Row", ExerciseCategory.LEDJA, true),
            Exercise("Lat Pulldown", ExerciseCategory.LEDJA, true),
            Exercise("Close Grip Lat Pulldown", ExerciseCategory.LEDJA, true),
            Exercise("Wide Grip Lat Pulldown", ExerciseCategory.LEDJA, true),
            Exercise("Face Pull", ExerciseCategory.LEDJA, false),

            // Biceps (sve izolacija)
            Exercise("Barbell Curl", ExerciseCategory.BICEPS, false),
            Exercise("EZ Bar Curl", ExerciseCategory.BICEPS, false),
            Exercise("Dumbbell Curl", ExerciseCategory.BICEPS, false),
            Exercise("Hammer Curl", ExerciseCategory.BICEPS, false),
            Exercise("Preacher Curl", ExerciseCategory.BICEPS, false),
            Exercise("Incline Dumbbell Curl", ExerciseCategory.BICEPS, false),
            Exercise("Cable Curl", ExerciseCategory.BICEPS, false),
            Exercise("Concentration Curl", ExerciseCategory.BICEPS, false),
            Exercise("Spider Curl", ExerciseCategory.BICEPS, false),
            Exercise("Machine Curl", ExerciseCategory.BICEPS, false),

            // Triceps (compound prvo, pa izolacija)
            Exercise("Close Grip Bench Press", ExerciseCategory.TRICEPS, true),
            Exercise("Triceps Dips", ExerciseCategory.TRICEPS, true),
            Exercise("Bench Dips", ExerciseCategory.TRICEPS, true),
            Exercise("Skull Crushers", ExerciseCategory.TRICEPS, false),
            Exercise("Triceps Pushdown", ExerciseCategory.TRICEPS, false),
            Exercise("Rope Pushdown", ExerciseCategory.TRICEPS, false),
            Exercise("Overhead Triceps Extension", ExerciseCategory.TRICEPS, false),
            Exercise("Cable Overhead Extension", ExerciseCategory.TRICEPS, false),

            Exercise("Triceps Kickback", ExerciseCategory.TRICEPS, false),
            Exercise("Triceps Machine", ExerciseCategory.TRICEPS, false),

            // Trbušnjaci
            Exercise("Hanging Leg Raises", ExerciseCategory.TRBUSNJACI, true),
            Exercise("Ab Wheel Rollout", ExerciseCategory.TRBUSNJACI, true),
            Exercise("Leg Raises", ExerciseCategory.TRBUSNJACI, false),
            Exercise("Plank", ExerciseCategory.TRBUSNJACI, true),
            Exercise("Mountain Climbers", ExerciseCategory.TRBUSNJACI, true),
            Exercise("Decline Crunches", ExerciseCategory.TRBUSNJACI, false),
            Exercise("Crunches", ExerciseCategory.TRBUSNJACI, false),
            Exercise("Bicycle Crunches", ExerciseCategory.TRBUSNJACI, false),
            Exercise("Russian Twists", ExerciseCategory.TRBUSNJACI, false),
            Exercise("Plank Shoulder Taps", ExerciseCategory.TRBUSNJACI, true),

            // Noge (compound prvo, pa izolacija)
            Exercise("Barbell Squat", ExerciseCategory.NOGE, true),
            Exercise("Front Squat", ExerciseCategory.NOGE, true),
            Exercise("Leg Press", ExerciseCategory.NOGE, true),
            Exercise("Hack Squat", ExerciseCategory.NOGE, true),
            Exercise("Hip Thrust", ExerciseCategory.NOGE, true),
            Exercise("Romanian Deadlift", ExerciseCategory.NOGE, true),
            Exercise("Bulgarian Split Squat", ExerciseCategory.NOGE, true),
            Exercise("Lunges", ExerciseCategory.NOGE, true),
            Exercise("Walking Lunges", ExerciseCategory.NOGE, true),
            Exercise("Goblet Squat", ExerciseCategory.NOGE, true),
            Exercise("Leg Extension", ExerciseCategory.NOGE, false),
            Exercise("Leg Curl", ExerciseCategory.NOGE, false),
            Exercise("Hip Adduction", ExerciseCategory.NOGE, false),
            Exercise("Hip Abduction", ExerciseCategory.NOGE, false),
            Exercise("Standing Calf Raise", ExerciseCategory.NOGE, false),
            Exercise("Seated Calf Raise", ExerciseCategory.NOGE, false),

            // Kardio (sve compound - full body)
            Exercise("Running", ExerciseCategory.KARDIO, true),
            Exercise("Sprint", ExerciseCategory.KARDIO, true),
            Exercise("Burpees", ExerciseCategory.KARDIO, true),
            Exercise("Jump Rope", ExerciseCategory.KARDIO, true),
            Exercise("Box Jumps", ExerciseCategory.KARDIO, true),
            Exercise("Rowing Machine", ExerciseCategory.KARDIO, true),
            Exercise("Treadmill", ExerciseCategory.KARDIO, true),
            Exercise("Cycling", ExerciseCategory.KARDIO, true),
            Exercise("Stationary Bike", ExerciseCategory.KARDIO, true),
            Exercise("Elliptical", ExerciseCategory.KARDIO, true),
            Exercise("Stair Climber", ExerciseCategory.KARDIO, true),
            Exercise("Walking", ExerciseCategory.KARDIO, true)
        )

        fun byCategory(category: ExerciseCategory): List<Exercise> {
            return ALL_EXERCISES.filter { it.category == category }
        }

        fun findByName(name: String): Exercise? {
            return ALL_EXERCISES.firstOrNull {
                it.name.equals(name, ignoreCase = true)
            }
        }
    }
}

