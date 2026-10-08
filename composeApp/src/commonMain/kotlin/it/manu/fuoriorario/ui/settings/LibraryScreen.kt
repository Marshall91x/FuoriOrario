package it.manu.fuoriorario.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.exercise_edit
import fuoriorario.composeapp.generated.resources.exercise_new
import fuoriorario.composeapp.generated.resources.exercise_update
import fuoriorario.composeapp.generated.resources.exercise_updated
import fuoriorario.composeapp.generated.resources.library_add
import fuoriorario.composeapp.generated.resources.library_added
import fuoriorario.composeapp.generated.resources.library_down
import fuoriorario.composeapp.generated.resources.library_empty
import fuoriorario.composeapp.generated.resources.library_hint
import fuoriorario.composeapp.generated.resources.library_label
import fuoriorario.composeapp.generated.resources.library_remove
import fuoriorario.composeapp.generated.resources.library_remove_confirm
import fuoriorario.composeapp.generated.resources.library_removed
import fuoriorario.composeapp.generated.resources.library_save
import fuoriorario.composeapp.generated.resources.library_title
import fuoriorario.composeapp.generated.resources.library_up
import it.manu.fuoriorario.core.designsystem.CategoryChip
import it.manu.fuoriorario.core.designsystem.ExerciseErrorText
import it.manu.fuoriorario.core.designsystem.ExerciseFields
import it.manu.fuoriorario.core.designsystem.GhostButton
import it.manu.fuoriorario.core.designsystem.GhostStyle
import it.manu.fuoriorario.core.designsystem.LoadFailed
import it.manu.fuoriorario.core.designsystem.LocalToast
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.designsystem.PrimaryButton
import it.manu.fuoriorario.core.designsystem.Sheet
import it.manu.fuoriorario.core.designsystem.launchWrite
import it.manu.fuoriorario.core.designsystem.show
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.domain.Category
import it.manu.fuoriorario.domain.LibraryExercise
import it.manu.fuoriorario.domain.PlanItemError
import it.manu.fuoriorario.domain.cleaned
import it.manu.fuoriorario.domain.planItemError
import it.manu.fuoriorario.feature.plan.data.PlanRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Staff's Libreria (PRD F5): the team's model exercises in their order, moved with ↑ ↓. Tap one to edit it, or delete it
 * with a second tap. Plans hold copies, so nothing here changes them (ADR 0005).
 */
@Composable
fun LibraryScreen(plans: PlanRepository) {
    val c = FuoriOrarioTheme.colors
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    var library by remember { mutableStateOf<List<LibraryExercise>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }

    /** The exercise in the sheet; without id, a new one. */
    var editing by remember { mutableStateOf<LibraryExercise?>(null) }

    /** A save, removal or move is in flight. */
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(attempt) {
        loadFailed = false
        try {
            library = plans.library()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            loadFailed = true
        }
    }

    if (loadFailed) LoadFailed { attempt++ }
    val list = library ?: return

    fun write(action: suspend CoroutineScope.() -> Unit) {
        if (busy) return
        busy = true
        scope.launchWrite(toast, onDone = { busy = false }, action)
    }

    fun save(exercise: LibraryExercise) = write {
        if (exercise.id == null) {
            library = list + plans.addToLibrary(exercise)
            launch { toast.show(getString(Res.string.library_added)) }
        } else {
            plans.updateInLibrary(exercise)
            library = list.map { if (it.id == exercise.id) exercise else it }
            launch { toast.show(getString(Res.string.exercise_updated)) }
        }
        editing = null
    }

    fun remove(exercise: LibraryExercise) = write {
        plans.removeFromLibrary(exercise)
        library = list.filter { it.id != exercise.id }
        editing = null
        launch { toast.show(getString(Res.string.library_removed)) }
    }

    /** One place up ([by] -1) or down (1). */
    fun move(exercise: LibraryExercise, by: Int) = write {
        val moved = list.toMutableList().apply {
            val i = indexOf(exercise)
            add(i + by, removeAt(i))
        }.mapIndexed { i, it -> it.copy(sort = i) }
        plans.reorderLibrary(moved)
        library = moved
    }

    Panel {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(Res.string.library_title).uppercase(), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(Res.string.library_hint), color = c.muted, style = MaterialTheme.typography.bodyMedium)
        }
        PrimaryButton(stringResource(Res.string.library_add), {
            editing = LibraryExercise("", Category.entries.first(), sort = (list.maxOfOrNull { it.sort } ?: -1) + 1)
        })
        if (list.isEmpty()) {
            Text(stringResource(Res.string.library_empty), color = c.muted)
        } else {
            Column {
                list.forEachIndexed { i, exercise ->
                    if (i > 0) HorizontalDivider(color = c.line)
                    LibraryRow(
                        exercise,
                        onEdit = { editing = exercise },
                        onUp = if (i > 0) ({ move(exercise, -1) }) else null,
                        onDown = if (i < list.lastIndex) ({ move(exercise, 1) }) else null
                    )
                }
            }
        }
    }

    editing?.let { LibrarySheet(it, busy, onDismiss = { editing = null }, onSave = ::save, onRemove = ::remove) }
}

