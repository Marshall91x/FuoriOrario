package it.manu.fuoriorario.feature.team

import it.manu.fuoriorario.core.error.ErrorManager
import it.manu.fuoriorario.core.session.SelectedPlayer
import it.manu.fuoriorario.core.today
import it.manu.fuoriorario.core.viewmodel.ComposeViewModel
import it.manu.fuoriorario.core.viewmodel.DispatcherProvider
import it.manu.fuoriorario.core.viewmodel.UiState
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.OverviewRow
import it.manu.fuoriorario.domain.Period
import it.manu.fuoriorario.domain.overviewRow
import it.manu.fuoriorario.domain.progress
import it.manu.fuoriorario.domain.weekOf
import it.manu.fuoriorario.feature.plan.data.PlanRepository
import it.manu.fuoriorario.feature.shots.data.ShotRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** [rows] is null while the team loads: a row per player, in the roster's order. */
data class TeamOverviewScreenState(val rows: List<Pair<Member, OverviewRow>>? = null)

/** Quadro squadra (PRD F6): [players]' plan this week and recent shots; opening one makes staff follow them. */
class TeamOverviewViewModel(
    players: List<Member>,
    private val shots: ShotRepository,
    private val plans: PlanRepository,
    private val selectedPlayer: SelectedPlayer,
    dispatchers: DispatcherProvider,
    errorManager: ErrorManager
) : ComposeViewModel<TeamOverviewScreenState>(
    // The title is there before the numbers.
    defaultState = TeamOverviewScreenState().let { UiState(UseCaseMutableState.ShowData(it), it) },
    dispatcherProvider = dispatchers,
    errorManager = errorManager
) {
    init {
        defaultLaunch(dispatchers.main()) {
            val today = today()
            // Two requests for the whole team, however many players. In their own scope: a failed one would
            // otherwise cancel the load before it shows the error.
            val (week, sessions) = coroutineScope {
                val week = async { plans.teamWeek(weekOf(today)) }
                val sessions = async { shots.teamSessions(Period.DAYS_30.start(today)) }
                week.await() to sessions.await()
            }
            val (items, checks) = week
            val byPlayer = sessions.groupBy { it.memberId }
            val rows = players.map { player ->
                val plan = progress(items.filter { it.memberId == player.id }, checks)
                player to overviewRow(plan, byPlayer[player.id].orEmpty(), today)
            }
            emitSuccess(TeamOverviewScreenState(rows))
        }
    }

    /** Players come from the roster: they all have an id. */
    fun onOpen(player: Member) {
        player.id?.let(selectedPlayer::select)
    }
}
