package it.manu.fuoriorario.feature.shots

import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.save_failed
import fuoriorario.composeapp.generated.resources.session_saved
import fuoriorario.composeapp.generated.resources.shots_deleted
import it.manu.fuoriorario.core.error.AppErrorManager
import it.manu.fuoriorario.core.error.FallbackHandler
import it.manu.fuoriorario.core.error.NetworkErrorHandler
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.DEFAULT_ZONE_REFS
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.SessionError
import it.manu.fuoriorario.domain.ShotSession
import it.manu.fuoriorario.domain.Shots
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.testing.TestDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalCoroutinesApi::class)
class ShotLogViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val luca = Member("Luca B.", Role.PLAYER, id = "luca")
    private val older =
        ShotSession(LocalDate(2026, 10, 1), mapOf(Zone.PIT to Shots(3, 5)), id = "old", memberId = "luca")
    private val shots = FakeShotRepository(older)

    private fun vm() = ShotLogViewModel(
        luca,
        shots,
        TestDispatcherProvider(dispatcher),
        AppErrorManager(listOf(NetworkErrorHandler(), FallbackHandler())) {}
    )

    private val ShotLogViewModel.data get() = uiState.value.data!!

    @Test
    fun load_showsSessionsAndRefs() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        assertIs<UseCaseMutableState.ShowData<*>>(vm.uiState.value.state)
        assertEquals(listOf(older), vm.data.sessions)
        assertEquals(DEFAULT_ZONE_REFS, vm.data.refs)
    }

    @Test
    fun load_offline_showsErrorThenRetryLoads() = runTest(dispatcher) {
        shots.failLoad = true
        val vm = vm()
        advanceUntilIdle()

        val handler = assertIs<UseCaseMutableState.Error>(vm.uiState.value.state).handler
        assertIs<NetworkErrorHandler>(handler)

        handler.retryFunction!!()
        advanceUntilIdle()

        assertEquals(listOf(older), vm.data.sessions)
    }

    @Test
    fun save_invalid_showsErrorInTheSheet() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onLog()
        vm.onZoneChanged(Zone.PIT, Shots(3, 2))
        vm.onSave()
        advanceUntilIdle()

        assertEquals(SessionError.MadeOverAttempted(Zone.PIT), vm.data.draft?.error)
        assertEquals(1, shots.sessions.size)
    }

    @Test
    fun save_addsItFirstAndClosesTheSheet() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onLog()
        vm.onZoneChanged(Zone.TL, Shots(7, 10))
        vm.onNoteChanged("Gambe stanche")
        vm.onSave()
        advanceUntilIdle()

        val saved = shots.sessions.last()
        assertEquals("luca", saved.memberId)
        assertEquals("Gambe stanche", saved.note)
        assertEquals(listOf(saved, older), vm.data.sessions)
        assertNull(vm.data.draft)
        assertFalse(vm.data.busy)
        assertEquals(getString(Res.string.session_saved), vm.toasts.first())
    }

    @Test
    fun save_failure_keepsTheSheetAsTyped() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onLog()
        vm.onZoneChanged(Zone.TL, Shots(7, 10))
        shots.failNext = true
        vm.onSave()
        advanceUntilIdle()

        assertEquals(Shots(7, 10), vm.data.draft?.zones?.get(Zone.TL))
        assertFalse(vm.data.busy)
        assertEquals(listOf(older), vm.data.sessions)
        assertEquals(getString(Res.string.save_failed), vm.toasts.first())
    }

    @Test
    fun delete_asksForASecondTap() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onDelete(older)
        advanceUntilIdle()

        assertNotNull(vm.data.confirmingDelete)
        assertEquals(1, shots.sessions.size)

        vm.onDelete(older)
        advanceUntilIdle()

        assertEquals(emptyList(), vm.data.sessions)
        assertEquals(emptyList(), shots.sessions)
        assertEquals(getString(Res.string.shots_deleted), vm.toasts.first())
    }
}
