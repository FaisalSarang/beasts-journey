package com.workoutlog.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workoutlog.app.data.ActiveExercise
import com.workoutlog.app.data.ActiveSession
import com.workoutlog.app.data.AppData
import com.workoutlog.app.data.ExerciseType
import com.workoutlog.app.data.LoggedSet
import com.workoutlog.app.data.SetEntry
import com.workoutlog.app.ui.theme.Hint
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(session: ActiveSession, data: AppData, vm: AppViewModel) {
    val previous = remember(session.workoutId, data.logs.size) { data.lastLogFor(session.workoutId) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    fun updateExercise(index: Int, transform: (ActiveExercise) -> ActiveExercise) = vm.updateSession { s ->
        s.copy(exercises = s.exercises.mapIndexed { i, e -> if (i == index) transform(e) else e })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(session.workoutName, fontWeight = FontWeight.Bold)
                        Text(
                            formatDuration(now - session.startedAt),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { vm.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back (workout keeps running)")
                    }
                },
                actions = {
                    Button(onClick = { confirmFinish = true }) { Text("Finish", fontWeight = FontWeight.Bold) }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Discard workout", color = MaterialTheme.colorScheme.error) },
                                onClick = { menuOpen = false; confirmDiscard = true },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(session.exercises, key = { _, e -> e.exerciseId }) { index, exercise ->
                ExerciseCard(
                    exercise = exercise,
                    unit = data.unit,
                    previousSets = previous?.exercises?.firstOrNull { it.exerciseId == exercise.exerciseId }?.sets.orEmpty(),
                    onSetChange = { setIndex, entry ->
                        updateExercise(index) { e -> e.copy(sets = e.sets.mapIndexed { i, s -> if (i == setIndex) entry else s }) }
                    },
                    onAddSet = { updateExercise(index) { e -> e.copy(sets = e.sets + SetEntry()) } },
                    onRemoveSet = { updateExercise(index) { e -> if (e.sets.size > 1) e.copy(sets = e.sets.dropLast(1)) else e } },
                )
            }
        }
    }

    if (confirmFinish) {
        val filled = session.exercises.sumOf { e -> e.sets.count { it.weight.isNotBlank() || it.reps.isNotBlank() } }
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Finish workout?") },
            text = {
                Text(
                    if (filled == 0) "No sets have values yet, so nothing will be saved to your history."
                    else "$filled sets will be saved. Empty sets are skipped.",
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmFinish = false; vm.finishSession() }) { Text("Finish") }
            },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Keep going") } },
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard this workout?") },
            text = { Text("Nothing from this session will be saved.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; vm.discardSession() }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ExerciseCard(
    exercise: ActiveExercise,
    unit: String,
    previousSets: List<LoggedSet>,
    onSetChange: (Int, SetEntry) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: () -> Unit,
) {
    val cardio = exercise.type.cardio
    val weightLabel = when {
        cardio -> "KM"
        exercise.type == ExerciseType.BODYWEIGHT -> "+${unit.uppercase()}"
        else -> unit.uppercase()
    }
    val repsLabel = if (cardio) "MIN" else "REPS"

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Column(Modifier.padding(horizontal = 12.dp)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    buildString {
                        append(exercise.type.label)
                        exercise.rangeText?.let { append(" · target $it ${repsLabel.lowercase()}") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                HeaderCell("SET", Modifier.width(34.dp))
                HeaderCell("PREVIOUS", Modifier.weight(1.2f))
                HeaderCell(weightLabel, Modifier.weight(1f))
                HeaderCell(repsLabel, Modifier.weight(1f))
                Spacer(Modifier.width(44.dp))
            }
            exercise.sets.forEachIndexed { i, set ->
                SetRow(
                    number = i + 1,
                    set = set,
                    previous = previousSets.getOrNull(i),
                    repsHint = exercise.rangeText ?: "",
                    onChange = { onSetChange(i, it) },
                )
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onAddSet) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add set")
                }
                if (exercise.sets.size > 1) {
                    TextButton(onClick = onRemoveSet) {
                        Icon(Icons.Filled.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Remove set")
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    Text(
        text,
        modifier = modifier,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun SetRow(number: Int, set: SetEntry, previous: LoggedSet?, repsHint: String, onChange: (SetEntry) -> Unit) {
    val rowColor = if (set.done) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    Row(
        Modifier.fillMaxWidth().background(rowColor).padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$number",
            modifier = Modifier.width(34.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
        )
        Text(
            previous?.let { p ->
                listOfNotNull(p.weight?.let(::formatNumber), p.reps?.toString()).joinToString(" × ")
            } ?: "—",
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.Center,
            color = Hint,
            style = MaterialTheme.typography.bodyMedium,
        )
        SetField(
            value = set.weight,
            hint = previous?.weight?.let(::formatNumber) ?: "",
            decimal = true,
            onValueChange = { onChange(set.copy(weight = it)) },
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        SetField(
            value = set.reps,
            hint = repsHint,
            decimal = false,
            onValueChange = { onChange(set.copy(reps = it)) },
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        IconButton(
            onClick = { onChange(set.copy(done = !set.done)) },
            modifier = Modifier.padding(start = 4.dp).size(40.dp),
        ) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (set.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = if (set.done) "Mark set not done" else "Mark set done",
                    tint = if (set.done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Compact numeric input; the hint (e.g. the target rep range) is shown in a light color until the user types. */
@Composable
private fun SetField(
    value: String,
    hint: String,
    decimal: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
    BasicTextField(
        value = value,
        onValueChange = { raw ->
            val cleaned = if (decimal) {
                val v = raw.replace(',', '.').filter { it.isDigit() || it == '.' }
                val dot = v.indexOf('.')
                if (dot >= 0) v.substring(0, dot + 1) + v.substring(dot + 1).replace(".", "") else v
            } else {
                raw.filter(Char::isDigit)
            }
            onValueChange(cleaned.take(7))
        },
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = ImeAction.Next,
        ),
        modifier = modifier,
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (value.isEmpty()) {
                    Text(hint, style = textStyle.copy(color = Hint, fontWeight = FontWeight.Normal))
                }
                inner()
            }
        },
    )
}
