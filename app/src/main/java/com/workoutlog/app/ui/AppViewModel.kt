package com.workoutlog.app.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.workoutlog.app.data.ActiveExercise
import com.workoutlog.app.data.ActiveSession
import com.workoutlog.app.data.AppData
import com.workoutlog.app.data.LoggedExercise
import com.workoutlog.app.data.LoggedSet
import com.workoutlog.app.data.SetEntry
import com.workoutlog.app.data.Storage
import com.workoutlog.app.data.Workout
import com.workoutlog.app.data.WorkoutLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object Home : Screen
    data class Editor(val workoutId: String?) : Screen
    data object Session : Screen
    data object Progress : Screen
}

@OptIn(FlowPreview::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val storage = Storage(app)
    private val _data = MutableStateFlow(storage.load())
    val data: StateFlow<AppData> = _data

    val backStack = mutableStateListOf<Screen>(Screen.Home)

    init {
        viewModelScope.launch {
            _data.drop(1).debounce(300).collect { d ->
                withContext(Dispatchers.IO) { storage.save(d) }
            }
        }
    }

    /** Write immediately, e.g. when the app goes to the background. */
    fun flush() {
        val snapshot = _data.value
        viewModelScope.launch(Dispatchers.IO) { storage.save(snapshot) }
    }

    fun navigate(screen: Screen) {
        backStack.add(screen)
    }

    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    fun setUnit(unit: String) = _data.update { it.copy(unit = unit) }

    fun saveWorkout(workout: Workout) = _data.update { d ->
        val exists = d.workouts.any { it.id == workout.id }
        d.copy(workouts = if (exists) d.workouts.map { if (it.id == workout.id) workout else it } else d.workouts + workout)
    }

    fun deleteWorkout(id: String) = _data.update { d -> d.copy(workouts = d.workouts.filterNot { it.id == id }) }

    fun startSession(workout: Workout) {
        _data.update { d ->
            val last = d.lastLogFor(workout.id)
            d.copy(
                active = ActiveSession(
                    workoutId = workout.id,
                    workoutName = workout.name,
                    startedAt = System.currentTimeMillis(),
                    exercises = workout.exercises.map { e ->
                        // Match the number of sets done last time if the user added extras.
                        val lastSets = last?.exercises?.firstOrNull { it.exerciseId == e.id }?.sets?.size ?: 0
                        ActiveExercise(
                            exerciseId = e.id,
                            name = e.name,
                            type = e.type,
                            repMin = e.repMin,
                            repMax = e.repMax,
                            sets = List(maxOf(e.sets, lastSets, 1)) { SetEntry() },
                        )
                    },
                ),
            )
        }
        navigate(Screen.Session)
    }

    fun updateSession(transform: (ActiveSession) -> ActiveSession) = _data.update { d ->
        d.copy(active = d.active?.let(transform))
    }

    fun discardSession() {
        _data.update { it.copy(active = null) }
        backStack.removeAll { it == Screen.Session }
    }

    fun finishSession() {
        _data.update { d ->
            val a = d.active ?: return@update d
            val exercises = a.exercises.mapNotNull { e ->
                val sets = e.sets.mapNotNull { s ->
                    val w = s.weight.replace(',', '.').toDoubleOrNull()
                    val r = s.reps.toIntOrNull()
                    if (w == null && r == null) null else LoggedSet(w, r)
                }
                if (sets.isEmpty()) null else LoggedExercise(e.exerciseId, e.name, e.type, sets)
            }
            val log = WorkoutLog(
                workoutId = a.workoutId,
                workoutName = a.workoutName,
                startedAt = a.startedAt,
                finishedAt = System.currentTimeMillis(),
                exercises = exercises,
            )
            d.copy(active = null, logs = if (exercises.isEmpty()) d.logs else d.logs + log)
        }
        flush()
        backStack.removeAll { it == Screen.Session }
    }

    fun deleteLog(id: String) = _data.update { d -> d.copy(logs = d.logs.filterNot { it.id == id }) }
}
