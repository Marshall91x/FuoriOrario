package it.manu.fuoriorario.feature.settings

import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.exercise_updated
import fuoriorario.composeapp.generated.resources.library_added
import fuoriorario.composeapp.generated.resources.library_removed
import it.manu.fuoriorario.core.error.ErrorManager
import it.manu.fuoriorario.core.viewmodel.DispatcherProvider
import it.manu.fuoriorario.core.viewmodel.ScreenModel
import it.manu.fuoriorario.domain.Category
import it.manu.fuoriorario.domain.LibraryExercise
import it.manu.fuoriorario.domain.PlanItemError
import it.manu.fuoriorario.domain.cleaned
import it.manu.fuoriorario.domain.planItemError
import it.manu.fuoriorario.feature.plan.data.PlanRepository

/**
 * [library] is null while it loads. [editing] is the sheet's exercise as typed (null when closed; without id, a new one),
 * with its last failed check. [confirmingRemoval] took the first "Elimina" tap; [busy]: a write is in flight.
 */
data class LibraryScreenState(
    val library: List<LibraryExercise>? = null,
    val editing: LibraryExercise? = null,
    val editError: PlanItemError? = null,
    val confirmingRemoval: Boolean = false,
    val busy: Boolean = false
)

/** Staff's Libreria (PRD F5): add, edit, reorder and delete the team's model exercises. */
class LibraryViewModel(private val plans: PlanRepository, dispatchers: DispatcherProvider, errorManager: ErrorManager) :
    ScreenModel<LibraryScreenState>(LibraryScreenState(), dispatchers, errorManager, busy = { copy(busy = it) }) {
    init {
        defaultLaunch(dispatchers.main()) {
            val library = plans.library()
            set { copy(library = library) }
        }
    }

    /** A new exercise, last in the order. */
    fun onAdd() {
        val list = state.library ?: return
        val sort = (list.maxOfOrNull { it.sort } ?: -1) + 1
        onEdit(LibraryExercise("", Category.entries.first(), sort = sort))
    }

    fun onEdit(exercise: LibraryExercise) =
        update { copy(editing = exercise, editError = null, confirmingRemoval = false) }

    fun onEditChanged(exercise: LibraryExercise) = update { copy(editing = exercise) }

    fun onDismiss() = update { copy(editing = null) }

    /** Errors stay in the sheet; a failed save toasts and keeps what was typed. */
    fun onSave() {
        val draft = state.editing ?: return
        val error = planItemError(draft.title, null, draft.videoUrl.orEmpty())
        update { copy(editError = error) }
        if (error != null || state.busy) return
        val exercise = draft.cleaned()
        act {
            if (exercise.id == null) {
                val added = plans.addToLibrary(exercise)
                set { copy(library = library.orEmpty() + added, editing = null) }
                toast(Res.string.library_added)
            } else {
                plans.updateInLibrary(exercise)
                set { copy(library = library?.map { if (it.id == exercise.id) exercise else it }, editing = null) }
                toast(Res.string.exercise_updated)
            }
        }
    }

    /** The first tap asks to confirm, the second deletes the exercise in the sheet. */
    fun onRemove() {
        val exercise = state.editing ?: return
        if (!state.confirmingRemoval) return update { copy(confirmingRemoval = true) }
        if (state.busy) return
        act {
            plans.removeFromLibrary(exercise)
            set { copy(library = library?.filter { it.id != exercise.id }, editing = null) }
            toast(Res.string.library_removed)
        }
    }

    /** One place up ([by] -1) or down (1). */
    fun onMove(exercise: LibraryExercise, by: Int) {
        val list = state.library ?: return
        if (state.busy) return
        val moved = list.toMutableList().apply {
            val i = indexOf(exercise)
            add(i + by, removeAt(i))
        }.mapIndexed { i, it -> it.copy(sort = i) }
        act {
            plans.reorderLibrary(moved)
            set { copy(library = moved) }
        }
    }
}
