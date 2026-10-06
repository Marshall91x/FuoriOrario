package it.manu.fuoriorario.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class TrendTest {
    private val start = LocalDate(2026, 9, 1)

    @Test
    fun trend_oldestFirstWithBothSeries() {
        val sessions = listOf(
            ShotSession(start.plus(DatePeriod(days = 1)), mapOf(Zone.TL to Shots(7, 10)), id = "b"),
            ShotSession(start, mapOf(Zone.PIT to Shots(1, 3), Zone.TL to Shots(1, 2)), id = "a")
        )
        assertEquals(
            listOf(TrendPoint(start, 33, 50), TrendPoint(start.plus(DatePeriod(days = 1)), null, 70)),
            trend(sessions)
        )
    }

    @Test
    fun trend_keepsTheLast14() {
        // Newest first, like the repository.
        val sessions = (0 until 20).map {
            ShotSession(
                start.plus(DatePeriod(days = 19 - it)),
                mapOf(
                    Zone.PIT to Shots(it, 20)
                )
            )
        }
        val points = trend(sessions)
        assertEquals(14, points.size)
        assertEquals(start.plus(DatePeriod(days = 6)), points.first().date)
        assertEquals(65, points.first().fieldGoal)
        assertEquals(0, points.last().fieldGoal)
    }

    @Test
    fun trend_skipsSessionsWithoutShots() {
        assertEquals(emptyList(), trend(listOf(ShotSession(start, emptyMap()))))
    }
}
