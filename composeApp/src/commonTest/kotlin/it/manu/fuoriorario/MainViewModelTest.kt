package it.manu.fuoriorario

import com.russhwolf.settings.MapSettings
import it.manu.fuoriorario.core.error.AppErrorManager
import it.manu.fuoriorario.core.navigation.Route
import it.manu.fuoriorario.core.session.SelectedPlayer
import it.manu.fuoriorario.core.session.Session
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.feature.auth.FakeAuthRepository
import it.manu.fuoriorario.feature.roster.FakeRosterRepository
import it.manu.fuoriorario.feature.roster.data.RosterRepository
import it.manu.fuoriorario.testing.TestDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

private const val ACK = "2026-10-01T10:00:00Z"

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val coach = Member("Coach", Role.STAFF, ACK, id = "coach")
    private val luca = Member("Luca B.", Role.PLAYER, ACK, id = "luca", jerseyNumber = "7")
    private val anna = Member("Anna", Role.PLAYER, ACK, id = "anna", jerseyNumber = "4")
    private val settings = MapSettings()

    private fun vm(auth: FakeAuthRepository, roster: RosterRepository = FakeRosterRepository(coach, luca, anna)) =
        MainViewModel(
            auth,
            roster,
            SelectedPlayer(settings),
            TestDispatcherProvider(dispatcher),
            AppErrorManager(emptyList()) {}
        )

    private val MainViewModel.state get() = uiState.value.data!!

    @Test
    fun beforeTheSession_isLoading() {
        assertEquals(Route.LOADING, MainScreenState().route)
    }

    @Test
    fun signedOut_login() = runTest(dispatcher) {
        val vm = vm(FakeAuthRepository())
        advanceUntilIdle()

        assertEquals(Route.LOGIN, vm.state.route)
    }

    @Test
    fun expired_loginWithNotice() = runTest(dispatcher) {
        val auth = FakeAuthRepository(luca)
        val vm = vm(auth)
        advanceUntilIdle()

        auth.session.value = Session.Expired
        advanceUntilIdle()

        assertEquals(Route.LOGIN, vm.state.route)
        assertEquals(Session.Expired, vm.state.session)
    }

    @Test
    fun notAcknowledged_privacyThenHome() = runTest(dispatcher) {
        val auth = FakeAuthRepository(luca.copy(privacyAckAt = null))
        val vm = vm(auth)
        advanceUntilIdle()
        assertEquals(Route.PRIVACY, vm.state.route)

        auth.acknowledgePrivacy()
        advanceUntilIdle()
        assertEquals(Route.HOME, vm.state.route)
    }

    @Test
    fun player_followsThemselves() = runTest(dispatcher) {
        val vm = vm(FakeAuthRepository(luca))
        advanceUntilIdle()

        assertEquals(luca, vm.state.followed)
        assertNull(vm.state.players)
    }

    @Test
    fun staff_followsFirstPlayerThenThePickedOne() = runTest(dispatcher) {
        val vm = vm(FakeAuthRepository(coach))
        advanceUntilIdle()

        assertEquals(listOf(anna, luca), vm.state.players?.getOrNull())
        assertEquals(anna, vm.state.followed)

        vm.onPick(luca)
        advanceUntilIdle()

        assertEquals(luca, vm.state.followed)
        assertEquals("luca", SelectedPlayer(settings).id.value)
    }

    @Test
    fun staffNotYetSaved_loadsPlayersToo() = runTest(dispatcher) {
        val vm = vm(FakeAuthRepository(coach.copy(id = null)))
        advanceUntilIdle()

        assertEquals(listOf(anna, luca), vm.state.players?.getOrNull())
    }

    @Test
    fun staff_pickSurvivesARestart() = runTest(dispatcher) {
        SelectedPlayer(settings).select("luca")

        val vm = vm(FakeAuthRepository(coach))
        advanceUntilIdle()

        assertEquals(luca, vm.state.followed)
    }

    @Test
    fun playersFailure_retryLoadsThem() = runTest(dispatcher) {
        var fail = true
        val roster = object : RosterRepository by FakeRosterRepository(coach, luca) {
            override suspend fun members() = if (fail) error("offline") else listOf(coach, luca)
        }
        val vm = vm(FakeAuthRepository(coach), roster)
        advanceUntilIdle()
        assertTrue(vm.state.players!!.isFailure)
        assertNull(vm.state.followed)

        fail = false
        vm.onRetryPlayers()
        advanceUntilIdle()

        assertEquals(luca, vm.state.followed)
    }

    @Test
    fun ownRowChanged_reloadsTheMember() = runTest(dispatcher) {
        val auth = FakeAuthRepository(coach)
        val vm = vm(auth)
        advanceUntilIdle()

        vm.onRosterChanged(luca)
        assertEquals(0, auth.refreshes)
        vm.onRosterChanged(coach)
        assertEquals(1, auth.refreshes)
    }

    @Test
    fun signOut_login() = runTest(dispatcher) {
        val vm = vm(FakeAuthRepository(luca))
        advanceUntilIdle()

        vm.onSignOut()
        advanceUntilIdle()

        assertEquals(Route.LOGIN, vm.state.route)
    }
}
