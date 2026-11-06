package it.manu.fuoriorario.feature.league

import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.fixture_deleted
import fuoriorario.composeapp.generated.resources.fixture_saved
import fuoriorario.composeapp.generated.resources.save_failed
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.getString

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

    private val first get() = repository.league!!.fixtures.first { it.number == "1" }

    private fun TestScope.loaded() = vm().also { advanceUntilIdle() }

    @Test
    fun edit_opensTheDraftInRomeTime() = runTest(dispatcher) {
        repository.league = league.copy(
            fixtures = listOf(Fixture("1", 1, Leg.FIRST, Instant.parse("2026-11-09T17:30:00Z"), "a", "b", "Pala"))
        )
        val vm = loaded()
        vm.onEdit(first)
        advanceUntilIdle()

        val draft = assertNotNull(vm.data.editing)
        assertEquals("1", draft.number)
        assertEquals("9/11/2026", draft.date)
        assertEquals("18:30", draft.time)
        assertEquals("Pala", draft.venue)
    }

    @Test
    fun save_played_withoutResult_saysSoAndWritesNothing() = runTest(dispatcher) {
        val vm = loaded()
        vm.onEdit(first)
        vm.onDraftChanged(vm.data.editing!!.copy(status = FixtureStatus.PLAYED, homeScore = "20"))
        vm.onSave()
        advanceUntilIdle()

        assertEquals(FixtureError.RESULT, vm.data.error)
        assertEquals(FixtureStatus.SCHEDULED, first.status)
    }

    @Test
    fun save_played_writesTheResultAndShowsIt() = runTest(dispatcher) {
        val vm = loaded()
        vm.onEdit(first)
        vm.onDraftChanged(vm.data.editing!!.copy(status = FixtureStatus.PLAYED, homeScore = "20", awayScore = "0"))
        vm.onSave()
        advanceUntilIdle()

        assertEquals(Triple(FixtureStatus.PLAYED, 20, 0), Triple(first.status, first.homeScore, first.awayScore))
        assertEquals(first, vm.data.rounds[0].fixtures.single())
        assertNull(vm.data.editing)
        assertEquals(getString(Res.string.fixture_saved), vm.toasts.first())
    }

    @Test
    fun save_postponed_dropsTheDate() = runTest(dispatcher) {
        val vm = loaded()
        vm.onEdit(first)
        vm.onDraftChanged(vm.data.editing!!.copy(status = FixtureStatus.POSTPONED))
        vm.onSave()
        advanceUntilIdle()

        assertEquals(FixtureStatus.POSTPONED, first.status)
        assertNull(first.startsAt)
    }

    @Test
    fun save_scheduled_withoutValidDate_saysSo() = runTest(dispatcher) {
        val vm = loaded()
        vm.onEdit(first)
        vm.onDraftChanged(vm.data.editing!!.copy(date = "31/2/2026"))
        vm.onSave()
        advanceUntilIdle()

        assertEquals(FixtureError.DATE, vm.data.error)
    }

    @Test
    fun save_moved_changesDateAndTime() = runTest(dispatcher) {
        val vm = loaded()
        vm.onEdit(first)
        vm.onDraftChanged(vm.data.editing!!.copy(date = "10.12.26", time = "20.15"))
        vm.onSave()
        advanceUntilIdle()

        assertEquals(Instant.parse("2026-12-10T19:15:00Z"), first.startsAt)
    }

    @Test
    fun add_sameTeams_saysSoThenAddsToTheRound() = runTest(dispatcher) {
        val vm = loaded()
        vm.onAdd()
        val draft = vm.data.editing!!
        assertTrue(draft.new)
        vm.onDraftChanged(draft.copy(round = 2, homeId = "a", awayId = "a", date = "1/12/2026", time = "18:00"))
        vm.onSave()
        advanceUntilIdle()
        assertEquals(FixtureError.TEAMS, vm.data.error)

        vm.onDraftChanged(vm.data.editing!!.copy(awayId = "c", venue = " Pala "))
        vm.onSave()
        advanceUntilIdle()

        val added = repository.league!!.fixtures.single { it.number == draft.number }
        assertEquals(listOf(2, "a", "c", "Pala"), listOf(added.round, added.homeId, added.awayId, added.venue))
        assertEquals(FixtureStatus.SCHEDULED, added.status)
        assertNull(vm.data.error)
    }

    @Test
    fun save_offline_toastsAndKeepsTheSheet() = runTest(dispatcher) {
        val vm = loaded()
        vm.onEdit(first)
        vm.onDraftChanged(vm.data.editing!!.copy(venue = "Nuova"))
        repository.failNext = true
        vm.onSave()
        advanceUntilIdle()

        assertEquals(getString(Res.string.save_failed), vm.toasts.first())
        assertEquals("Nuova", vm.data.editing?.venue)
        assertEquals(null, first.venue)
    }

    @Test
    fun delete_asksThenDeletes() = runTest(dispatcher) {
        val vm = loaded()
        vm.onEdit(first)
        vm.onDelete()
        advanceUntilIdle()
        assertTrue(vm.data.confirmingDelete)
        assertEquals(3, repository.league!!.fixtures.size)

        vm.onDelete()
        advanceUntilIdle()

        assertEquals(listOf("2", "3"), repository.league!!.fixtures.map { it.number })
        assertNull(vm.data.editing)
        assertEquals(getString(Res.string.fixture_deleted), vm.toasts.first())
    }
}
