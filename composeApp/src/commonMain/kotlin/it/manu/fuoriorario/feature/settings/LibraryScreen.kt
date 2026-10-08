package it.manu.fuoriorario.feature.settings

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.exercise_edit
import fuoriorario.composeapp.generated.resources.exercise_new
import fuoriorario.composeapp.generated.resources.exercise_update
import fuoriorario.composeapp.generated.resources.library_add
import fuoriorario.composeapp.generated.resources.library_down
import fuoriorario.composeapp.generated.resources.library_empty
import fuoriorario.composeapp.generated.resources.library_hint
import fuoriorario.composeapp.generated.resources.library_label
import fuoriorario.composeapp.generated.resources.library_remove
import fuoriorario.composeapp.generated.resources.library_remove_confirm
import fuoriorario.composeapp.generated.resources.library_save
import fuoriorario.composeapp.generated.resources.library_title
import fuoriorario.composeapp.generated.resources.library_up
import it.manu.fuoriorario.core.designsystem.CategoryChip
import it.manu.fuoriorario.core.designsystem.ExerciseErrorText
import it.manu.fuoriorario.core.designsystem.ExerciseFields
import it.manu.fuoriorario.core.designsystem.GhostButton
import it.manu.fuoriorario.core.designsystem.GhostStyle
import it.manu.fuoriorario.core.designsystem.LocalToast
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.designsystem.PrimaryButton
import it.manu.fuoriorario.core.designsystem.Sheet
import it.manu.fuoriorario.core.designsystem.show
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.Category
import it.manu.fuoriorario.domain.LibraryExercise
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Staff's Libreria (PRD F5): the team's model exercises in their order, moved with ↑ ↓. Tap one to edit it, or delete it
 * with a second tap. Plans hold copies, so nothing here changes them (ADR 0005).
 */
@Composable
fun LibraryScreen(vm: LibraryViewModel = koinViewModel()) {
    val uiState = vm.uiState.collectAsStateWithLifecycle()
    val toast = LocalToast.current
    LaunchedEffect(vm) { vm.toasts.collect { launch { toast.show(it) } } }
    LibraryStateContent(
        uiState.value.state,
        LibraryActions(
            onAdd = vm::onAdd,
            onEdit = vm::onEdit,
            onEditChanged = vm::onEditChanged,
            onDismiss = vm::onDismiss,
            onSave = vm::onSave,
            onRemove = vm::onRemove,
            onMove = vm::onMove
        )
    )
}

/** What the library does, from the screen down to its sheet. */
class LibraryActions(
    val onAdd: () -> Unit = {},
    val onEdit: (LibraryExercise) -> Unit = {},
    val onEditChanged: (LibraryExercise) -> Unit = {},
    val onDismiss: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onRemove: () -> Unit = {},
    val onMove: (LibraryExercise, Int) -> Unit = { _, _ -> }
)

@Composable
fun LibraryStateContent(state: UseCaseMutableState<LibraryScreenState>?, actions: LibraryActions) {
    when (state) {
        is UseCaseMutableState.Error -> state.handler.ErrorScreenContent()
        is UseCaseMutableState.ShowData -> LibraryContent(state.items, actions)
        UseCaseMutableState.Loading, null -> Unit
    }
}

/** Nothing until the library arrives. */
@Composable
fun LibraryContent(state: LibraryScreenState, actions: LibraryActions) {
    val c = FuoriOrarioTheme.colors
    val list = state.library ?: return
    Panel {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(Res.string.library_title).uppercase(), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(Res.string.library_hint), color = c.muted, style = MaterialTheme.typography.bodyMedium)
        }
        PrimaryButton(stringResource(Res.string.library_add), actions.onAdd)
        if (list.isEmpty()) {
            Text(stringResource(Res.string.library_empty), color = c.muted)
        } else {
            Column {
                list.forEachIndexed { i, exercise ->
                    if (i > 0) HorizontalDivider(color = c.line)
                    LibraryRow(
                        exercise,
                        onEdit = { actions.onEdit(exercise) },
                        onUp = if (i > 0) ({ actions.onMove(exercise, -1) }) else null,
                        onDown = if (i < list.lastIndex) ({ actions.onMove(exercise, 1) }) else null
                    )
                }
            }
        }
    }
    state.editing?.let { LibrarySheet(it, state, actions) }
}

@Preview
@Composable
private fun LibraryContentPreview() = FuoriOrarioTheme {
    Column {
        LibraryContent(
            LibraryScreenState(
                listOf(
                    LibraryExercise("Mikan drill", Category.FOOTWORK, "3 × 20 canestri", sort = 0, id = "l1"),
                    LibraryExercise("Tiri liberi sotto fatica", Category.SHOOTING, sort = 1, id = "l2")
                )
            ),
            LibraryActions()
        )
    }
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
private fun LibrarySheet(draft: LibraryExercise, state: LibraryScreenState, actions: LibraryActions) {
    val isNew = draft.id == null
    Sheet(
        stringResource(if (isNew) Res.string.exercise_new else Res.string.exercise_edit),
        stringResource(Res.string.library_label),
        actions.onDismiss
    ) {
        ExerciseFields(draft, actions.onEditChanged)
        ExerciseErrorText(state.editError)
        if (isNew) {
            PrimaryButton(
                stringResource(Res.string.library_save),
                actions.onSave,
                Modifier.fillMaxWidth(),
                enabled = !state.busy
            )
        } else {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                PrimaryButton(stringResource(Res.string.exercise_update), actions.onSave, enabled = !state.busy)
                GhostButton(
                    stringResource(
                        if (state.confirmingRemoval) Res.string.library_remove_confirm else Res.string.library_remove
                    ),
                    actions.onRemove,
                    style = GhostStyle.DANGER
                )
            }
        }
    }
}
