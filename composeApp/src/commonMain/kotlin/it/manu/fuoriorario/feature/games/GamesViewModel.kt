package it.manu.fuoriorario.feature.games

import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.game_saved
import it.manu.fuoriorario.core.error.ErrorManager
import it.manu.fuoriorario.core.today
import it.manu.fuoriorario.core.viewmodel.DispatcherProvider
import it.manu.fuoriorario.core.viewmodel.ScreenModel
import it.manu.fuoriorario.domain.CallUp
import it.manu.fuoriorario.domain.GAME_NOTE_MAX
import it.manu.fuoriorario.domain.Game
import it.manu.fuoriorario.domain.GameDraft
import it.manu.fuoriorario.domain.GameError
import it.manu.fuoriorario.domain.GameEvent
import it.manu.fuoriorario.domain.GameEventType
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.OPPONENT_MAX
import it.manu.fuoriorario.domain.Quarter
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.domain.gameError
import it.manu.fuoriorario.domain.rosterOrder
import it.manu.fuoriorario.feature.games.data.GameDraftStore
import it.manu.fuoriorario.feature.games.data.GameRepository
import it.manu.fuoriorario.feature.roster.data.RosterRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate

/** "Nuova partita" as typed; [callUps] are member ids. */
data class GameForm(
    val date: LocalDate,
    val opponent: String = "",
    val home: Boolean = true,
    val note: String = "",
    val callUps: Set<String> = emptySet(),
    val error: GameError? = null
)

/**
 * [games] is null until loaded; [players] (staff only) are who can be called up. [draft] is the partita in corso, if
 * any, shown in Live while [live]. [confirmingAbandon] took the first tap of "Abbandona". [busy]: the save is in flight.
 * [loadFailed]: the list didn't load with a game in progress, which stays reachable.
 */
data class GamesScreenState(
    val games: List<Game>? = null,
    val players: List<Member> = emptyList(),
    val draft: GameDraft? = null,
    val form: GameForm? = null,
    val live: Boolean = false,
    val confirmingAbandon: Boolean = false,
    val busy: Boolean = false,
    val loadFailed: Boolean = false
) {
    /** The form or Live: a second-level screen, without the tab bar. */
    val secondLevel get() = form != null || live
}

/** Partite (PRD F8): the team's games; staff create one, record it live and save it at the end (ADR 0010). */
class GamesViewModel(
    private val staff: Boolean,
    private val repository: GameRepository,
    private val roster: RosterRepository,
    private val drafts: GameDraftStore,
    private val dispatchers: DispatcherProvider,
    errorManager: ErrorManager
) : ScreenModel<GamesScreenState>(
    GamesScreenState(draft = drafts.draft.value),
    dispatchers,
    errorManager,
    busy = { copy(busy = it) }
) {
    init {
        load()
    }

    /** "Riprova" under the list, when it failed with a game in progress on screen. */
    fun onRetry() = load()

    /**
     * On main, like every keystroke: a background `set` could drop one typed meanwhile. With a game in progress a
     * failure leaves the list empty instead of the error screen: Live works without network (ADR 0010).
     */
    private fun load() = defaultLaunch(dispatchers.main()) {
        val loaded = try {
            val games = repository.games()
            games to if (staff) roster.members().filter { it.role == Role.PLAYER }.rosterOrder() else emptyList()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (state.draft == null) throw e
            null
        }
        set {
            loaded?.let { (games, players) -> copy(games = games, players = players, loadFailed = false) }
                ?: copy(loadFailed = true)
        }
    }

    /** Only one game at a time: with a draft it's "Riprendi" or "Abbandona". */
    fun onNew() = update { if (draft != null) this else copy(form = GameForm(today())) }

    fun onCancelForm() = update { copy(form = null) }

    fun onDateChanged(date: LocalDate) = updateForm { copy(date = date) }

    fun onOpponentChanged(opponent: String) = updateForm { copy(opponent = opponent.take(OPPONENT_MAX)) }

    fun onHomeChanged(home: Boolean) = updateForm { copy(home = home) }

    fun onNoteChanged(note: String) = updateForm { copy(note = note.take(GAME_NOTE_MAX)) }

    fun onCallUpToggled(id: String, on: Boolean) = updateForm { copy(callUps = if (on) callUps + id else callUps - id) }

    @OptIn(ExperimentalUuidApi::class)
    fun onStart() = update {
        val form = form ?: return@update this
        val error = gameError(form.opponent)
        if (error != null) return@update copy(form = form.copy(error = error))
        val draft = GameDraft(
            Uuid.random().toString(),
            form.date,
            form.opponent.trim(),
            form.home,
            form.note.trim().ifEmpty { null },
            players.filter { it.id in form.callUps }.map { CallUp(it.id!!, it.displayName, it.jerseyNumber) }
        )
        drafts.save(draft)
        copy(form = null, draft = draft, live = true)
    }

    fun onResume() = update { copy(live = draft != null, confirmingAbandon = false) }

    /** Back from Live: the draft stays, to resume. */
    fun onLeaveLive() = update { copy(live = false) }

    /** The first tap asks to confirm, the second forgets the draft. */
    fun onAbandon() = update {
        when {
            draft == null -> this
            !confirmingAbandon -> copy(confirmingAbandon = true)
            else -> {
                drafts.save(null)
                copy(draft = null, confirmingAbandon = false)
            }
        }
    }

    /** The convocato the next shots and free throws go to: they stay selected. */
    fun onSelect(id: String) = editDraft { copy(selected = id) }

    fun onQuarter(quarter: Quarter) = editDraft { copy(quarter = quarter) }

    fun onShot(zone: Zone, made: Boolean) = addEvent { GameEvent(GameEventType.SHOT, quarter, it, zone, made) }

    fun onFreeThrow(made: Boolean) = addEvent { GameEvent(GameEventType.FREE_THROW, quarter, it, made = made) }

    fun onOpponentScored(points: Int) =
        editDraft { copy(events = events + GameEvent(GameEventType.OPPONENT, quarter, value = points)) }

    fun onUndo() = editDraft { copy(events = events.dropLast(1)) }

    fun onRemoveEvent(index: Int) = editDraft { copy(events = events.filterIndexed { i, _ -> i != index }) }

    /** "Termina partita": saved, it joins the list; failed, the draft stays as it was to try again. */
    fun onFinish() {
        val draft = state.draft ?: return
        if (state.busy) return
        act {
            val saved = repository.save(draft)
            drafts.save(null)
            set { copy(games = listOf(saved) + games.orEmpty(), draft = null, live = false) }
            toast(Res.string.game_saved)
        }
    }

    /** [event] of the selected convocato; without a selection nothing happens. */
    private fun addEvent(event: GameDraft.(String) -> GameEvent) = editDraft {
        selected?.let { copy(events = events + event(it)) } ?: this
    }

    /** Every change is copied to the device at once (ADR 0010); none while the save is in flight. */
    private fun editDraft(change: GameDraft.() -> GameDraft) = update {
        val draft = draft ?: return@update this
        if (busy) return@update this
        val changed = draft.change()
        if (changed != draft) drafts.save(changed)
        copy(draft = changed, confirmingAbandon = false)
    }

    private fun updateForm(change: GameForm.() -> GameForm) = update { copy(form = form?.change()?.copy(error = null)) }
}
