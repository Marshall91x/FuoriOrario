package it.manu.fuoriorario.feature.plays

import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.play_removed
import fuoriorario.composeapp.generated.resources.play_saved
import fuoriorario.composeapp.generated.resources.play_unsaved
import fuoriorario.composeapp.generated.resources.save_failed
import it.manu.fuoriorario.core.error.AppErrorManager
import it.manu.fuoriorario.core.error.FallbackHandler
import it.manu.fuoriorario.core.error.NetworkErrorHandler
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.CourtSize
import it.manu.fuoriorario.domain.Play
import it.manu.fuoriorario.domain.PlayCategory
import it.manu.fuoriorario.domain.PlayError
import it.manu.fuoriorario.domain.Point
import it.manu.fuoriorario.domain.startingStep
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
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalCoroutinesApi::class)
class PlaysViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val box = Play(
        "Box",
        PlayCategory.BASELINE_INBOUND,
        court = CourtSize.HALF,
        defense = false,
        steps = listOf(startingStep(defense = false)),
        id = "box"
    )
    private val plays = FakePlayRepository(box)

    private fun vm() = PlaysViewModel(
        plays,
        TestDispatcherProvider(dispatcher),
        AppErrorManager(listOf(NetworkErrorHandler(), FallbackHandler())) {}
    )

    private val PlaysViewModel.data get() = uiState.value.data!!

    @Test
    fun load_isLoadingUntilThePlaysArrive() = runTest(dispatcher) {
        val vm = vm()

        assertIs<UseCaseMutableState.Loading>(vm.uiState.value.state)
    }

    @Test
    fun load_showsThePlays() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        assertIs<UseCaseMutableState.ShowData<*>>(vm.uiState.value.state)
        assertEquals(listOf(box), vm.data.plays)
    }

    @Test
    fun load_offline_showsErrorThenRetryLoads() = runTest(dispatcher) {
        plays.failLoad = true
        val vm = vm()
        advanceUntilIdle()

        val handler = assertIs<UseCaseMutableState.Error>(vm.uiState.value.state).handler
        assertIs<NetworkErrorHandler>(handler)

        handler.retryFunction!!()
        advanceUntilIdle()

        assertEquals(listOf(box), vm.data.plays)
    }

    @Test
    fun save_withoutTitle_staysInTheEditor() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onNew()
        vm.onSave()
        advanceUntilIdle()

        assertEquals(PlayError.TITLE, vm.data.draft?.error)
        assertEquals(listOf(box), plays.plays)
    }

    @Test
    fun save_new_addsItAndOpensIt() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onNew()
        vm.onTitleChanged(" Spain ")
        vm.onAddStep()
        vm.onPieceMoved("1", Point(250f, 200f))
        vm.onHold(Point(60f, 230f))
        vm.onScreenToggled("5", true)
        vm.onNoteChanged("1 sale, palla a 2.")
        vm.onSave()
        advanceUntilIdle()

        val saved = plays.plays.last()
        assertEquals("Spain", saved.title)
        assertEquals(Point(250f, 200f), saved.steps[1].pos["1"])
        assertEquals("2", saved.steps[1].ball)
        assertEquals(listOf("5"), saved.steps[1].screens)
        assertEquals("1 sale, palla a 2.", saved.steps[1].note)
        assertEquals(startingStep(defense = false), saved.steps[0])
        assertNull(vm.data.draft)
        assertEquals(saved.id, vm.data.openId)
        assertEquals(listOf(box, saved), vm.data.plays)
        assertEquals(getString(Res.string.play_saved), vm.toasts.first())
    }

    @Test
    fun save_failure_keepsTheEditorAsDrawn() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onOpen(box)
        vm.onEdit()
        vm.onTitleChanged("Box 2")
        plays.failNext = true
        vm.onSave()
        advanceUntilIdle()

        assertEquals("Box 2", vm.data.draft?.play?.title)
        assertFalse(vm.data.busy)
        assertEquals(listOf(box), plays.plays)
        assertEquals(getString(Res.string.save_failed), vm.toasts.first())
    }

    @Test
    fun leave_withChanges_asksForASecondTap() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onOpen(box)
        vm.onEdit()
        vm.onLeave()
        assertNull(vm.data.draft)

        vm.onEdit()
        vm.onTitleChanged("Box 2")
        vm.onLeave()
        assertTrue(vm.data.draft!!.confirmingExit)
        assertEquals(getString(Res.string.play_unsaved), vm.toasts.first())

        vm.onLeave()
        assertNull(vm.data.draft)
        assertEquals("box", vm.data.openId)
    }

    @Test
    fun holdAwayFromEveryAttacker_keepsTheConfirmation() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onOpen(box)
        vm.onEdit()
        vm.onTitleChanged("Box 2")
        vm.onLeave()
        vm.onHold(Point(250f, 210f))

        assertTrue(vm.data.draft!!.confirmingExit)
    }

    @Test
    fun remove_asksForASecondTap() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onOpen(box)
        vm.onEdit()
        vm.onRemove()
        advanceUntilIdle()

        assertNotNull(vm.data.draft)
        assertEquals(listOf(box), plays.plays)

        vm.onRemove()
        advanceUntilIdle()

        assertEquals(emptyList(), plays.plays)
        assertEquals(emptyList(), vm.data.plays)
        assertNull(vm.data.draft)
        assertNull(vm.data.openId)
        assertEquals(getString(Res.string.play_removed), vm.toasts.first())
    }

    @Test
    fun steps_addAndRemoveWithinTheLimits() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onNew()
        repeat(25) { vm.onAddStep() }
        assertEquals(20, vm.data.draft!!.play.steps.size)
        assertEquals(19, vm.data.draft!!.index)

        vm.onRemoveStep()
        assertEquals(19, vm.data.draft!!.play.steps.size)
        assertEquals(18, vm.data.draft!!.index)
    }
}
