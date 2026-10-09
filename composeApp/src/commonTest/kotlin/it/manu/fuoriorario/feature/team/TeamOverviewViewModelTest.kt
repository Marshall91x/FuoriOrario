package it.manu.fuoriorario.feature.team

import com.russhwolf.settings.MapSettings
import it.manu.fuoriorario.core.error.AppErrorManager
import it.manu.fuoriorario.core.error.FallbackHandler
import it.manu.fuoriorario.core.error.NetworkErrorHandler
import it.manu.fuoriorario.core.session.SelectedPlayer
import it.manu.fuoriorario.core.today
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.Category
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanCheck
import it.manu.fuoriorario.domain.PlanItem
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.ShotSession
import it.manu.fuoriorario.domain.Shots
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.domain.weekOf
import it.manu.fuoriorario.feature.plan.FakePlanRepository
import it.manu.fuoriorario.feature.shots.FakeShotRepository
import it.manu.fuoriorario.testing.TestDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class TeamOverviewViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val luca = Member("Luca B.", Role.PLAYER, jerseyNumber = "7", id = "luca")
    private val anna = Member("Anna", Role.PLAYER, id = "anna")
    private val shots = FakeShotRepository(
        ShotSession(today(), mapOf(Zone.CEN to Shots(3, 10), Zone.TL to Shots(7, 10)), id = "l1", memberId = "luca")
    )
    private val plans = FakePlanRepository(
        PlanItem(weekOf(today()), "Mikan", Category.FOOTWORK, days = listOf(0, 1), id = "p1", memberId = "luca"),
        checks = setOf(PlanCheck("p1", 0))
    )
    private val selected = SelectedPlayer(MapSettings())

    private fun vm() = TeamOverviewViewModel(
        listOf(luca, anna),
        shots,
        plans,
        selected,
        TestDispatcherProvider(dispatcher),
        AppErrorManager(listOf(NetworkErrorHandler(), FallbackHandler())) {}
    )

    private val TeamOverviewViewModel.rows get() = uiState.value.data!!.rows!!

    @Test
    fun load_showsARowPerPlayerInOrder() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        assertEquals(listOf(luca, anna), vm.rows.map { it.first })
        val row = vm.rows[0].second
        assertEquals(50, row.plan)
        assertEquals(20, row.attempted7)
        assertEquals(70, row.freeThrows30)
        assertEquals(null, vm.rows[1].second.plan)
    }

    @Test
    fun load_offline_showsErrorThenRetryLoads() = runTest(dispatcher) {
        shots.failTeam = true
        val vm = vm()
        advanceUntilIdle()

        val handler = assertIs<UseCaseMutableState.Error>(vm.uiState.value.state).handler
        assertIs<NetworkErrorHandler>(handler)

        handler.retryFunction!!()
        advanceUntilIdle()

        assertEquals(2, vm.rows.size)
    }

    @Test
    fun open_selectsThePlayer() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onOpen(anna)

        assertEquals("anna", selected.id.value)
    }
}
