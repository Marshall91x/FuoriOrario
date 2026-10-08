package it.manu.fuoriorario.feature.settings

import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.refs_saved
import fuoriorario.composeapp.generated.resources.save_failed
import it.manu.fuoriorario.core.error.AppErrorManager
import it.manu.fuoriorario.core.error.FallbackHandler
import it.manu.fuoriorario.core.error.NetworkErrorHandler
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.DEFAULT_ZONE_REFS
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.feature.shots.FakeShotRepository
import it.manu.fuoriorario.testing.TestDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalCoroutinesApi::class)
class ZoneRefsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val shots = FakeShotRepository().apply { refs = DEFAULT_ZONE_REFS + (Zone.PIT to 30) }

    private fun vm() = ZoneRefsViewModel(
        shots,
        TestDispatcherProvider(dispatcher),
        AppErrorManager(listOf(NetworkErrorHandler(), FallbackHandler())) {}
    )

    private val ZoneRefsViewModel.data get() = uiState.value.data!!

    @Test
    fun load_showsTheRefsAsText() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        assertEquals("30", vm.data.draft!!.getValue(Zone.PIT))
    }

    @Test
    fun load_offline_showsErrorThenRetryLoads() = runTest(dispatcher) {
        shots.failRefs = true
        val vm = vm()
        advanceUntilIdle()

        val handler = assertIs<UseCaseMutableState.Error>(vm.uiState.value.state).handler
        assertIs<NetworkErrorHandler>(handler)

        handler.retryFunction!!()
        advanceUntilIdle()

        assertEquals("30", vm.data.draft!!.getValue(Zone.PIT))
    }

    @Test
    fun save_invalid_showsTheErrorAndSavesNothing() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onRefChanged(Zone.PIT, "120")
        vm.onSave()
        advanceUntilIdle()

        assertTrue(vm.data.invalid)
        assertEquals(30, shots.refs.getValue(Zone.PIT))
    }

    @Test
    fun save_storesAndToasts() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onRefChanged(Zone.PIT, "45")
        vm.onSave()
        advanceUntilIdle()

        assertEquals(DEFAULT_ZONE_REFS + (Zone.PIT to 45), shots.refs)
        assertEquals(getString(Res.string.refs_saved), vm.toasts.first())
    }

    @Test
    fun save_offline_toastsAndKeepsWhatWasTyped() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        shots.failNext = true
        vm.onRefChanged(Zone.PIT, "45")
        vm.onSave()
        advanceUntilIdle()

        assertEquals(getString(Res.string.save_failed), vm.toasts.first())
        assertEquals("45", vm.data.draft!!.getValue(Zone.PIT))
        assertEquals(30, shots.refs.getValue(Zone.PIT))
    }

    @Test
    fun restore_fillsInTheDefaultsWithoutSaving() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onRefChanged(Zone.PIT, "120")
        vm.onSave()
        vm.onRestore()
        advanceUntilIdle()

        assertEquals("55", vm.data.draft!!.getValue(Zone.PIT))
        assertFalse(vm.data.invalid)
        assertEquals(30, shots.refs.getValue(Zone.PIT))
    }
}
