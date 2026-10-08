package it.manu.fuoriorario.ui.plan

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.russhwolf.settings.MapSettings
import it.manu.fuoriorario.core.today
import it.manu.fuoriorario.domain.Category
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanCheck
import it.manu.fuoriorario.domain.PlanItem
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.weekOf
import it.manu.fuoriorario.feature.auth.FakeAuthRepository
import it.manu.fuoriorario.feature.shots.FakeShotRepository
import it.manu.fuoriorario.testing.TestApp
import it.manu.fuoriorario.testing.awaitText
import it.manu.fuoriorario.testing.runAppTest
import it.manu.fuoriorario.ui.roster.FakeRosterRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.plus

private const val ACK = "2026-10-01T10:00:00Z"

/** #16: staff copy a player's exercises from the week before, without checks or note. */
@OptIn(ExperimentalTestApi::class)
class CopyWeekTest {
    private val coach = Member("Coach", Role.STAFF, ACK, id = "coach")
    private val luca = Member("Luca B.", Role.PLAYER, ACK, jerseyNumber = "7", id = "luca")
    private val week = weekOf(today())
    private val lastWeek = week.minus(DatePeriod(days = 7))

    private fun plans() = FakePlanRepository(
        PlanItem(
            lastWeek,
            "Mikan drill",
            Category.FOOTWORK,
            "3 × 20",
            days = listOf(0, 2),
            id = "p1",
            memberId = "luca"
        ),
        PlanItem(lastWeek, "Liberi", Category.SHOOTING, days = listOf(5), id = "p2", memberId = "luca"),
        checks = setOf(PlanCheck("p1", 0), PlanCheck("p2", 5))
    ).apply { notes["luca" to lastWeek] = "Vecchia nota" }

    private suspend fun ComposeUiTest.open(plans: FakePlanRepository) {
        setContent {
            TestApp(
                FakeAuthRepository(coach),
                FakeRosterRepository(coach, luca),
                FakeShotRepository(),
                MapSettings(),
                plans
            )
        }
        awaitText("Piano")
        onNodeWithText("Piano").performClick()
        awaitText("Copia settimana precedente")
    }

    @Test
    fun copiesExercisesOnly() = runAppTest {
        val plans = plans()
        open(plans)
        awaitText("Nessun esercizio assegnato. Aggiungilo con “Aggiungi esercizio”.")

        plans.failNext = true
        onNodeWithText("Copia settimana precedente").performClick()
        awaitText("Salvataggio non riuscito. Riprova tra poco.")

        onNodeWithText("Copia settimana precedente").performClick()
        awaitText("Esercizi copiati")
        awaitText("Mikan drill")
        awaitText("Liberi")
        awaitText("0/3")
        assertTrue(onAllNodesWithText("Vecchia nota").fetchSemanticsNodes().isEmpty())
        assertEquals(
            listOf(
                PlanItem(
                    week,
                    "Mikan drill",
                    Category.FOOTWORK,
                    "3 × 20",
                    days = listOf(0, 2),
                    id = "p3",
                    memberId = "luca"
                ),
                PlanItem(week, "Liberi", Category.SHOOTING, days = listOf(5), id = "p4", memberId = "luca")
            ),
            plans.items.filter { it.week == week }
        )
        assertEquals(2, plans.checks.size)

        // Not empty any more: a second tap confirms, then adds again.
        onNodeWithText("Copia settimana precedente").performClick()
        awaitText("Conferma copia")
        assertEquals(2, plans.items.count { it.week == week })
        onNodeWithText("Conferma copia").performClick()
        awaitText("Esercizi copiati")
        assertEquals(4, plans.items.count { it.week == week })
    }

    @Test
    fun emptyPreviousWeekChangesNothing() = runAppTest {
        val plans = plans()
        open(plans)
        onNodeWithContentDescription("Settimana successiva").performClick()
        awaitText("Torna a questa settimana")
        onNodeWithText("Copia settimana precedente").performClick()
        awaitText("La settimana precedente non ha esercizi")
        assertEquals(2, plans.items.size)
        assertTrue(plans.items.none { it.week == week.plus(DatePeriod(days = 7)) })
    }

    @Test
    fun emptyPreviousWeekDoesNotAskToConfirm() = runAppTest {
        val plans = plans()
        open(plans)
        // Last week has exercises, the one before has none.
        onNodeWithContentDescription("Settimana precedente").performClick()
        awaitText("Mikan drill")
        onNodeWithText("Copia settimana precedente").performClick()
        awaitText("La settimana precedente non ha esercizi")
        assertTrue(onAllNodesWithText("Conferma copia").fetchSemanticsNodes().isEmpty())
        assertEquals(2, plans.items.size)
    }

    @Test
    fun weekChangedMeanwhileCopiesNothing() = runAppTest {
        val plans = plans()
        open(plans)
        awaitText("Nessun esercizio assegnato. Aggiungilo con “Aggiungi esercizio”.")
        // Another staff fills the week after it was loaded here.
        plans.items +=
            PlanItem(week, "Tiro dal palleggio", Category.SHOOTING, days = listOf(1), id = "x1", memberId = "luca")

        onNodeWithText("Copia settimana precedente").performClick()
        awaitText("Il piano è cambiato nel frattempo: ecco quello aggiornato. Riprova.")
        awaitText("Tiro dal palleggio")
        assertEquals(listOf("x1"), plans.items.filter { it.week == week }.map { it.id })
    }
}
