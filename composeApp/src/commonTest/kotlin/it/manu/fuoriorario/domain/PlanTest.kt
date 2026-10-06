package it.manu.fuoriorario.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

class PlanTest {
    private val monday = LocalDate(2026, 10, 5)

    @Test
    fun weekStartsOnMonday() {
        assertEquals(monday, weekOf(monday))
        assertEquals(monday, weekOf(LocalDate(2026, 10, 8)))
        assertEquals(monday, weekOf(LocalDate(2026, 10, 11)))
        // Across a month and a year.
        assertEquals(LocalDate(2025, 12, 29), weekOf(LocalDate(2026, 1, 1)))
        assertEquals(0, dayIndex(monday))
        assertEquals(6, dayIndex(LocalDate(2026, 10, 11)))
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
            PlanItem(monday, "Mikan", Category.FOOTWORK, null, "Piedi", "https://x", listOf(0, 2, 4)),
            newPlanItem(monday, " Mikan ", Category.FOOTWORK, " ", " Piedi ", " https://x ", setOf(4, 0, 2))
        )
    }

    @Test
    fun categoriesAreStoredAsTheDatabaseExpects() {
        // Same list as the check on plan_items.category.
        assertEquals(
            listOf("Ball handling", "Tiro", "Footwork", "Atletica", "Difesa", "Recupero"),
            Category.entries.map { Json.encodeToString(it).trim('"') }
        )
    }

    @Test
    fun progressCountsChecksOnAssignedDays() {
        val items = listOf(
            PlanItem(monday, "Mikan", Category.FOOTWORK, days = listOf(0, 2, 4), id = "a"),
            PlanItem(monday, "Liberi", Category.SHOOTING, days = listOf(1), id = "b")
        )
        assertEquals(Progress(0, 4), progress(items, emptySet()))
        assertEquals(
            Progress(2, 4),
            // A check left on a day no longer assigned, or on another week's exercise, does not count.
            progress(items, setOf(PlanCheck("a", 0), PlanCheck("b", 1), PlanCheck("a", 1), PlanCheck("z", 0)))
        )
        assertEquals(Progress(0, 0), progress(emptyList(), emptySet()))
    }
}
