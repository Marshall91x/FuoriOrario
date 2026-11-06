package it.manu.fuoriorario.feature.league

import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.league_deleted
import fuoriorario.composeapp.generated.resources.league_imported
import it.manu.fuoriorario.core.error.ErrorManager
import it.manu.fuoriorario.core.viewmodel.DispatcherProvider
import it.manu.fuoriorario.core.viewmodel.ScreenModel
import it.manu.fuoriorario.domain.Fixture
import it.manu.fuoriorario.domain.ImportPreview
import it.manu.fuoriorario.domain.League
import it.manu.fuoriorario.domain.missing
import it.manu.fuoriorario.domain.reimports
import it.manu.fuoriorario.feature.league.data.LeagueRepository
import it.manu.fuoriorario.feature.league.data.UnreadableCalendarException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * [league] is this season's, if any. [preview] is the PDF read, to confirm into league [previewId]: the one shown
 * when reimporting it, else a new id made once, so a confirm sent again after a lost answer adds nothing. [ours] is
 * the team picked as ours. [unreadable]: the last PDF wasn't a FIP calendar. [confirmingDelete] took the first tap.
 */
data class LeagueScreenState(
    val league: League? = null,
    val preview: ImportPreview? = null,
    val previewId: String? = null,
    val ours: String? = null,
    val unreadable: Boolean = false,
    val confirmingDelete: Boolean = false,
    val busy: Boolean = false
) {
    /** On a reimport, the fixtures no longer in the PDF: they stay. */
    val missing: List<Fixture> get() = preview?.missing(league).orEmpty()
}

/** Squadra → Impostazioni → Campionato (ADR 0011): staff import the FIP PDF, confirm the preview, delete the league. */
class LeagueViewModel(
    private val repository: LeagueRepository,
    dispatchers: DispatcherProvider,
    errorManager: ErrorManager
) : ScreenModel<LeagueScreenState>(LeagueScreenState(), dispatchers, errorManager, busy = { copy(busy = it) }) {
    init {
        defaultLaunch(dispatchers.main()) {
            val league = repository.league(seasonStart())
            set { copy(league = league) }
        }
    }

    /** The PDF picked, [read] off the device here: the Edge Function reads it, nothing is written yet. */
    @OptIn(ExperimentalUuidApi::class)
    fun onPdfPicked(read: suspend () -> ByteArray) {
        if (state.busy) return
        update { copy(unreadable = false, confirmingDelete = false) }
        act {
            val preview = try {
                repository.preview(read())
            } catch (_: UnreadableCalendarException) {
                set { copy(unreadable = true, preview = null) }
                return@act
            }
            set {
                copy(
                    preview = preview,
                    previewId = if (preview.reimports(league)) league!!.id else Uuid.random().toString(),
                    ours = league?.ours?.name?.takeIf { it in preview.teams }
                )
            }
        }
    }

    fun onOursPicked(name: String) = update { copy(ours = name) }

    fun onCancelPreview() = update { copy(preview = null, previewId = null, ours = null) }

    fun onConfirm() {
        val preview = state.preview ?: return
        val id = state.previewId ?: return
        val ours = state.ours ?: return
        if (state.busy) return
        act {
            repository.confirm(id, preview, ours)
            val league = repository.league(seasonStart())
            set { copy(league = league, preview = null, previewId = null, ours = null) }
            toast(Res.string.league_imported)
        }
    }

    /** "Elimina", then "Conferma": recorded games stay. */
    fun onDelete() {
        val league = state.league ?: return
        if (!state.confirmingDelete) return update { copy(confirmingDelete = true) }
        if (state.busy) return
        act {
            repository.delete(league)
            set { copy(league = null, confirmingDelete = false) }
            toast(Res.string.league_deleted)
        }
    }
}
