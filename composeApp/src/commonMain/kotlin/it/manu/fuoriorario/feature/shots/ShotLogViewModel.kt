package it.manu.fuoriorario.feature.shots

import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.session_saved
import fuoriorario.composeapp.generated.resources.shots_deleted
import it.manu.fuoriorario.core.error.ErrorManager
import it.manu.fuoriorario.core.today
import it.manu.fuoriorario.core.viewmodel.DispatcherProvider
import it.manu.fuoriorario.core.viewmodel.ScreenModel
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.NOTE_MAX
import it.manu.fuoriorario.domain.SessionError
import it.manu.fuoriorario.domain.ShotSession
import it.manu.fuoriorario.domain.Shots
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.domain.newSession
import it.manu.fuoriorario.domain.sessionError
import it.manu.fuoriorario.feature.shots.data.ShotRepository
import kotlinx.coroutines.async
import kotlinx.datetime.LocalDate

/** The "Registra sessione" form as typed: [error] is the last failed check, shown in the sheet. */
data class SessionDraft(
    val date: LocalDate,
    val zones: Map<Zone, Shots> = Zone.entries.associateWith { Shots() },
    val note: String = "",
    val error: SessionError? = null
)

/**
 * [sessions] and [refs] are null until loaded. [draft] is the open sheet, null when closed.
 * [confirmingDelete] took the first "Elimina" tap; [busy]: a save or delete is in flight.
 */
data class ShotLogScreenState(
    val player: Member,
    val today: LocalDate,
    val sessions: List<ShotSession>? = null,
    val refs: Map<Zone, Int>? = null,
    val draft: SessionDraft? = null,
    val confirmingDelete: ShotSession? = null,
    val busy: Boolean = false
)

/** [player]'s Diario di tiro (PRD F3): their sessions and the team's riferimenti, with log and delete. */
class ShotLogViewModel(
    player: Member,
    private val shots: ShotRepository,
    private val dispatchers: DispatcherProvider,
    errorManager: ErrorManager
) : ScreenModel<ShotLogScreenState>(
    // The header is there before the sessions: they fill in when loaded.
    ShotLogScreenState(player, today()),
    dispatchers,
    errorManager,
    busy = { copy(busy = it) }
) {
    init {
        load()
    }

    // On main, like every keystroke: a background `set` could drop one typed meanwhile. The calls suspend, never block.
    private fun load() = defaultLaunch(dispatchers.main()) {
        val teamRefs = async { shots.zoneRefs() }
        val sessions = shots.sessions(state.player)
        val refs = teamRefs.await()
        set { copy(sessions = sessions, refs = refs) }
    }

    fun onLog() = update { copy(draft = SessionDraft(today), confirmingDelete = null) }

    fun onDismissLog() = update { copy(draft = null) }

    fun onDateChanged(date: LocalDate) = updateDraft { copy(date = date) }

    fun onZoneChanged(zone: Zone, shots: Shots) = updateDraft { copy(zones = zones + (zone to shots)) }

    fun onNoteChanged(note: String) = updateDraft { copy(note = note.take(NOTE_MAX)) }

    fun onSave() {
        val draft = state.draft ?: return
        if (state.busy) return
        val error = sessionError(draft.zones)
        updateDraft { copy(error = error) }
        if (error != null) return
        val session = newSession(draft.date, draft.zones, draft.note).copy(memberId = state.player.id)
        act {
            val saved = shots.add(session)
            // Stable sort: the new one goes first among sessions of the same day.
            set { copy(sessions = (listOf(saved) + sessions.orEmpty()).sortedByDescending { it.date }, draft = null) }
            toast(Res.string.session_saved)
        }
    }

    /** The first tap asks to confirm, the second deletes. */
    fun onDelete(session: ShotSession) {
        if (state.busy) return
        if (state.confirmingDelete != session) return update { copy(confirmingDelete = session) }
        update { copy(confirmingDelete = null) }
        act {
            shots.delete(session)
            set { copy(sessions = sessions.orEmpty().filter { it.id != session.id }) }
            toast(Res.string.shots_deleted)
        }
    }

    private fun updateDraft(change: SessionDraft.() -> SessionDraft) = update { copy(draft = draft?.change()) }
}
