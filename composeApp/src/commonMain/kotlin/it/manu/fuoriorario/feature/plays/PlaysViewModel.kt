package it.manu.fuoriorario.feature.plays

import androidx.lifecycle.viewModelScope
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.play_removed
import fuoriorario.composeapp.generated.resources.play_saved
import fuoriorario.composeapp.generated.resources.play_unsaved
import it.manu.fuoriorario.core.error.ErrorManager
import it.manu.fuoriorario.core.viewmodel.DispatcherProvider
import it.manu.fuoriorario.core.viewmodel.ScreenModel
import it.manu.fuoriorario.domain.CourtSize
import it.manu.fuoriorario.domain.MoveKind
import it.manu.fuoriorario.domain.PLAY_DESCRIPTION_MAX
import it.manu.fuoriorario.domain.PLAY_TITLE_MAX
import it.manu.fuoriorario.domain.Play
import it.manu.fuoriorario.domain.PlayCategory
import it.manu.fuoriorario.domain.PlayError
import it.manu.fuoriorario.domain.Point
import it.manu.fuoriorario.domain.STEPS_MAX
import it.manu.fuoriorario.domain.STEP_NOTE_MAX
import it.manu.fuoriorario.domain.Step
import it.manu.fuoriorario.domain.bent
import it.manu.fuoriorario.domain.cleaned
import it.manu.fuoriorario.domain.isDefender
import it.manu.fuoriorario.domain.moves
import it.manu.fuoriorario.domain.nearest
import it.manu.fuoriorario.domain.playError
import it.manu.fuoriorario.domain.startingStep
import it.manu.fuoriorario.domain.with
import it.manu.fuoriorario.domain.withDefense
import it.manu.fuoriorario.domain.within
import it.manu.fuoriorario.domain.without
import it.manu.fuoriorario.feature.plays.data.PlayRepository
import kotlinx.coroutines.launch

/** A handle's key among the drag targets: the piece whose move it bends, marked. */
const val HANDLE = "~"

/** How near an attacker a hold must be to give him the ball, in viewBox units: about two pieces. */
private const val HOLD_REACH = 40f

/** How many changes ↶ can take back. */
private const val UNDO_MAX = 50

/**
 * The editor as drawn (ADR 0008): [play] is the draft of [original], without id for a new one; [index] is the step on
 * screen. [confirmingExit] and [confirmingRemoval] took the first tap; any change takes them back.
 * [history] is the play and step before each change ↶ can take back, the last one last; [dragStarted]: a drag began and
 * hasn't changed anything yet, its first move goes in the history.
 */
data class PlayDraft(
    val original: Play,
    val play: Play = original,
    val index: Int = 0,
    val error: PlayError? = null,
    val confirmingExit: Boolean = false,
    val confirmingRemoval: Boolean = false,
    val history: List<Pair<Play, Int>> = emptyList(),
    val dragStarted: Boolean = false
) {
    val isNew get() = original.id == null
    val unsaved get() = play != original
    val step get() = play.steps[index]
    val prev get() = play.steps.getOrNull(index - 1)

    /** The pieces, and a handle halfway along each move to bend it. */
    val targets: Map<String, Point>
        get() = step.pos + moves(prev, step)
            .filter { it.kind != MoveKind.PASS && it.from != it.to }
            .associate { "$HANDLE${it.piece}" to it.at(0.5f) }
}

/**
 * [plays] is null until loaded. [openId] is the play in the viewer; [draft] is the editor, over the viewer or the list.
 * [busy]: a save or removal is in flight.
 */
data class PlaysScreenState(
    val plays: List<Play>? = null,
    val openId: String? = null,
    val draft: PlayDraft? = null,
    val busy: Boolean = false
) {
    val open get() = plays?.find { it.id == openId }

    /** A play or the editor is open: a second-level screen, without the tab bar. */
    val secondLevel get() = openId != null || draft != null
}