/** Prototype `.item`: area chip, title and volume, then ↑ ↓ where there is room to move. */
@Composable
private fun LibraryRow(exercise: LibraryExercise, onEdit: () -> Unit, onUp: (() -> Unit)?, onDown: (() -> Unit)?) {
    val c = FuoriOrarioTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).clickable(onClick = onEdit), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CategoryChip(exercise.category)
            Text(exercise.title, fontWeight = FontWeight.SemiBold)
            exercise.volume?.let { Text(it, color = c.accent, style = MaterialTheme.typography.bodyMedium) }
        }
        onUp?.let { MoveButton("↑", stringResource(Res.string.library_up), "library_up_${exercise.id}", it) }
        onDown?.let { MoveButton("↓", stringResource(Res.string.library_down), "library_down_${exercise.id}", it) }
    }
}

/** ↑ or ↓, read out as [label]. */
@Composable
private fun MoveButton(arrow: String, label: String, tag: String, onClick: () -> Unit) {
    GhostButton(arrow, onClick, Modifier.testTag(tag).semantics { contentDescription = label })
}

/** The exercise's fields, as in a plan but without days; an existing one can also be deleted with a second tap. */
@Composable
private fun LibrarySheet(
    exercise: LibraryExercise,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (LibraryExercise) -> Unit,
    onRemove: (LibraryExercise) -> Unit
) {
    var draft by remember { mutableStateOf(exercise) }
    var error by remember { mutableStateOf<PlanItemError?>(null) }
    var confirmingRemoval by remember { mutableStateOf(false) }

    fun save() {
        error = planItemError(draft.title, null, draft.videoUrl.orEmpty())
        if (error == null) onSave(draft.cleaned())
    }

    val isNew = exercise.id == null
    Sheet(
        stringResource(if (isNew) Res.string.exercise_new else Res.string.exercise_edit),
        stringResource(Res.string.library_label),
        onDismiss
    ) {
        ExerciseFields(draft) { draft = it }
        ExerciseErrorText(error)
        if (isNew) {
            PrimaryButton(stringResource(Res.string.library_save), ::save, Modifier.fillMaxWidth(), enabled = !busy)
        } else {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                PrimaryButton(stringResource(Res.string.exercise_update), ::save, enabled = !busy)
                GhostButton(
                    stringResource(
                        if (confirmingRemoval) Res.string.library_remove_confirm else Res.string.library_remove
                    ),
                    { if (confirmingRemoval) onRemove(exercise) else confirmingRemoval = true },
                    style = GhostStyle.DANGER
                )
            }
        }
    }
}
