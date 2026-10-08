package it.manu.fuoriorario.feature.roster

import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.last_staff
import fuoriorario.composeapp.generated.resources.member_added
import fuoriorario.composeapp.generated.resources.member_removed
import fuoriorario.composeapp.generated.resources.member_saved
import fuoriorario.composeapp.generated.resources.save_failed
import it.manu.fuoriorario.core.error.AppErrorManager
import it.manu.fuoriorario.core.error.FallbackHandler
import it.manu.fuoriorario.core.error.NetworkErrorHandler
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.MemberError
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.testing.TestDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalCoroutinesApi::class)
class RosterViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val staff = Member("Coach", Role.STAFF, email = "staff@example.com", id = "s")
    private val luca = Member("Luca B.", Role.PLAYER, email = "luca@example.com", jerseyNumber = "7", id = "l")
    private val roster = FakeRosterRepository(staff, luca)

    private fun vm() = RosterViewModel(
        roster,
        TestDispatcherProvider(dispatcher),
        AppErrorManager(listOf(NetworkErrorHandler(), FallbackHandler())) {}
    )

    private val RosterViewModel.data get() = uiState.value.data!!

    @Test
    fun load_showsTheTeam() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        assertEquals(listOf(staff, luca), vm.data.members)
    }

    @Test
    fun load_offline_showsErrorThenRetryLoads() = runTest(dispatcher) {
        roster.failLoad = true
        val vm = vm()
        advanceUntilIdle()

        val handler = assertIs<UseCaseMutableState.Error>(vm.uiState.value.state).handler
        assertIs<NetworkErrorHandler>(handler)

        handler.retryFunction!!()
        advanceUntilIdle()

        assertEquals(listOf(staff, luca), vm.data.members)
    }

    @Test
    fun add_invalid_showsTheErrorAndSavesNothing() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onDraftChanged(Member("Marco R.", Role.PLAYER, email = "marco"))
        vm.onAdd()
        advanceUntilIdle()

        assertEquals(MemberError.EMAIL_INVALID, vm.data.addError)
        assertEquals(2, roster.members.size)
    }

    @Test
    fun add_emailTaken_showsTheErrorUnderTheForm() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        // Not in the loaded team: only the server knows.
        roster.members += Member("Other", Role.PLAYER, email = "marco@example.com", id = "o")
        vm.onDraftChanged(Member("Marco R.", Role.PLAYER, email = "marco@example.com"))
        vm.onAdd()
        advanceUntilIdle()

        assertEquals(MemberError.EMAIL_TAKEN, vm.data.addError)
        assertEquals("Marco R.", vm.data.draft.displayName)
    }

    @Test
    fun add_savesClearsTheFormAndToasts() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onDraftChanged(Member(" Marco R. ", Role.PLAYER, email = " Marco@Example.com ", jerseyNumber = "12"))
        vm.onAdd()
        advanceUntilIdle()

        val marco =
            Member("Marco R.", Role.PLAYER, email = "marco@example.com", jerseyNumber = "12", id = "marco@example.com")
        assertEquals(listOf(staff, luca, marco), vm.data.members)
        assertEquals("", vm.data.draft.displayName)
        assertEquals(getString(Res.string.member_added, "Marco R."), vm.toasts.first())
        assertEquals(marco, vm.changes.first())
    }

    @Test
    fun add_offline_toastsAndKeepsTheForm() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        roster.failNext = true
        vm.onDraftChanged(Member("Marco R.", Role.PLAYER, email = "marco@example.com"))
        vm.onAdd()
        advanceUntilIdle()

        assertEquals(getString(Res.string.save_failed), vm.toasts.first())
        assertEquals("Marco R.", vm.data.draft.displayName)
        assertEquals(listOf(staff, luca), vm.data.members)
    }

    @Test
    fun edit_savesAndClosesTheSheet() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEdit(luca)
        vm.onEditChanged(luca.copy(displayName = "Luca Bianchi"))
        vm.onSaveEdit()
        advanceUntilIdle()

        assertNull(vm.data.editing)
        assertEquals(luca.copy(displayName = "Luca Bianchi"), vm.data.members!!.last())
        assertEquals(getString(Res.string.member_saved), vm.toasts.first())
    }

    @Test
    fun remove_asksToConfirmThenRemoves() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onRemove(luca)
        advanceUntilIdle()

        assertEquals(luca, vm.data.confirmingRemoval)
        assertEquals(2, roster.members.size)

        vm.onRemove(luca)
        advanceUntilIdle()

        assertEquals(listOf(staff), vm.data.members)
        assertEquals(getString(Res.string.member_removed, "Luca B."), vm.toasts.first())
        assertEquals(luca, vm.changes.first())
    }

    @Test
    fun remove_lastStaff_toastsWhyAndKeepsThem() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onRemove(staff)
        vm.onRemove(staff)
        advanceUntilIdle()

        assertEquals(getString(Res.string.last_staff), vm.toasts.first())
        assertEquals(listOf(staff, luca), vm.data.members)
    }
}
