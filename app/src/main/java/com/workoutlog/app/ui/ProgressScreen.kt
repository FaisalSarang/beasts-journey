package com.workoutlog.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.workoutlog.app.data.AppData
import com.workoutlog.app.data.LoggedExercise
import com.workoutlog.app.data.WorkoutLog
import com.workoutlog.app.ui.theme.Hint
import kotlin.math.roundToInt

private enum class Metric(val label: String, val cardio: Boolean) {
    E1RM("Est. 1RM", false),
    BEST("Best weight", false),
    VOLUME("Volume", false),
    REPS("Total reps", false),
    DISTANCE("Distance", true),
    MINUTES("Minutes", true);

    fun of(e: LoggedExercise): Double? {
        val s = e.sets
        return when (this) {
            E1RM -> s.mapNotNull { x -> if (x.weight != null && x.reps != null && x.reps > 0) x.weight * (1 + x.reps / 30.0) else null }.maxOrNull()
            BEST -> s.mapNotNull { it.weight }.maxOrNull()
            VOLUME -> s.mapNotNull { x -> if (x.weight != null && x.reps != null) x.weight * x.reps else null }.takeIf { it.isNotEmpty() }?.sum()
            REPS, MINUTES -> s.mapNotNull { it.reps }.takeIf { it.isNotEmpty() }?.sum()?.toDouble()
            DISTANCE -> s.mapNotNull { it.weight }.takeIf { it.isNotEmpty() }?.sum()
        }
    }

    fun unit(weightUnit: String) = when (this) {
        E1RM, BEST, VOLUME -> weightUnit
        REPS -> "reps"
        DISTANCE -> "km"
        MINUTES -> "min"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(data: AppData, vm: AppViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Progress", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { vm.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
                TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Strength") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("History") })
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (data.logs.isEmpty()) {
                Text(
                    "Finish a workout to start tracking your progress. Each session is compared with your previous logs.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            } else if (tab == 0) {
                StrengthTab(data)
            } else {
                HistoryTab(data, vm)
            }
        }
    }
}

@Composable
private fun StrengthTab(data: AppData) {
    // Group by workout; include logs of deleted templates under their saved name.
    val groups = remember(data.logs) {
        data.logs.groupBy { it.workoutId }
            .map { (id, logs) -> Triple(id, logs.maxBy { it.finishedAt }.workoutName, logs.sortedBy { it.finishedAt }) }
            .sortedByDescending { it.third.last().finishedAt }
    }
    var selectedId by remember { mutableStateOf(groups.first().first) }
    val selected = groups.firstOrNull { it.first == selectedId } ?: groups.first()
    val logs = selected.third

    val exerciseIds = remember(logs) {
        logs.flatMap { l -> l.exercises.map { it.exerciseId } }.distinct()
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                groups.forEach { (id, name, _) ->
                    FilterChip(selected = id == selected.first, onClick = { selectedId = id }, label = { Text(name) })
                }
            }
        }
        item { WorkoutSummary(logs, data.unit) }
        items(exerciseIds, key = { it }) { exId ->
            val points = logs.mapNotNull { l -> l.exercises.firstOrNull { it.exerciseId == exId }?.let { l.finishedAt to it } }
            ExerciseProgressCard(points, data.unit)
        }
    }
}

private fun WorkoutLog.volume(): Double =
    exercises.filterNot { it.type.cardio }.sumOf { e -> e.sets.sumOf { s -> (s.weight ?: 0.0) * (s.reps ?: 0) } }

@Composable
private fun WorkoutSummary(logs: List<WorkoutLog>, unit: String) {
    val first = logs.first()
    val last = logs.last()
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text("Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row {
                Stat("Sessions", "${logs.size}", Modifier.weight(1f))
                Stat("Last volume", "${formatNumber(last.volume())} $unit", Modifier.weight(1f))
                Stat("Since first", changeText(first.volume(), last.volume()), Modifier.weight(1f), changeColor(first.volume(), last.volume()))
            }
            if (logs.size >= 2) {
                Spacer(Modifier.height(12.dp))
                LineChart(logs.map { it.volume() }, Modifier.fillMaxWidth().height(110.dp))
                Row {
                    Text(formatShortDate(first.finishedAt), style = MaterialTheme.typography.labelSmall, color = Hint, modifier = Modifier.weight(1f))
                    Text(formatShortDate(last.finishedAt), style = MaterialTheme.typography.labelSmall, color = Hint)
                }
            }
        }
    }
}

