package it.manu.fuoriorario.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

class TeamOverviewTest {
    private val today = LocalDate(2026, 10, 7)

    @Test
    fun planPercentRoundsLikeThePrototype() {
        assertEquals(null, Progress(0, 0).percent)
        assertEquals(0, Progress(0, 4).percent)
        assertEquals(67, Progress(2, 3).percent)
        assertEquals(50, Progress(1, 2).percent)
        assertEquals(100, Progress(3, 3).percent)
    }

    @Test
    fun planLevelThresholds() {
        assertEquals(PlanLevel.LOW, planLevel(null))
        assertEquals(PlanLevel.LOW, planLevel(39))
        assertEquals(PlanLevel.MID, planLevel(40))
        assertEquals(PlanLevel.MID, planLevel(74))
        assertEquals(PlanLevel.GOOD, planLevel(75))
        // 74.6% rounds to 75: the pill matches the number it shows.
        assertEquals(PlanLevel.GOOD, planLevel(Progress(103, 138).percent))
    }

    @Test
    fun rowCountsShotsOver7And30Days() {
        val sessions = listOf(
            ShotSession(today, mapOf(Zone.PIT to Shots(3, 5), Zone.TL to Shots(7, 10))),
            // 7th day back: still in the 7 days.
            ShotSession(LocalDate(2026, 10, 1), mapOf(Zone.CEN to Shots(2, 5))),
            // 30 days only.
            ShotSession(LocalDate(2026, 9, 8), mapOf(Zone.ACS to Shots(1, 5), Zone.TL to Shots(2, 2))),
            // Out of both.
            ShotSession(LocalDate(2026, 9, 7), mapOf(Zone.ACS to Shots(9, 9), Zone.TL to Shots(9, 9)))
        )
        assertEquals(
            OverviewRow(plan = 50, attempted7 = 20, freeThrows30 = 75, three30 = 30),
            overviewRow(Progress(1, 2), sessions, today)
        )
    }

    @Test
    fun rowWithoutDataIsEmpty() {
        assertEquals(OverviewRow(null, 0, null, null), overviewRow(Progress(0, 0), emptyList(), today))
    }
}
