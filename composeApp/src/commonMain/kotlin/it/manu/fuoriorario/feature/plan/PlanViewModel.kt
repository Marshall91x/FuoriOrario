package it.manu.fuoriorario.feature.plan

import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.exercise_added
import fuoriorario.composeapp.generated.resources.exercise_library_failed
import fuoriorario.composeapp.generated.resources.exercise_removed
import fuoriorario.composeapp.generated.resources.exercise_updated
import fuoriorario.composeapp.generated.resources.note_saved
import fuoriorario.composeapp.generated.resources.plan_changed
import fuoriorario.composeapp.generated.resources.plan_copied
import fuoriorario.composeapp.generated.resources.plan_copy_empty
import it.manu.fuoriorario.core.error.ErrorManager
import it.manu.fuoriorario.core.today
import it.manu.fuoriorario.core.viewmodel.CustomException
import it.manu.fuoriorario.core.viewmodel.DispatcherProvider
import it.manu.fuoriorario.core.viewmodel.ScreenModel
import it.manu.fuoriorario.domain.Category
import it.manu.fuoriorario.domain.LibraryExercise
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanCheck
import it.manu.fuoriorario.domain.PlanItem
import it.manu.fuoriorario.domain.PlanItemError
import it.manu.fuoriorario.domain.WEEKLY_NOTE_MAX
import it.manu.fuoriorario.domain.newPlanItem
import it.manu.fuoriorario.domain.planItemError
import it.manu.fuoriorario.feature.plan.data.PlanChangedException
import it.manu.fuoriorario.feature.plan.data.PlanRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.jetbrains.compose.resources.getString

/**
 * The exercise sheet as typed: a new one when [item] is null (Mon, Wed, Fri by default), else [item] being edited.
 * [error] is the last failed check; [confirmingRemoval] took the first "Rimuovi" tap.
 */
data class ExerciseDraft(
    val item: PlanItem? = null,
    val exercise: LibraryExercise = LibraryExercise("", Category.entries.first()),
    val days: Set<Int> = setOf(0, 2, 4),
    val error: PlanItemError? = null,
    val confirmingRemoval: Boolean = false
)

/**
 * [items] is null while [week] (a Monday) loads. [exercise] and [noteDraft] are the open sheets, null when closed.
 * [library] fills on the first "Aggiungi esercizio". [confirmingCopy] took the first "Copia" tap into a week with
 * exercises; [busy]: a write is in flight.
 */
data class PlanScreenState(
    val player: Member,
    val week: LocalDate,
    val today: LocalDate,
    val items: List<PlanItem>? = null,
    val checks: Set<PlanCheck> = emptySet(),
    val note: String? = null,
    val library: List<LibraryExercise> = emptyList(),
    val exercise: ExerciseDraft? = null,
    val noteDraft: String? = null,
    val confirmingCopy: Boolean = false,
    val busy: Boolean = false
)

