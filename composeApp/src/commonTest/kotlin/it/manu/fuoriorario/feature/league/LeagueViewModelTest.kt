package it.manu.fuoriorario.feature.league

import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.league_deleted
import fuoriorario.composeapp.generated.resources.league_imported
import fuoriorario.composeapp.generated.resources.save_failed
import it.manu.fuoriorario.core.error.AppErrorManager
import it.manu.fuoriorario.core.error.FallbackHandler
import it.manu.fuoriorario.core.error.NetworkErrorHandler
import it.manu.fuoriorario.domain.Fixture
import it.manu.fuoriorario.domain.FixtureStatus
import it.manu.fuoriorario.domain.ImportFixture
import it.manu.fuoriorario.domain.ImportPreview
import it.manu.fuoriorario.domain.League
import it.manu.fuoriorario.domain.LeagueTeam
import it.manu.fuoriorario.domain.Leg
import it.manu.fuoriorario.testing.TestDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.io.IOException
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalCoroutinesApi::class)
class LeagueViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val pdf = ImportPreview(
        "U19 - PRIMA FASE",
        listOf("Artemis", "Atessa", "Vasto"),
        listOf(
            ImportFixture("000691", 1, Leg.FIRST, LocalDateTime(2026, 11, 9, 18, 30), "Atessa", "Artemis", "Palestra"),
            ImportFixture("000692", 1, Leg.FIRST, LocalDateTime(2026, 11, 9, 19, 30), "Vasto", "Atessa", null)
        )
    )
    private val repository = FakeLeagueRepository().apply { preview = pdf }

    private fun vm() = LeagueViewModel(
        repository,
        TestDispatcherProvider(dispatcher),
        AppErrorManager(listOf(NetworkErrorHandler(), FallbackHandler())) {}
    )

    private val LeagueViewModel.data get() = uiState.value.data!!

    @Test
    fun import_showsThePreviewThenConfirmWritesIt() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onPdfPicked { byteArrayOf(1) }
        advanceUntilIdle()

        assertEquals(pdf, vm.data.preview)
        assertNull(vm.data.ours)
        assertNull(repository.league)

        vm.onOursPicked("Artemis")
        vm.onConfirm()
        advanceUntilIdle()

        assertEquals("Artemis", repository.league?.ours?.name)
        assertEquals(2, vm.data.league?.fixtures?.size)
        assertNull(vm.data.preview)
        assertEquals(getString(Res.string.league_imported), vm.toasts.first())
    }

    @Test
    fun confirm_withoutOurs_writesNothing() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onPdfPicked { byteArrayOf(1) }
        advanceUntilIdle()
        vm.onConfirm()
        advanceUntilIdle()

        assertNull(repository.league)
        assertNotNull(vm.data.preview)
    }

    @Test
    fun confirm_offline_keepsThePreviewAndARetryWritesTheSameLeague() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onPdfPicked { byteArrayOf(1) }
        advanceUntilIdle()
        vm.onOursPicked("Artemis")
        repository.failNext = true
        vm.onConfirm()
        advanceUntilIdle()

        assertEquals(getString(Res.string.save_failed), vm.toasts.first())
        assertNotNull(vm.data.preview)

        vm.onConfirm()
        advanceUntilIdle()
        vm.onPdfPicked { byteArrayOf(1) }
        advanceUntilIdle()
        vm.onOursPicked("Artemis")
        vm.onConfirm()
        advanceUntilIdle()

        // The first retry and the reimport: one league.
        assertEquals(1, repository.confirmed.toSet().size)
    }

    @Test
    fun reimport_marksOursAsBeforeAndTellsTheFixturesNoLongerThere() = runTest(dispatcher) {
        val artemis = LeagueTeam("t1", "Artemis", ours = true)
        val atessa = LeagueTeam("t2", "Atessa")
        repository.league = League(
            "l1",
            "U19 - PRIMA FASE",
            listOf(artemis, atessa),
            listOf(
                Fixture(
                    "000691", 1, Leg.FIRST, Instant.parse("2026-11-09T17:30:00Z"), "t2", "t1",
                    status = FixtureStatus.PLAYED, homeScore = 50, awayScore = 60
                ),
                Fixture("000600", 1, Leg.FIRST, null, "t1", "t2")
            )
        )
        val vm = vm()
        advanceUntilIdle()
        vm.onPdfPicked { byteArrayOf(1) }
        advanceUntilIdle()

        assertEquals("Artemis", vm.data.ours)
        assertEquals(listOf("000600"), vm.data.missing.map { it.number })

        vm.onConfirm()
        advanceUntilIdle()

        assertEquals(listOf("l1"), repository.confirmed)
        assertEquals(60, vm.data.league?.fixtures?.first { it.number == "000691" }?.awayScore)
    }

    @Test
    fun import_notACalendar_saysSoAndShowsNoPreview() = runTest(dispatcher) {
        repository.preview = null
        val vm = vm()
        advanceUntilIdle()
        vm.onPdfPicked { byteArrayOf(1) }
        advanceUntilIdle()

        assertTrue(vm.data.unreadable)
        assertNull(vm.data.preview)
        assertFalse(vm.data.busy)
    }

    @Test
    fun import_fileNotRead_toastsAndShowsNoPreview() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onPdfPicked { throw IOException("gone") }
        advanceUntilIdle()

        assertEquals(getString(Res.string.save_failed), vm.toasts.first())
        assertNull(vm.data.preview)
        assertFalse(vm.data.busy)
    }

    @Test
    fun cancel_dropsThePreview() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onPdfPicked { byteArrayOf(1) }
        advanceUntilIdle()
        vm.onCancelPreview()
        advanceUntilIdle()

        assertNull(vm.data.preview)
        assertTrue(repository.confirmed.isEmpty())
    }

    @Test
    fun delete_needsTwoTaps() = runTest(dispatcher) {
        repository.league = League("l1", "U19", emptyList(), emptyList())
        val vm = vm()
        advanceUntilIdle()
        vm.onDelete()
        advanceUntilIdle()

        assertTrue(vm.data.confirmingDelete)
        assertNotNull(repository.league)

        vm.onDelete()
        advanceUntilIdle()

        assertNull(repository.league)
        assertNull(vm.data.league)
        assertEquals(getString(Res.string.league_deleted), vm.toasts.first())
    }
}
