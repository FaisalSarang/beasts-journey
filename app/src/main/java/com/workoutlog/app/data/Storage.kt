package com.workoutlog.app.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Persists everything to a single JSON file in the app's private storage. */
class Storage(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "workout_log.json"))

    fun load(): AppData = try {
        val text = file.readFully().toString(Charsets.UTF_8)
        decode(JSONObject(text))
    } catch (e: Exception) {
        AppData()
    }

    @Synchronized
    fun save(data: AppData) {
        val out = file.startWrite()
        try {
            out.write(encode(data).toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
        }
    }

    private fun encode(d: AppData) = JSONObject().apply {
        put("version", 1)
        put("unit", d.unit)
        put("workouts", JSONArray(d.workouts.map { w ->
            JSONObject().apply {
                put("id", w.id)
                put("name", w.name)
                put("createdAt", w.createdAt)
                put("exercises", JSONArray(w.exercises.map { e ->
                    JSONObject().apply {
                        put("id", e.id)
                        put("name", e.name)
                        put("type", e.type.name)
                        put("sets", e.sets)
                        e.repMin?.let { put("repMin", it) }
                        e.repMax?.let { put("repMax", it) }
                    }
                }))
            }
        }))
        put("logs", JSONArray(d.logs.map { l ->
            JSONObject().apply {
                put("id", l.id)
                put("workoutId", l.workoutId)
                put("workoutName", l.workoutName)
                put("startedAt", l.startedAt)
                put("finishedAt", l.finishedAt)
                put("exercises", JSONArray(l.exercises.map { e ->
                    JSONObject().apply {
                        put("exerciseId", e.exerciseId)
                        put("name", e.name)
                        put("type", e.type.name)
                        put("sets", JSONArray(e.sets.map { s ->
                            JSONObject().apply {
                                s.weight?.let { put("weight", it) }
                                s.reps?.let { put("reps", it) }
                            }
                        }))
                    }
                }))
            }
        }))
        d.active?.let { a ->
            put("active", JSONObject().apply {
                put("workoutId", a.workoutId)
                put("workoutName", a.workoutName)
                put("startedAt", a.startedAt)
                put("exercises", JSONArray(a.exercises.map { e ->
                    JSONObject().apply {
                        put("exerciseId", e.exerciseId)
                        put("name", e.name)
                        put("type", e.type.name)
                        e.repMin?.let { put("repMin", it) }
                        e.repMax?.let { put("repMax", it) }
                        put("sets", JSONArray(e.sets.map { s ->
                            JSONObject().apply {
                                put("weight", s.weight)
                                put("reps", s.reps)
                                put("done", s.done)
                            }
                        }))
                    }
                }))
            })
        }
    }

    private fun decode(o: JSONObject) = AppData(
        unit = o.optString("unit", "kg"),
        workouts = o.optJSONArray("workouts").objects().map { w ->
            Workout(
                id = w.getString("id"),
                name = w.getString("name"),
                createdAt = w.optLong("createdAt"),
                exercises = w.optJSONArray("exercises").objects().map { e ->
                    Exercise(
                        id = e.getString("id"),
                        name = e.getString("name"),
                        type = ExerciseType.from(e.optString("type")),
                        sets = e.optInt("sets", 3),
                        repMin = e.optIntOrNull("repMin"),
                        repMax = e.optIntOrNull("repMax"),
                    )
                },
            )
        },
        logs = o.optJSONArray("logs").objects().map { l ->
            WorkoutLog(
                id = l.getString("id"),
                workoutId = l.getString("workoutId"),
                workoutName = l.optString("workoutName"),
                startedAt = l.optLong("startedAt"),
                finishedAt = l.optLong("finishedAt"),
                exercises = l.optJSONArray("exercises").objects().map { e ->
                    LoggedExercise(
                        exerciseId = e.getString("exerciseId"),
                        name = e.optString("name"),
                        type = ExerciseType.from(e.optString("type")),
                        sets = e.optJSONArray("sets").objects().map { s ->
                            LoggedSet(
                                weight = if (s.has("weight")) s.getDouble("weight") else null,
                                reps = s.optIntOrNull("reps"),
                            )
                        },
                    )
                },
            )
        },
        active = o.optJSONObject("active")?.let { a ->
            ActiveSession(
                workoutId = a.getString("workoutId"),
                workoutName = a.optString("workoutName"),
                startedAt = a.optLong("startedAt"),
                exercises = a.optJSONArray("exercises").objects().map { e ->
                    ActiveExercise(
                        exerciseId = e.getString("exerciseId"),
                        name = e.optString("name"),
                        type = ExerciseType.from(e.optString("type")),
                        repMin = e.optIntOrNull("repMin"),
                        repMax = e.optIntOrNull("repMax"),
                        sets = e.optJSONArray("sets").objects().map { s ->
                            SetEntry(s.optString("weight"), s.optString("reps"), s.optBoolean("done"))
                        },
                    )
                },
            )
        },
    )

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }

    private fun JSONObject.optIntOrNull(key: String): Int? = if (has(key)) getInt(key) else null
}
