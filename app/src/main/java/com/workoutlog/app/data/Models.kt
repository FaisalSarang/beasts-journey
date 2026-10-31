package com.workoutlog.app.data

import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

enum class ExerciseType(val label: String, val cardio: Boolean = false) {
    DUMBBELL("Dumbbells"),
    BARBELL("Barbell"),
    KETTLEBELL("Kettlebell"),
    CABLE("Cable machine"),
    MACHINE("Machine"),
    SMITH("Smith machine"),
    BODYWEIGHT("Bodyweight"),
    BANDS("Resistance bands"),
    TREADMILL("Treadmill", cardio = true),
    BIKE("Stationary bike", cardio = true),
    ROWER("Rowing machine", cardio = true),
    ELLIPTICAL("Elliptical", cardio = true),
    STAIRS("Stair climber", cardio = true),
    OTHER("Other");

    companion object {
        fun from(name: String?): ExerciseType = entries.firstOrNull { it.name == name } ?: OTHER
    }
}

/** A planned exercise inside a workout template. For cardio, the range is target minutes. */
data class Exercise(
    val id: String = newId(),
    val name: String,
    val type: ExerciseType,
    val sets: Int,
    val repMin: Int? = null,
    val repMax: Int? = null,
) {
    val rangeText: String?
        get() = when {
            repMin != null && repMax != null && repMin != repMax -> "$repMin-$repMax"
            repMin != null -> "$repMin"
            repMax != null -> "$repMax"
            else -> null
        }
}

data class Workout(
    val id: String = newId(),
    val name: String,
    val exercises: List<Exercise>,
    val createdAt: Long = System.currentTimeMillis(),
)

/** One set row while a workout is in progress; text fields stay as strings so partial input like "12." survives. */
data class SetEntry(
    val weight: String = "",
    val reps: String = "",
    val done: Boolean = false,
)

data class ActiveExercise(
    val exerciseId: String,
    val name: String,
    val type: ExerciseType,
    val repMin: Int?,
    val repMax: Int?,
    val sets: List<SetEntry>,
) {
    val rangeText: String?
        get() = Exercise(exerciseId, name, type, sets.size, repMin, repMax).rangeText
}

data class ActiveSession(
    val workoutId: String,
    val workoutName: String,
    val startedAt: Long,
    val exercises: List<ActiveExercise>,
)

/** For cardio sets, [weight] holds distance and [reps] holds minutes. */
data class LoggedSet(val weight: Double?, val reps: Int?)

data class LoggedExercise(
    val exerciseId: String,
    val name: String,
    val type: ExerciseType,
    val sets: List<LoggedSet>,
)

data class WorkoutLog(
    val id: String = newId(),
    val workoutId: String,
    val workoutName: String,
    val startedAt: Long,
    val finishedAt: Long,
    val exercises: List<LoggedExercise>,
)

data class AppData(
    val workouts: List<Workout> = emptyList(),
    val logs: List<WorkoutLog> = emptyList(),
    val active: ActiveSession? = null,
    val unit: String = "kg",
) {
    fun lastLogFor(workoutId: String): WorkoutLog? =
        logs.filter { it.workoutId == workoutId }.maxByOrNull { it.finishedAt }
}