@Composable
private fun ExerciseProgressCard(points: List<Pair<Long, LoggedExercise>>, weightUnit: String) {
    val latest = points.last().second
    val cardio = latest.type.cardio
    val available = Metric.entries.filter { m -> m.cardio == cardio && points.any { m.of(it.second) != null } }
    var metric by remember { mutableStateOf(available.firstOrNull()) }
    val m = metric?.takeIf { it in available } ?: available.firstOrNull()

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text(latest.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(latest.type.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (m == null) {
                Text("No values logged yet.", color = Hint, modifier = Modifier.padding(top = 8.dp))
                return@Column
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                available.forEach { option ->
                    FilterChip(selected = option == m, onClick = { metric = option }, label = { Text(option.label) })
                }
            }
            val series = points.mapNotNull { (t, e) -> m.of(e)?.let { t to it } }
            val values = series.map { it.second }
            val unit = m.unit(weightUnit)
            val prev = values.getOrNull(values.size - 2)
            Spacer(Modifier.height(8.dp))
            Row {
                Stat("Latest", "${formatNumber(values.last())} $unit", Modifier.weight(1f))
                Stat("Best", "${formatNumber(values.max())} $unit", Modifier.weight(1f))
                Stat("vs last time", prev?.let { changeText(it, values.last()) } ?: "—", Modifier.weight(1f), prev?.let { changeColor(it, values.last()) })
                Stat("Since first", changeText(values.first(), values.last()), Modifier.weight(1f), changeColor(values.first(), values.last()))
            }
            if (values.size >= 2) {
                Spacer(Modifier.height(12.dp))
                LineChart(values, Modifier.fillMaxWidth().height(100.dp))
                Row {
                    Text(formatShortDate(series.first().first), style = MaterialTheme.typography.labelSmall, color = Hint, modifier = Modifier.weight(1f))
                    Text(formatShortDate(series.last().first), style = MaterialTheme.typography.labelSmall, color = Hint)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Last session: " + latest.sets.joinToString("   ") { s ->
                    listOfNotNull(s.weight?.let(::formatNumber), s.reps?.toString()).joinToString("×")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun changeText(from: Double, to: Double): String {
    if (from == 0.0) return if (to == 0.0) "0%" else "new"
    val pct = ((to - from) / from * 100).roundToInt()
    return if (pct > 0) "+$pct%" else "$pct%"
}

@Composable
private fun changeColor(from: Double, to: Double): Color = when {
    to > from -> MaterialTheme.colorScheme.primary
    to < from -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurface
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier, color: Color? = null) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold, color = color ?: MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun LineChart(values: List<Double>, modifier: Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        val min = values.min()
        val max = values.max()
        val span = (max - min).takeIf { it > 0 } ?: 1.0
        val pad = 8.dp.toPx()
        val w = size.width - pad * 2
        val h = size.height - pad * 2
        fun point(i: Int) = Offset(
            x = pad + if (values.size == 1) w / 2 else w * i / (values.size - 1),
            y = pad + h - (h * ((values[i] - min) / span)).toFloat(),
        )
        listOf(0f, 0.5f, 1f).forEach { f ->
            val y = pad + h * f
            drawLine(grid, Offset(pad, y), Offset(pad + w, y), strokeWidth = 1.dp.toPx())
        }
        val path = Path().apply {
            values.indices.forEach { i -> point(i).let { if (i == 0) moveTo(it.x, it.y) else lineTo(it.x, it.y) } }
        }
        drawPath(path, line, style = Stroke(width = 3.dp.toPx()))
        values.indices.forEach { i -> drawCircle(line, radius = 4.dp.toPx(), center = point(i)) }
    }
}

@Composable
private fun HistoryTab(data: AppData, vm: AppViewModel) {
    var pendingDelete by remember { mutableStateOf<WorkoutLog?>(null) }
    val logs = remember(data.logs) { data.logs.sortedByDescending { it.finishedAt } }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(logs, key = { it.id }) { log ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(log.workoutName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${formatDate(log.finishedAt)} · ${formatDuration(log.finishedAt - log.startedAt)} · " +
                                    "${log.exercises.sumOf { it.sets.size }} sets",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { pendingDelete = log }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete log", tint = Hint)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 0.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    log.exercises.forEach { e ->
                        Row(Modifier.padding(vertical = 2.dp, horizontal = 0.dp)) {
                            Text(e.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                e.sets.joinToString("  ") { s ->
                                    listOfNotNull(s.weight?.let(::formatNumber), s.reps?.toString()).joinToString("×")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this log?") },
            text = { Text("${log.workoutName} on ${formatDate(log.finishedAt)} will be removed from your history.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteLog(log.id); pendingDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}
