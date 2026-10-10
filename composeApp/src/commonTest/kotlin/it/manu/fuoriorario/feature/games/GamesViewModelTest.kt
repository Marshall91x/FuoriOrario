package it.manu.fuoriorario.feature.games

import com.russhwolf.settings.MapSettings
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.game_deleted
import fuoriorario.composeapp.generated.resources.game_saved
import fuoriorario.composeapp.generated.resources.save_failed
import it.manu.fuoriorario.core.error.AppErrorManager
import it.manu.fuoriorario.core.error.FallbackHandler
import it.manu.fuoriorario.core.error.NetworkErrorHandler
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.CallUp
import it.manu.fuoriorario.domain.Game
import it.manu.fuoriorario.domain.GameDraft
import it.manu.fuoriorario.domain.GameError
import it.manu.fuoriorario.domain.GameEvent
import it.manu.fuoriorario.domain.GameEventType
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Quarter
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.Shots
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.feature.games.data.GameDraftStore
import it.manu.fuoriorario.feature.roster.FakeRosterRepository
import it.manu.fuoriorario.feature.shots.FakeShotRepository
import it.manu.fuoriorario.testing.TestDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalCoroutinesApi::class)
class GamesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val coach = Member("Coach", Role.STAFF, id = "coach")
    private val luca = Member("Luca B.", Role.PLAYER, id = "luca", jerseyNumber = "7")
    private val anna = Member("Anna", Role.PLAYER, id = "anna", jerseyNumber = "4")
    private val past = Game(LocalDate(2026, 10, 3), "Virtus", home = true, ourScore = 60, theirScore = 52, id = "g0")
    private val games = FakeGameRepository(past)
    private val roster = FakeRosterRepository(coach, luca, anna)
    private val shots = FakeShotRepository().apply { refs = refs + (Zone.CEN to 30) }
    private val settings = MapSettings()
    private val drafts = GameDraftStore(settings)

    private fun vm(staff: Boolean = true) = GamesViewModel(
        staff,
        games,
        roster,
        shots,
        drafts,
        TestDispatcherProvider(dispatcher),
        AppErrorManager(listOf(NetworkErrorHandler(), FallbackHandler())) {}
    )

    private val GamesViewModel.data get() = uiState.value.data!!

    /** Staff start a game against Fortitudo with both players, in Live. */
    private suspend fun kotlinx.coroutines.test.TestScope.live(): GamesViewModel {
        val vm = vm()
        advanceUntilIdle()
        vm.onNew()
        vm.onOpponentChanged("Fortitudo")
        vm.onCallUpToggled("luca", true)
        vm.onCallUpToggled("anna", true)
        vm.onStart()
        advanceUntilIdle()
        return vm
    }

    @Test
    fun load_staffGetTheGamesAndThePlayersForTheCallUps() = runTest(dispatcher) {
        val vm = vm()
        assertIs<UseCaseMutableState.Loading>(vm.uiState.value.state)
        advanceUntilIdle()

        assertEquals(listOf(past), vm.data.games)
        assertEquals(listOf(anna, luca), vm.data.players)
    }

    @Test
    fun load_playersDoNotReadTheRoster() = runTest(dispatcher) {
        roster.failLoad = true
        val vm = vm(staff = false)
        advanceUntilIdle()

        assertEquals(listOf(past), vm.data.games)
        assertEquals(emptyList(), vm.data.players)
    }

    @Test
    fun start_withoutOpponent_saysSoAndStaysInTheForm() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onNew()
        vm.onOpponentChanged("  ")
        vm.onStart()
        advanceUntilIdle()

        assertEquals(GameError.NO_OPPONENT, vm.data.form?.error)
        assertNull(vm.data.draft)
        assertNull(drafts.draft.value)
    }

    @Test
    fun start_opensLiveWithTheCallUpsInRosterOrderAndKeepsTheDraft() = runTest(dispatcher) {
        val vm = live()

        assertNull(vm.data.form)
        assertTrue(vm.data.live)
        val draft = assertNotNull(vm.data.draft)
        assertEquals("Fortitudo", draft.opponent)
        assertEquals(listOf(CallUp("anna", "Anna", "4"), CallUp("luca", "Luca B.", "7")), draft.callUps)
        assertEquals(draft, drafts.draft.value)
    }

    @Test
    fun shot_goesToTheSelectedPlayerInTheQuarterAndSurvivesARestart() = runTest(dispatcher) {
        val vm = live()
        vm.onShot(Zone.CEN, made = true)
        vm.onSelect("luca")
        vm.onQuarter(Quarter.Q2)
        vm.onShot(Zone.CEN, made = true)
        vm.onShot(Zone.PIT, made = false)
        vm.onFreeThrow(made = true)
        vm.onOpponentScored(2)
        advanceUntilIdle()

        val events = listOf(
            GameEvent(GameEventType.SHOT, Quarter.Q2, "luca", Zone.CEN, true),
            GameEvent(GameEventType.SHOT, Quarter.Q2, "luca", Zone.PIT, false),
            GameEvent(GameEventType.FREE_THROW, Quarter.Q2, "luca", made = true),
            GameEvent(GameEventType.OPPONENT, Quarter.Q2, value = 2)
        )
        assertEquals(events, vm.data.draft?.events)

        val restarted = GamesViewModel(
            true,
            games,
            roster,
            shots,
            GameDraftStore(settings),
            TestDispatcherProvider(dispatcher),
            AppErrorManager(emptyList()) {}
        )
        advanceUntilIdle()
        assertEquals(events, restarted.data.draft?.events)
        assertEquals("luca", restarted.data.draft?.selected)
    }

    @Test
    fun undoAndRemove_takeEventsBack() = runTest(dispatcher) {
        val vm = live()
        vm.onOpponentScored(1)
        vm.onOpponentScored(2)
        vm.onOpponentScored(3)
        vm.onRemoveEvent(0)
        vm.onUndo()
        advanceUntilIdle()

        assertEquals(listOf(GameEvent(GameEventType.OPPONENT, Quarter.Q1, value = 2)), vm.data.draft?.events)
        assertEquals(vm.data.draft, drafts.draft.value)
    }

    @Test
    fun finish_savesTheWholeGameAndForgetsTheDraft() = runTest(dispatcher) {
        val vm = live()
        vm.onSelect("anna")
        vm.onShot(Zone.ACS, made = true)
        val draft = vm.data.draft!!
        vm.onFinish()
        advanceUntilIdle()

        assertEquals(listOf(draft), games.saved)
        assertEquals(draft.toGame(), vm.data.games?.first())
        assertNull(vm.data.draft)
        assertFalse(vm.data.live)
        assertNull(drafts.draft.value)
        assertEquals(getString(Res.string.game_saved), vm.toasts.first())
    }

    @Test
    fun finish_offline_toastsAndKeepsTheDraft() = runTest(dispatcher) {
        val vm = live()
        vm.onOpponentScored(3)
        val draft = vm.data.draft
        games.failNext = true
        vm.onFinish()
        advanceUntilIdle()

        assertEquals(getString(Res.string.save_failed), vm.toasts.first())
        assertEquals(draft, vm.data.draft)
        assertEquals(draft, drafts.draft.value)
        assertTrue(vm.data.live)
        assertFalse(vm.data.busy)
        assertEquals(listOf(past), vm.data.games)
    }

    @Test
    fun abandon_secondTapForgetsTheDraft() = runTest(dispatcher) {
        val vm = live()
        vm.onLeaveLive()
        vm.onAbandon()
        advanceUntilIdle()
        assertTrue(vm.data.confirmingAbandon)
        assertNotNull(drafts.draft.value)

        vm.onAbandon()
        advanceUntilIdle()
        assertNull(vm.data.draft)
        assertNull(drafts.draft.value)
    }

    @Test
    fun new_withADraft_doesNothing() = runTest(dispatcher) {
        drafts.save(GameDraft("g1", LocalDate(2026, 10, 10), "Virtus", home = true))
        val vm = vm()
        advanceUntilIdle()
        vm.onNew()
        advanceUntilIdle()

        assertNull(vm.data.form)
        assertFalse(vm.data.live)
        vm.onResume()
        advanceUntilIdle()
        assertTrue(vm.data.live)
    }

    @Test
    fun load_offlineWithADraft_liveStillWorksAndRetryLoadsTheList() = runTest(dispatcher) {
        drafts.save(GameDraft("g1", LocalDate(2026, 10, 10), "Virtus", home = true))
        games.failLoad = true
        val vm = vm()
        advanceUntilIdle()

        assertIs<UseCaseMutableState.ShowData<*>>(vm.uiState.value.state)
        assertNull(vm.data.games)
        vm.onResume()
        vm.onOpponentScored(2)
        advanceUntilIdle()
        assertEquals(1, drafts.draft.value?.events?.size)

        vm.onRetry()
        advanceUntilIdle()
        assertEquals(listOf(past), vm.data.games)
    }

    @Test
    fun abandon_aFirstTapForgottenByResuming_asksAgain() = runTest(dispatcher) {
        val vm = live()
        vm.onLeaveLive()
        vm.onAbandon()
        vm.onResume()
        vm.onLeaveLive()
        vm.onAbandon()
        advanceUntilIdle()

        assertNotNull(drafts.draft.value)
    }

    private val lucaUp = CallUp("luca", "Luca B.", "7")
    private val annaUp = CallUp("anna", "Anna", "4")
    private val pastEvents = listOf(
        GameEvent(GameEventType.SHOT, Quarter.Q1, "luca", Zone.CEN, true),
        GameEvent(GameEventType.OPPONENT, Quarter.Q1, value = 2),
        GameEvent(GameEventType.SHOT, Quarter.Q2, "anna", Zone.PIT, false),
        GameEvent(GameEventType.FREE_THROW, Quarter.Q2, "luca", made = true)
    )

    /** Staff open the past game, with both players' events. */
    private suspend fun kotlinx.coroutines.test.TestScope.summary(): GamesViewModel {
        games.callUps["g0"] = listOf(annaUp, lucaUp)
        games.events["g0"] = pastEvents
        val vm = vm()
        advanceUntilIdle()
        vm.onOpen(past)
        advanceUntilIdle()
        return vm
    }

    @Test
    fun open_loadsTheCallUpsTheEventsAndTheTeamRefsOnASecondLevelScreen() = runTest(dispatcher) {
        val vm = summary()

        val summary = assertNotNull(vm.data.summary)
        assertTrue(summary.loaded)
        assertEquals(listOf(annaUp, lucaUp), summary.callUps)
        assertEquals(pastEvents, summary.events)
        assertEquals(30, summary.refs[Zone.CEN])
        assertTrue(vm.data.secondLevel)

        vm.onCloseSummary()
        advanceUntilIdle()
        assertNull(vm.data.summary)
        assertFalse(vm.data.secondLevel)
    }

    @Test
    fun quarter_filtersTheTabellinoAndTheMap() = runTest(dispatcher) {
        val vm = summary()
        vm.onSummaryQuarter(Quarter.Q2)
        advanceUntilIdle()

        val summary = vm.data.summary!!
        assertEquals(listOf(0, 1), summary.box.map { it.points })
        assertEquals(Shots(0, 1), summary.zones.getValue(Zone.PIT))
        assertEquals(Shots(), summary.zones.getValue(Zone.CEN))
    }

    @Test
    fun boxRow_showsThatPlayersMapUntilTappedAgain() = runTest(dispatcher) {
        val vm = summary()
        vm.onBoxRow("anna")
        advanceUntilIdle()
        assertEquals(Shots(0, 1), vm.data.summary!!.zones.getValue(Zone.PIT))
        assertEquals(Shots(), vm.data.summary!!.zones.getValue(Zone.CEN))

        vm.onBoxRow("anna")
        advanceUntilIdle()
        assertNull(vm.data.summary!!.member)
        assertEquals(Shots(1, 1), vm.data.summary!!.zones.getValue(Zone.CEN))
    }

    @Test
    fun delete_secondTapRemovesTheGame() = runTest(dispatcher) {
        val vm = summary()
        vm.onDelete()
        advanceUntilIdle()
        assertTrue(vm.data.summary!!.confirmingRemoval)
        assertEquals(listOf(past), games.games)

        vm.onDelete()
        advanceUntilIdle()
        assertEquals(getString(Res.string.game_deleted), vm.toasts.first())
        assertNull(vm.data.summary)
        assertEquals(emptyList(), vm.data.games)
        assertEquals(emptyList(), games.games)
    }

    @Test
    fun delete_offline_toastsAndKeepsTheSummary() = runTest(dispatcher) {
        val vm = summary()
        vm.onDelete()
        games.failNext = true
        vm.onDelete()
        advanceUntilIdle()

        assertEquals(getString(Res.string.save_failed), vm.toasts.first())
        assertNotNull(vm.data.summary)
        assertEquals(listOf(past), vm.data.games)
    }

    @Test
    fun load_playersGetTheirPointsInEachGameCalledUpTo() = runTest(dispatcher) {
        games.callUps["g0"] = listOf(lucaUp)
        games.callUps["g1"] = listOf(lucaUp)
        games.events["g0"] = pastEvents.filter { it.memberId == "luca" }
        val vm = vm(staff = false)
        advanceUntilIdle()

        assertEquals(mapOf("g0" to 4, "g1" to 0), vm.data.points)
    }

    @Test
    fun open_offline_errorScreenThenRetryLoadsTheSummary() = runTest(dispatcher) {
        games.callUps["g0"] = listOf(annaUp, lucaUp)
        games.events["g0"] = pastEvents
        val vm = vm()
        advanceUntilIdle()
        games.failLoad = true
        vm.onOpen(past)
        advanceUntilIdle()

        val handler = assertIs<UseCaseMutableState.Error>(vm.uiState.value.state).handler
        assertIs<NetworkErrorHandler>(handler).retryFunction!!()
        advanceUntilIdle()
        assertTrue(vm.data.summary!!.loaded)
        assertEquals(pastEvents, vm.data.summary!!.events)
    }
}
