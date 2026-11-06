package it.manu.fuoriorario.feature.league

import it.manu.fuoriorario.core.error.AppErrorManager
import it.manu.fuoriorario.core.error.FallbackHandler
import it.manu.fuoriorario.core.error.NetworkErrorHandler
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.Fixture
import it.manu.fuoriorario.domain.League
import it.manu.fuoriorario.domain.LeagueTeam
import it.manu.fuoriorario.domain.Leg
import it.manu.fuoriorario.testing.TestDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val now = Clock.System.now()
    private val us = LeagueTeam("a", "Artemis", ours = true)
    private val vasto = LeagueTeam("b", "Vasto")
    private val atessa = LeagueTeam("c", "Atessa")
    private val league = League(
        "l",
        "U19",
        listOf(us, vasto, atessa),
        listOf(
            Fixture("1", 1, Leg.FIRST, now - 7.days, "a", "b"),
            Fixture("2", 2, Leg.FIRST, now + 1.days, "b", "c"),
            Fixture("3", 3, Leg.FIRST, now + 7.days, "c", "a")
        )
    )
    private val repository = FakeLeagueRepository(league)

    private fun vm() = CalendarViewModel(
        repository,
        TestDispatcherProvider(dispatcher),
        AppErrorManager(listOf(NetworkErrorHandler(), FallbackHandler())) {}
    )

    private val CalendarViewModel.data get() = uiState.value.data!!

    @Test
    fun load_showsOursOpenOnOurNextFixture() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        assertEquals(
            listOf(listOf("1"), emptyList(), listOf("3")),
            vm.data.rounds.map { r ->
                r.fixtures.map { it.number }
            }
        )
        assertEquals(2, vm.data.open)
        assertEquals(listOf(us), vm.data.rounds[1].resting)
    }

    @Test
    fun wholeLeague_showsEveryFixtureOpenOnTheNextOne() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onOursChanged(false)
        advanceUntilIdle()

        assertEquals(listOf("2"), vm.data.rounds[1].fixtures.map { it.number })
        assertEquals(1, vm.data.open)
    }

    @Test
    fun noLeague_showsNothing() = runTest(dispatcher) {
        repository.league = null
        val vm = vm()
        advanceUntilIdle()

        assertNull(vm.data.league)
        assertEquals(emptyList(), vm.data.rounds)
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

        assertEquals("U19", vm.data.league?.name)
    }
}
