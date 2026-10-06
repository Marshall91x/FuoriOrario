package it.manu.fuoriorario.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

class PeriodTest {
    private val today = LocalDate(2026, 10, 6)

    @Test
    fun days_includeTodayAndCountBack() {
        assertEquals(LocalDate(2026, 9, 30), Period.DAYS_7.start(today))
        assertEquals(LocalDate(2026, 9, 7), Period.DAYS_30.start(today))
        // Across the new year.
        assertEquals(LocalDate(2025, 12, 27), Period.DAYS_7.start(LocalDate(2026, 1, 2)))
    }

    @Test
    fun season_startsOnTheFirstOfSeptember() {
        assertEquals(LocalDate(2026, 9, 1), Period.SEASON.start(LocalDate(2026, 9, 1)))
        assertEquals(LocalDate(2025, 9, 1), Period.SEASON.start(LocalDate(2026, 8, 31)))
        assertEquals(LocalDate(2025, 9, 1), Period.SEASON.start(LocalDate(2026, 1, 1)))
        assertEquals(LocalDate(2025, 9, 1), Period.SEASON.start(LocalDate(2025, 12, 31)))
    }

    @Test
    fun filter_keepsSessionsFromStartToToday() {
        fun on(date: LocalDate) = ShotSession(date, mapOf(Zone.TL to Shots(1, 1)))
        val sessions = listOf(today, LocalDate(2026, 9, 30), LocalDate(2026, 9, 29), LocalDate(2026, 8, 31)).map(::on)
        assertEquals(sessions.take(2), Period.DAYS_7.filter(sessions, today))
        assertEquals(sessions.take(3), Period.SEASON.filter(sessions, today))
        // The previous season ends on 31 August.
        assertEquals(sessions.takeLast(1), Period.SEASON.filter(sessions, LocalDate(2026, 8, 31)))
    }

    @Test
    fun stats_sumZonesAcrossSessions() {
        val stats = stats(
            listOf(
                ShotSession(today, mapOf(Zone.PIT to Shots(3, 5), Zone.ACS to Shots(1, 4), Zone.TL to Shots(7, 10))),
                ShotSession(today, mapOf(Zone.CEN to Shots(2, 5), Zone.TL to Shots(1, 2)))
            )
        )
        assertEquals(26, stats.attempted)
        assertEquals(Shots(6, 14), stats.fieldGoal)
        assertEquals(43, stats.fieldGoal.percent) // 42.86
        assertEquals(Shots(3, 9), stats.three)
        assertEquals(33, stats.three.percent)
        assertEquals(Shots(8, 12), stats.freeThrows)
        assertEquals(67, stats.freeThrows.percent) // 66.67
    }

    @Test
    fun stats_withoutAttemptsHaveNoPercent() {
        val stats = stats(listOf(ShotSession(today, mapOf(Zone.PIT to Shots(1, 2)))))
        assertNull(stats.three.percent)
        assertNull(stats.freeThrows.percent)
        val none = stats(emptyList())
        assertEquals(0, none.attempted)
        assertNull(none.fieldGoal.percent)
    }
}
