package com.workoutlog.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.workoutlog.app.data.Exercise
import com.workoutlog.app.data.ExerciseType
import com.workoutlog.app.data.Workout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(existing: Workout?, vm: AppViewModel) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    val exercises = remember { mutableStateListOf<Exercise>().apply { addAll(existing?.exercises.orEmpty()) } }
    // null = closed, index -1 = new exercise
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }

    val changed = name != (existing?.name ?: "") || exercises.toList() != existing?.exercises.orEmpty()
    val canSave = name.isNotBlank() && exercises.isNotEmpty()

    fun leave() {
        if (changed) confirmDiscard = true else vm.back()
    }

    BackHandler { leave() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New workout" else "Edit workout") },
                navigationIcon = {
                    IconButton(onClick = { leave() }) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                },
                actions = {
                    TextButton(
                        enabled = canSave,
                        onClick = {
                            val w = existing?.copy(name = name.trim(), exercises = exercises.toList())
                                ?: Workout(name = name.trim(), exercises = exercises.toList())
                            vm.saveWorkout(w)
                            vm.back()
                        },
                    ) { Text("Save", fontWeight = FontWeight.Bold) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Workout name") },
                    placeholder = { Text("e.g. Push Day") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(
                    "Exercises",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            itemsIndexed(exercises, key = { _, e -> e.id }) { index, e ->
                ExerciseRow(
                    exercise = e,
                    canMoveUp = index > 0,
                    canMoveDown = index < exercises.lastIndex,
                    onMoveUp = { exercises.add(index - 1, exercises.removeAt(index)) },
                    onMoveDown = { exercises.add(index + 1, exercises.removeAt(index)) },
                    onEdit = { editingIndex = index },
                    onDelete = { exercises.removeAt(index) },
                )
            }
            item {
                OutlinedButton(onClick = { editingIndex = -1 }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add exercise")
                }
            }
        }
    }

    editingIndex?.let { idx ->
        ExerciseDialog(
            initial = exercises.getOrNull(idx),
            onDismiss = { editingIndex = null },
            onSave = { e ->
                if (idx in exercises.indices) exercises[idx] = e else exercises.add(e)
                editingIndex = null
            },
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; vm.back() }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
        )
    }
}

@Composable
private fun ExerciseRow(
    exercise: Exercise,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        onClick = onEdit,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(exercise.name, fontWeight = FontWeight.Bold)
                val unit = if (exercise.type.cardio) "min" else "reps"
                Text(
                    buildString {
                        append(exercise.type.label)
                        append(" · ${exercise.sets} ${if (exercise.sets == 1) "set" else "sets"}")
                        exercise.rangeText?.let { append(" · $it $unit") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column {
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.height(32.dp)) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up")
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.height(32.dp)) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down")
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Remove") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseDialog(initial: Exercise?, onDismiss: () -> Unit, onSave: (Exercise) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: ExerciseType.DUMBBELL) }
    var sets by remember { mutableStateOf(initial?.sets ?: 3) }
    var repMin by remember { mutableStateOf(initial?.repMin?.toString() ?: "") }
    var repMax by remember { mutableStateOf(initial?.repMax?.toString() ?: "") }
    var typeMenuOpen by remember { mutableStateOf(false) }

    val rangeLabel = if (type.cardio) "Target minutes" else "Rep range"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add exercise" else "Edit exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise name") },
                    placeholder = { Text("e.g. Bench Press") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )

                ExposedDropdownMenuBox(expanded = typeMenuOpen, onExpandedChange = { typeMenuOpen = it }) {
                    OutlinedTextField(
                        value = type.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuOpen) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = typeMenuOpen, onDismissRequest = { typeMenuOpen = false }) {
                        ExerciseType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.label) },
                                onClick = { type = t; typeMenuOpen = false },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Number of sets", modifier = Modifier.weight(1f))
                    FilledTonalIconButton(onClick = { if (sets > 1) sets-- }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Fewer sets")
                    }
                    Text(
                        "$sets",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(40.dp),
                    )
                    FilledTonalIconButton(onClick = { if (sets < 20) sets++ }) {
                        Icon(Icons.Filled.Add, contentDescription = "More sets")
                    }
                }

                Column {
                    Text("$rangeLabel (optional)", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = repMin,
                            onValueChange = { repMin = it.filter(Char::isDigit).take(3) },
                            label = { Text("Min") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        Text("–", modifier = Modifier.padding(horizontal = 10.dp))
                        OutlinedTextField(
                            value = repMax,
                            onValueChange = { repMax = it.filter(Char::isDigit).take(3) },
                            label = { Text("Max") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    var lo = repMin.toIntOrNull()
                    var hi = repMax.toIntOrNull()
                    if (lo != null && hi != null && lo > hi) lo = hi.also { hi = lo }
                    val e = (initial ?: Exercise(name = "", type = type, sets = sets))
                        .copy(name = name.trim(), type = type, sets = sets, repMin = lo, repMax = hi)
                    onSave(e)
                },
            ) { Text(if (initial == null) "Add" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