/** Schemi (PRD F5): the team's plays, and staff's editor with save and remove. */
class PlaysViewModel(
    private val repository: PlayRepository,
    private val dispatchers: DispatcherProvider,
    errorManager: ErrorManager
) : ScreenModel<PlaysScreenState>(
    PlaysScreenState(),
    dispatchers,
    errorManager,
    busy = { copy(busy = it) }
) {
    init {
        // On main, like every keystroke: a background `set` could drop one typed meanwhile.
        defaultLaunch(dispatchers.main()) {
            val plays = repository.plays()
            set { copy(plays = plays) }
        }
    }

    fun onOpen(play: Play) = update { copy(openId = play.id) }

    fun onBack() = update { copy(openId = null) }

    fun onNew() = update {
        val play =
            Play("", PlayCategory.ATTACK, court = CourtSize.HALF, defense = false, steps = listOf(startingStep(false)))
        copy(draft = PlayDraft(play))
    }

    fun onEdit() = update { copy(draft = open?.let { PlayDraft(it) }) }

    fun onTitleChanged(title: String) = change { copy(title = title.take(PLAY_TITLE_MAX)) }

    fun onCategoryChanged(category: PlayCategory) = change { copy(category = category) }

    fun onDescriptionChanged(description: String) = change {
        copy(description = description.take(PLAY_DESCRIPTION_MAX))
    }

    fun onCourtChanged(court: CourtSize) = change { copy(court = court) }

    fun onDefenseChanged(on: Boolean) = change(record = true) {
        copy(defense = on, steps = steps.map { it.withDefense(on) })
    }

    fun onPrevStep() = updateDraft { copy(index = (index - 1).coerceAtLeast(0)) }

    fun onNextStep() = updateDraft { copy(index = (index + 1).coerceAtMost(play.steps.lastIndex)) }

    /**
     * [target], a piece or a [HANDLE], dragged to [at]: the piece moves, the handle bends its move. The passer or the
     * receiver moved after the ball was given: the pass goes first (#98).
     */
    fun onPieceMoved(target: String, at: Point) = editStep({ dragStarted }) { draft, step ->
        val spot = at.within(draft.play.court)
        val piece = target.removePrefix(HANDLE)
        val from = draft.prev?.pos?.get(piece)
        when {
            !target.startsWith(HANDLE) -> step.copy(
                pos = step.pos + (piece to spot),
                passLast = step.passLast && piece != draft.prev?.ball && piece != step.ball
            )
            from != null -> step.bent(piece, from, spot)
            else -> step
        }
    }

    /**
     * A hold at [at]: the ball to the attacker under the finger, if any. Given after the passer or the receiver moved,
     * the pass comes last, as the coach draws it on paper (#98).
     */
    fun onHold(at: Point) = editStep { draft, step ->
        nearest(step.pos.filterKeys { !isDefender(it) }, at, HOLD_REACH)?.let { ball ->
            val prev = draft.prev
            val moved = prev != null && listOf(prev.ball, ball).any { prev.pos[it] != step.pos[it] }
            step.copy(ball = ball, passLast = moved && prev?.ball != ball)
        } ?: step
    }

    fun onScreenToggled(piece: String, on: Boolean) =
        editStep { _, step -> step.copy(screens = if (on) step.screens + piece else step.screens - piece) }

    /** A drag begins: one entry in the history for all of it. */
    fun onDragStart() = updateDraft { copy(dragStarted = true) }

    fun onNoteChanged(note: String) = editStep({ false }) { _, step -> step.copy(note = note.take(STEP_NOTE_MAX)) }

    /** A copy of where everyone stands, ready to move, after the step on screen. */
    fun onAddStep() = update {
        val draft = draft ?: return@update this
        if (draft.play.steps.size >= STEPS_MAX) return@update this
        val steps = draft.play.steps.toMutableList().also {
            it.add(draft.index + 1, Step(draft.step.pos, draft.step.ball))
        }
        copy(draft = draft.recorded().changed(draft.play.copy(steps = steps)).copy(index = draft.index + 1))
    }

    fun onRemoveStep() = update {
        val draft = draft ?: return@update this
        if (draft.play.steps.size <= 1) return@update this
        val steps = draft.play.steps.without(draft.index)
        copy(
            draft = draft.recorded().changed(
                draft.play.copy(steps = steps)
            ).copy(index = draft.index.coerceAtMost(steps.lastIndex))
        )
    }

    /**
     * The last change back, on the step where it happened. The text fields aren't in the history and stay as typed: the
     * notes too, unless a step comes or goes.
     */
    fun onUndo() = updateDraft {
        val (was, at) = history.lastOrNull() ?: return@updateDraft this
        val steps = if (was.steps.size == play.steps.size) {
            was.steps.zip(play.steps) { old, now -> old.copy(note = now.note) }
        } else {
            was.steps
        }
        changed(play.copy(court = was.court, defense = was.defense, steps = steps))
            .copy(index = at, history = history.dropLast(1))
    }

    /** "Annulla" or the system back: with changes, the first tap only warns. */
    fun onLeave() {
        val draft = state.draft ?: return
        if (!draft.unsaved || draft.confirmingExit) return update { copy(draft = null) }
        updateDraft { copy(confirmingExit = true) }
        viewModelScope.launch(dispatchers.unconfined()) { toast(Res.string.play_unsaved) }
    }

    /** Saved, it opens in the viewer. */
    fun onSave() {
        val draft = state.draft ?: return
        if (state.busy) return
        val error = playError(draft.play)
        updateDraft { copy(error = error) }
        if (error != null) return
        val play = draft.play.cleaned()
        act {
            val saved = if (play.id == null) repository.add(play) else play.also { repository.update(it) }
            set {
                val list = plays.orEmpty()
                val updated = if (play.id == null) list + saved else list.map { if (it.id == saved.id) saved else it }
                copy(plays = updated, draft = null, openId = saved.id)
            }
            toast(Res.string.play_saved)
        }
    }

    /** The first tap asks to confirm, the second removes. */
    fun onRemove() {
        val draft = state.draft ?: return
        if (state.busy || draft.isNew) return
        if (!draft.confirmingRemoval) return updateDraft { copy(confirmingRemoval = true) }
        act {
            repository.remove(draft.original)
            set { copy(plays = plays.orEmpty().filter { it.id != draft.original.id }, draft = null, openId = null) }
            toast(Res.string.play_removed)
        }
    }

    private fun PlayDraft.changed(to: Play) =
        copy(play = to, confirmingExit = false, confirmingRemoval = false, dragStarted = false)

    private fun PlayDraft.recorded() = copy(history = (history + (play to index)).takeLast(UNDO_MAX))

    private fun change(record: Boolean = false, edit: Play.() -> Play) =
        updateDraft { (if (record) recorded() else this).changed(play.edit()) }

    /** A hold or drag that leaves the step as it was isn't a change: the first tap of a confirmation stays. */
    private fun editStep(record: PlayDraft.() -> Boolean = { true }, edit: (PlayDraft, Step) -> Step) = updateDraft {
        val edited = edit(this, step)
        if (edited == step) return@updateDraft this
        (if (record()) recorded() else this).changed(play.copy(steps = play.steps.with(index, edited)))
    }

    private fun updateDraft(change: PlayDraft.() -> PlayDraft) = update { copy(draft = draft?.change()) }
}
