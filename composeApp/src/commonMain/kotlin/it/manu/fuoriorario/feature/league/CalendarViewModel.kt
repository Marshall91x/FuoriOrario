package it.manu.fuoriorario.feature.league

import it.manu.fuoriorario.core.ROME
import it.manu.fuoriorario.core.error.ErrorManager
import it.manu.fuoriorario.core.today
import it.manu.fuoriorario.core.viewmodel.DispatcherProvider
import it.manu.fuoriorario.core.viewmodel.ScreenModel
import it.manu.fuoriorario.domain.League
import it.manu.fuoriorario.domain.Period
import it.manu.fuoriorario.domain.Round
import it.manu.fuoriorario.domain.nextRound
import it.manu.fuoriorario.domain.ours
import it.manu.fuoriorario.domain.rounds
import it.manu.fuoriorario.feature.league.data.LeagueRepository
import kotlin.time.Clock
import kotlinx.datetime.atStartOfDayIn

/** Leagues shown are the season's: the latest created from 1 September on. */
internal fun seasonStart() = Period.SEASON.start(today()).atStartOfDayIn(ROME)

/**
 * [league] null: none imported this season. [ours] is "Nostre" (only our fixtures) against "Tutto il girone";
 * [rounds] are what shows, [open] the one with the next fixture.
 */
data class CalendarScreenState(
    val league: League? = null,
    val ours: Boolean = true,
    val rounds: List<Round> = emptyList(),
    val open: Int = 0
)

/** Partite → Calendario (ADR 0011): the league's fixtures by round, for the whole team. */
class CalendarViewModel(
    private val repository: LeagueRepository,
    dispatchers: DispatcherProvider,
    errorManager: ErrorManager
) : ScreenModel<CalendarScreenState>(CalendarScreenState(), dispatchers, errorManager, busy = { this }) {
    init {
        defaultLaunch(dispatchers.main()) {
            val league = repository.league(seasonStart())
            set { show(league, ours) }
        }
    }

    fun onOursChanged(ours: Boolean) = update { show(league, ours) }

    private fun CalendarScreenState.show(league: League?, ours: Boolean): CalendarScreenState {
        val all = league?.rounds().orEmpty()
        val rounds = if (ours) all.ours(league?.ours) else all
        return copy(league = league, ours = ours, rounds = rounds, open = rounds.nextRound(Clock.System.now()))
    }
}
