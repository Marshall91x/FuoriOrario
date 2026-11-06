package it.manu.fuoriorario.feature.league

import it.manu.fuoriorario.core.error.ErrorManager
import it.manu.fuoriorario.core.viewmodel.ComposeViewModel
import it.manu.fuoriorario.core.viewmodel.DispatcherProvider
import it.manu.fuoriorario.core.viewmodel.UiState
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.League
import it.manu.fuoriorario.domain.Standing
import it.manu.fuoriorario.domain.standings
import it.manu.fuoriorario.feature.league.data.LeagueRepository

/** [league] null: none imported this season. */
data class StandingsScreenState(val league: League? = null, val standings: List<Standing> = emptyList())

/** Partite → Classifica: computed from the league's results, for the whole team. Read only. */
class StandingsViewModel(repository: LeagueRepository, dispatchers: DispatcherProvider, errorManager: ErrorManager) :
    ComposeViewModel<StandingsScreenState>(
        defaultState = UiState(UseCaseMutableState.Loading, StandingsScreenState()),
        dispatcherProvider = dispatchers,
        errorManager = errorManager
    ) {
    init {
        defaultLaunch(dispatchers.main()) {
            val league = repository.league(seasonStart())
            emitSuccess(StandingsScreenState(league, league?.standings().orEmpty()))
        }
    }
}
