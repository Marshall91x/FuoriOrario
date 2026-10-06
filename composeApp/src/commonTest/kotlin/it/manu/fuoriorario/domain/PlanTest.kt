package it.manu.fuoriorario.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

class PlanTest {
    private val monday = LocalDate(2026, 10, 5)

    @Test
    fun weekStartsOnMonday() {
        assertEquals(monday, weekOf(monday))
        assertEquals(monday, weekOf(LocalDate(2026, 10, 8)))
        assertEquals(monday, weekOf(LocalDate(2026, 10, 11)))
        // Across a month and a year.
        assertEquals(LocalDate(2025, 12, 29), weekOf(LocalDate(2026, 1, 1)))
    }

    @Test
    fun formErrorsInOrder() {
        assertEquals(PlanItemError.TITLE, planItemError(" ", emptySet(), "http://x"))
        assertEquals(PlanItemError.NO_DAYS, planItemError("Mikan", emptySet(), "http://x"))
        assertEquals(PlanItemError.VIDEO, planItemError("Mikan", setOf(0), "http://x"))
        assertEquals(PlanItemError.VIDEO, planItemError("Mikan", setOf(0), "www.youtube.com"))
        assertNull(planItemError("Mikan", setOf(0), " https://youtu.be/x "))
        assertNull(planItemError("Mikan", setOf(0), ""))
    }

    @Test
    fun newItemIsTrimmedWithSortedDays() {
        assertEquals(
            PlanItem(monday, "Mikan", "Footwork", null, "Piedi", "https://x", listOf(0, 2, 4)),
            newPlanItem(monday, " Mikan ", "Footwork", " ", " Piedi ", " https://x ", setOf(4, 0, 2))
        )
    }
}
