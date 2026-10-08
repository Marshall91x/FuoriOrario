package it.manu.fuoriorario.feature.settings

import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.exercise_updated
import fuoriorario.composeapp.generated.resources.library_added
import fuoriorario.composeapp.generated.resources.library_removed
import fuoriorario.composeapp.generated.resources.save_failed
import it.manu.fuoriorario.core.error.AppErrorManager
import it.manu.fuoriorario.core.error.FallbackHandler
import it.manu.fuoriorario.core.error.NetworkErrorHandler
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.Category
import it.manu.fuoriorario.domain.LibraryExercise
import it.manu.fuoriorario.domain.PlanItemError
import it.manu.fuoriorario.feature.plan.FakePlanRepository
import it.manu.fuoriorario.testing.TestDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val mikan = LibraryExercise("Mikan drill", Category.FOOTWORK, "3 × 20 canestri", sort = 0, id = "l1")
    private val liberi = LibraryExercise("Tiri liberi sotto fatica", Category.SHOOTING, sort = 1, id = "l2")
    private val plans = FakePlanRepository(library = listOf(mikan, liberi))

    private fun vm() = LibraryViewModel(
        plans,
        TestDispatcherProvider(dispatcher),
        AppErrorManager(listOf(NetworkErrorHandler(), FallbackHandler())) {}
    )

    private val LibraryViewModel.data get() = uiState.value.data!!

    @Test
    fun load_showsTheLibrary() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        assertEquals(listOf(mikan, liberi), vm.data.library)
    }

    @Test
    fun load_failed_showsTheErrorScreen() = runTest(dispatcher) {
        plans.failLibrary = true
        val vm = vm()
        advanceUntilIdle()

        assertIs<UseCaseMutableState.Error>(vm.uiState.value.state)
    }

    @Test
    fun add_invalid_showsTheErrorAndSavesNothing() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onAdd()
        vm.onSave()
        advanceUntilIdle()

        assertEquals(PlanItemError.TITLE, vm.data.editError)
        assertEquals(2, plans.library.size)
    }

    @Test
    fun add_savesLastClosesTheSheetAndToasts() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onAdd()
        vm.onEditChanged(vm.data.editing!!.copy(title = " Arresto e tiro "))
        vm.onSave()
        advanceUntilIdle()

        val added = LibraryExercise("Arresto e tiro", Category.BALL_HANDLING, sort = 2, id = "l3")
        assertEquals(listOf(mikan, liberi, added), vm.data.library)
        assertNull(vm.data.editing)
        assertEquals(getString(Res.string.library_added), vm.toasts.first())
    }

    @Test
    fun edit_offline_toastsAndKeepsTheSheet() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        plans.failNext = true
        vm.onEdit(mikan)
        vm.onEditChanged(mikan.copy(title = "Mikan"))
        vm.onSave()
        advanceUntilIdle()

        assertEquals(getString(Res.string.save_failed), vm.toasts.first())
        assertEquals("Mikan", vm.data.editing?.title)
        assertEquals(mikan, plans.library.first())
    }

    @Test
    fun edit_saves() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEdit(mikan)
        vm.onEditChanged(mikan.copy(volume = ""))
        vm.onSave()
        advanceUntilIdle()

        assertEquals(mikan.copy(volume = null), plans.library.first())
        assertEquals(mikan.copy(volume = null), vm.data.library!!.first())
        assertEquals(getString(Res.string.exercise_updated), vm.toasts.first())
    }

    @Test
    fun move_reorders() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onMove(mikan, 1)
        advanceUntilIdle()

        assertEquals(listOf("l2", "l1"), plans.library.map { it.id })
        assertEquals(listOf("l2", "l1"), vm.data.library!!.map { it.id })
    }

    @Test
    fun remove_asksToConfirmThenRemoves() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEdit(liberi)
        vm.onRemove()
        advanceUntilIdle()

        assertTrue(vm.data.confirmingRemoval)
        assertEquals(2, plans.library.size)

        vm.onRemove()
        advanceUntilIdle()

        assertEquals(listOf(mikan), vm.data.library)
        assertNull(vm.data.editing)
        assertEquals(getString(Res.string.library_removed), vm.toasts.first())
    }
}
