package it.manu.fuoriorario.feature.plan

import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.exercise_added
import fuoriorario.composeapp.generated.resources.exercise_library_failed
import fuoriorario.composeapp.generated.resources.plan_changed
import fuoriorario.composeapp.generated.resources.save_failed
import it.manu.fuoriorario.core.error.AppErrorManager
import it.manu.fuoriorario.core.error.FallbackHandler
import it.manu.fuoriorario.core.error.NetworkErrorHandler
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.Category
import it.manu.fuoriorario.domain.LibraryExercise
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanCheck
import it.manu.fuoriorario.domain.PlanItem
import it.manu.fuoriorario.domain.PlanItemError
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.testing.TestDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
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
class PlanViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val luca = Member("Luca B.", Role.PLAYER, id = "luca")
    private val week = LocalDate(2026, 10, 5)
    private val nextWeek = LocalDate(2026, 10, 12)
    private val mikan =
        PlanItem(week, "Mikan drill", Category.FOOTWORK, days = listOf(0, 2), id = "p1", memberId = "luca")
    private val liberi = PlanItem(nextWeek, "Liberi", Category.SHOOTING, days = listOf(4), id = "p2", memberId = "luca")
    private val plans = FakePlanRepository(mikan, liberi, checks = setOf(PlanCheck("p1", 0)))
        .apply { notes["luca" to week] = "Spingi" }

    private fun vm() = PlanViewModel(
        luca,
        week,
        plans,
        TestDispatcherProvider(dispatcher),
        AppErrorManager(listOf(NetworkErrorHandler(), FallbackHandler())) {}
    )

    private val PlanViewModel.data get() = uiState.value.data!!

    @Test
    fun load_showsTheWeek() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        assertIs<UseCaseMutableState.ShowData<*>>(vm.uiState.value.state)
        assertEquals(listOf(mikan), vm.data.items)
        assertEquals(setOf(PlanCheck("p1", 0)), vm.data.checks)
        assertEquals("Spingi", vm.data.note)
    }

    @Test
    fun load_offline_showsErrorThenRetryLoads() = runTest(dispatcher) {
        plans.failLoad = true
        val vm = vm()
        advanceUntilIdle()

        val handler = assertIs<UseCaseMutableState.Error>(vm.uiState.value.state).handler
        assertIs<NetworkErrorHandler>(handler)

        handler.retryFunction!!()
        advanceUntilIdle()

        assertEquals(listOf(mikan), vm.data.items)
    }

    @Test
    fun week_loadsThatWeekOnly() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onWeek(nextWeek)

        // The old week stays on screen, marked, until the new one arrives.
        assertEquals(listOf(mikan), vm.data.items)
        assertEquals("Spingi", vm.data.note)
        assertTrue(vm.data.reloading)
        advanceUntilIdle()

        assertEquals(nextWeek, vm.data.week)
        assertEquals(listOf(liberi), vm.data.items)
        assertEquals(emptySet(), vm.data.checks)
        assertNull(vm.data.note)
        assertFalse(vm.data.reloading)
    }

    @Test
    fun week_loading_copyAndNoteWaitForIt() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onWeek(nextWeek)

        // The exercises and note on screen are the previous week's.
        vm.onCopy()
        vm.onWriteNote()
        advanceUntilIdle()

        assertFalse(vm.data.confirmingCopy)
        assertNull(vm.data.noteDraft)
    }

    @Test
    fun toggle_checksAndUnchecks() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onToggle(PlanCheck("p1", 2))
        advanceUntilIdle()

        assertEquals(setOf(PlanCheck("p1", 0), PlanCheck("p1", 2)), vm.data.checks)

        vm.onToggle(PlanCheck("p1", 0))
        advanceUntilIdle()

        assertEquals(setOf(PlanCheck("p1", 2)), vm.data.checks)
        assertEquals(setOf(PlanCheck("p1", 2)), plans.checks)
    }

    @Test
    fun toggle_failure_leavesTheDotAsItWas() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        plans.failNext = true
        vm.onToggle(PlanCheck("p1", 2))
        advanceUntilIdle()

        assertEquals(setOf(PlanCheck("p1", 0)), vm.data.checks)
        assertFalse(vm.data.busy)
        assertEquals(getString(Res.string.save_failed), vm.toasts.first())
    }

    @Test
    fun add_fromTheLibrary_savesForThePlayer() = runTest(dispatcher) {
        plans.library += LibraryExercise("Liberi", Category.SHOOTING, "10 × 10", id = "l1")
        val vm = vm()
        advanceUntilIdle()
        vm.onAdd()
        advanceUntilIdle()

        vm.onExerciseChanged(vm.data.library.single())
        vm.onDaysChanged(setOf(1))
        vm.onSaveExercise()
        advanceUntilIdle()

        val added = plans.items.last()
        assertEquals(
            PlanItem(week, "Liberi", Category.SHOOTING, "10 × 10", days = listOf(1), id = "p3", memberId = "luca"),
            added
        )
        assertEquals(listOf(mikan, added), vm.data.items)
        assertNull(vm.data.exercise)
        assertEquals(getString(Res.string.exercise_added), vm.toasts.first())
    }

    @Test
    fun add_withoutTitle_showsErrorInTheSheet() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onAdd()
        vm.onSaveExercise()
        advanceUntilIdle()

        assertEquals(PlanItemError.TITLE, vm.data.exercise?.error)
        assertEquals(2, plans.items.size)
    }

    @Test
    fun add_libraryOffline_toastsAndTheNextSheetTriesAgain() = runTest(dispatcher) {
        plans.library += LibraryExercise("Liberi", Category.SHOOTING, id = "l1")
        plans.failLibrary = true
        val vm = vm()
        advanceUntilIdle()
        vm.onAdd()
        advanceUntilIdle()

        assertEquals(getString(Res.string.exercise_library_failed), vm.toasts.first())
        assertEquals(emptyList(), vm.data.library)

        vm.onDismissExercise()
        vm.onAdd()
        advanceUntilIdle()

        assertEquals(1, vm.data.library.size)
    }

    @Test
    fun copy_weekChangedMeanwhile_reloadsAndToasts() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onWeek(nextWeek)
        advanceUntilIdle()
        // Another staff adds one after the load: the copy is refused.
        val other = PlanItem(nextWeek, "Tiro", Category.SHOOTING, days = listOf(1), id = "x1", memberId = "luca")
        plans.items += other
        vm.onCopy() // asks to confirm: the week isn't empty
        advanceUntilIdle()
        vm.onCopy()
        advanceUntilIdle()

        assertEquals(getString(Res.string.plan_changed), vm.toasts.first())
        assertEquals(listOf(liberi, other), vm.data.items)
        assertFalse(vm.data.confirmingCopy)
    }

    @Test
    fun note_emptied_isRemoved() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onWriteNote()
        assertEquals("Spingi", vm.data.noteDraft)
        vm.onNoteChanged("  ")
        vm.onSaveNote()
        advanceUntilIdle()

        assertNull(vm.data.note)
        assertNull(vm.data.noteDraft)
        assertEquals(emptyMap(), plans.notes)
    }
}