/** [player]'s Piano (PRD F4) for a week: staff assign exercises and a note, the player checks the days done. */
class PlanViewModel(
    player: Member,
    week: LocalDate,
    private val plans: PlanRepository,
    private val dispatchers: DispatcherProvider,
    errorManager: ErrorManager
) : ScreenModel<PlanScreenState>(
    // The header is there before the exercises: they fill in when loaded.
    PlanScreenState(player, week, today()),
    dispatchers,
    errorManager,
    busy = { copy(busy = it) }
) {
    private var loading: Job? = null

    init {
        load()
    }

    // On main, like every keystroke: a background `set` could drop one typed meanwhile. The calls suspend, never block.
    private fun load() {
        // The week still loading would overwrite this one.
        loading?.cancel()
        defaultLaunch(dispatchers.main()) {
            loading = coroutineContext.job
            val week = state.week
            val items = plans.items(state.player, week)
            val checks = plans.checks(items)
            val note = plans.note(state.player, week)
            set { if (this.week == week) copy(items = items, checks = checks, note = note) else this }
        }
    }

    fun onWeek(week: LocalDate) {
        update { copy(week = week, items = null, checks = emptySet(), note = null, confirmingCopy = false) }
        load()
    }

    /** The library loads on the first sheet, again on the next if it failed: meanwhile the exercise is typed freely. */
    fun onAdd() {
        update { copy(exercise = ExerciseDraft()) }
        if (state.library.isNotEmpty()) return
        defaultLaunchForChannels(dispatchers.main(), errorFunction = { toast(Res.string.exercise_library_failed) }) {
            val library = plans.library()
            set { copy(library = library) }
        }
    }

    fun onEdit(item: PlanItem) = update {
        val exercise = item.run { LibraryExercise(title, category, volume, description, videoUrl) }
        copy(exercise = ExerciseDraft(item, exercise, item.days.toSet()))
    }

    fun onDismissExercise() = update { copy(exercise = null) }

    /** Typed, or picked from the library. */
    fun onExerciseChanged(exercise: LibraryExercise) = updateExercise { copy(exercise = exercise) }

    fun onDaysChanged(days: Set<Int>) = updateExercise { copy(days = days) }

    /** A new exercise, or the edited one. */
    fun onSaveExercise() {
        val draft = state.exercise ?: return
        if (state.busy) return
        val error = draft.exercise.run { planItemError(title, draft.days, videoUrl.orEmpty()) }
        updateExercise { copy(error = error) }
        if (error != null) return
        val new = draft.exercise.run {
            newPlanItem(
                state.week,
                title,
                category,
                volume.orEmpty(),
                description.orEmpty(),
                videoUrl.orEmpty(),
                draft.days
            )
        }
        val edited = draft.item
        act {
            if (edited == null) {
                val added = plans.add(new.copy(memberId = state.player.id))
                set { copy(items = items.orEmpty() + added, exercise = null) }
                toast(Res.string.exercise_added)
            } else {
                val item = new.copy(id = edited.id, memberId = edited.memberId)
                plans.update(item)
                set { copy(items = items?.map { if (it.id == item.id) item else it }, exercise = null) }
                toast(Res.string.exercise_updated)
            }
        }
    }

    /** The first tap asks to confirm, the second removes. */
    fun onRemove() {
        val draft = state.exercise ?: return
        val item = draft.item ?: return
        if (state.busy) return
        if (!draft.confirmingRemoval) return updateExercise { copy(confirmingRemoval = true) }
        act {
            plans.remove(item)
            set {
                copy(
                    items = items?.filter { it.id != item.id },
                    checks = checks.filter { it.planItemId != item.id }.toSet(),
                    exercise = null
                )
            }
            toast(Res.string.exercise_removed)
        }
    }

    /** Once the week is loaded; into one with exercises only on a second tap, and only if there is something to copy. */
    fun onCopy() {
        val current = state.items ?: return
        if (state.busy) return
        val week = state.week
        act {
            if (current.isNotEmpty() && !state.confirmingCopy) {
                if (plans.items(state.player, week.minus(DatePeriod(days = 7))).isEmpty()) {
                    toast(Res.string.plan_copy_empty)
                } else {
                    set { copy(confirmingCopy = this.week == week) }
                }
                return@act
            }
            // Success or not, the next copy asks again.
            set { copy(confirmingCopy = false) }
            val copies = try {
                plans.copyPreviousWeek(state.player, week, current.size)
            } catch (_: PlanChangedException) {
                set { copy(items = null) }
                load()
                throw CustomException(0, getString(Res.string.plan_changed), null)
            }
            if (state.week == week) set { copy(items = current + copies) }
            toast(if (copies.isEmpty()) Res.string.plan_copy_empty else Res.string.plan_copied)
        }
    }

    fun onWriteNote() = update { copy(noteDraft = note.orEmpty()) }

    fun onDismissNote() = update { copy(noteDraft = null) }

    fun onNoteChanged(text: String) = update { copy(noteDraft = text.take(WEEKLY_NOTE_MAX)) }

    /** Trimmed; emptied, the note goes. */
    fun onSaveNote() {
        val text = state.noteDraft ?: return
        if (state.busy) return
        val new = text.trim().ifEmpty { null }
        if (new == state.note) return onDismissNote()
        act {
            plans.saveNote(state.player, state.week, new)
            set { copy(note = new, noteDraft = null) }
            toast(Res.string.note_saved)
        }
    }

    /** Only once saved: a failure leaves the dot as it was. */
    fun onToggle(check: PlanCheck) {
        if (state.busy) return
        act {
            if (check in state.checks) {
                plans.uncheck(check)
                set { copy(checks = checks - check) }
            } else {
                plans.check(check)
                set { copy(checks = checks + check) }
            }
        }
    }

    private fun updateExercise(change: ExerciseDraft.() -> ExerciseDraft) = update {
        copy(exercise = exercise?.change())
    }
}
