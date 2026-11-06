package it.manu.fuoriorario.feature.league

import it.manu.fuoriorario.core.error.AppErrorManager
import it.manu.fuoriorario.core.error.FallbackHandler
import it.manu.fuoriorario.core.error.NetworkErrorHandler
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.Fixture
import it.manu.fuoriorario.domain.FixtureStatus
import it.manu.fuoriorario.domain.League
import it.manu.fuoriorario.domain.LeagueTeam
import it.manu.fuoriorario.domain.Leg
import it.manu.fuoriorario.testing.TestDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class StandingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val league = League(
        "l",
        "U19",
        listOf(LeagueTeam("a", "Artemis", ours = true), LeagueTeam("b", "Vasto"), LeagueTeam("c", "Atessa")),
        listOf(
            Fixture("1", 1, Leg.FIRST, null, "b", "a", null, FixtureStatus.PLAYED, 70, 60),
            Fixture("2", 2, Leg.FIRST, null, "a", "c", null, FixtureStatus.PLAYED, 80, 50)
        )
    )
    private val repository = FakeLeagueRepository(league)

    private fun vm() = StandingsViewModel(
        repository,
        TestDispatcherProvider(dispatcher),
        AppErrorManager(listOf(NetworkErrorHandler(), FallbackHandler())) {}
    )

    @Test
    fun load_showsTheLeaguesStandings() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        // a and b on 2 points: b won the game between them.
        assertEquals(listOf("b", "a", "c"), vm.uiState.value.data!!.standings.map { it.team.id })
    }

    @Test
    fun noLeague_showsNothing() = runTest(dispatcher) {
        repository.league = null
        val vm = vm()
        advanceUntilIdle()

        assertNull(vm.uiState.value.data!!.league)
        assertEquals(emptyList(), vm.uiState.value.data!!.standings)
    }

    @Test
    fun load_offline_showsErrorThenRetryLoads() = runTest(dispatcher) {
        repository.failLoad = true
        val vm = vm()
        advanceUntilIdle()

        val handler = assertIs<UseCaseMutableState.Error>(vm.uiState.value.state).handler
        assertIs<NetworkErrorHandler>(handler)
        handler.retryFunction!!()
        advanceUntilIdle()
        assertEquals(3, vm.uiState.value.data!!.standings.size)
    }
}
