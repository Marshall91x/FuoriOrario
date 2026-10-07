package it.manu.fuoriorario.ui.plan

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.russhwolf.settings.MapSettings
import it.manu.fuoriorario.App
import it.manu.fuoriorario.core.today
import it.manu.fuoriorario.domain.Category
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanCheck
import it.manu.fuoriorario.domain.PlanItem
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.weekOf
import it.manu.fuoriorario.ui.auth.FakeAuthRepository
import it.manu.fuoriorario.ui.auth.awaitText
import it.manu.fuoriorario.ui.auth.runAppTest
import it.manu.fuoriorario.ui.roster.FakeRosterRepository
import it.manu.fuoriorario.ui.shots.FakeShotRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus

private const val ACK = "2026-10-01T10:00:00Z"

/** #15: staff edit and remove a week's exercises and write its note; the player reads the note on top of the plan. */
@OptIn(ExperimentalTestApi::class)
class EditPlanTest {
    private val coach = Member("Coach", Role.STAFF, ACK, id = "coach")
    private val luca = Member("Luca B.", Role.PLAYER, ACK, jerseyNumber = "7", id = "luca")
    private val week = weekOf(today())

    private fun plans() = FakePlanRepository(
        PlanItem(week, "Mikan drill", Category.FOOTWORK, "3 × 20", days = listOf(0, 2), id = "p1", memberId = "luca"),
        PlanItem(week, "Liberi", Category.SHOOTING, days = listOf(5), id = "p2", memberId = "luca"),
        checks = setOf(PlanCheck("p1", 0), PlanCheck("p2", 5))
    )

    @Test
    fun staffEditsAndRemovesExercise() = runAppTest {
        val plans = plans()
        setContent {
            App(
                FakeAuthRepository(coach),
                FakeRosterRepository(coach, luca),
                FakeShotRepository(),
                MapSettings(),
                plans
            )
        }
        awaitText("Piano")
        onNodeWithText("Piano").performClick()
        awaitText("Mikan drill")

        onNodeWithTag("plan_edit_p1").performClick()
        awaitText("MODIFICA ESERCIZIO")
        awaitText("Salva modifiche")
        onNodeWithTag("exercise_title").assertTextEquals("Mikan drill")
        onNodeWithTag("exercise_volume").assertTextEquals("3 × 20")
        // Same validation as adding.
        onNodeWithTag("exercise_title").performTextClearance()
        onNodeWithText("Salva modifiche").performScrollTo().performClick()
        awaitText("Scrivi il nome dell'esercizio.")
        onNodeWithTag("exercise_title").performTextInput("Mikan")
        onNodeWithTag("exercise_volume").performScrollTo().performTextReplacement("4 × 20")
        onNodeWithTag("exercise_day_2").performScrollTo().performClick()

        plans.failNext = true
        onNodeWithText("Salva modifiche").performScrollTo().performClick()
        awaitText("Salvataggio non riuscito. Riprova tra poco.")
        onNodeWithTag("exercise_title").assertTextEquals("Mikan")

        onNodeWithText("Salva modifiche").performScrollTo().performClick()
        awaitText("Esercizio aggiornato")
        awaitText("4 × 20")
        assertEquals(
            PlanItem(week, "Mikan", Category.FOOTWORK, "4 × 20", days = listOf(0), id = "p1", memberId = "luca"),
            plans.items.first()
        )
        // Wednesday is gone: 1 of Monday + 1 of Saturday.
        awaitText("2/2")

        // Removal asks for a second tap, and takes the exercise's checks with it.
        onNodeWithTag("plan_edit_p1").performClick()
        awaitText("Rimuovi")
        onNodeWithText("Rimuovi").performScrollTo().performClick()
        assertEquals(2, plans.items.size)
        awaitText("Conferma rimozione")
        onNodeWithText("Conferma rimozione").performScrollTo().performClick()
        awaitText("Esercizio rimosso")
        awaitText("1/1")
        assertTrue(onAllNodesWithText("Mikan").fetchSemanticsNodes().isEmpty())
        assertEquals(listOf("p2"), plans.items.map { it.id })
        assertEquals(setOf(PlanCheck("p2", 5)), plans.checks)
    }

    @Test
    fun staffWriteNotePlayerReadsIt() = runAppTest {
        val plans = plans()
        setContent {
            App(
                FakeAuthRepository(coach),
                FakeRosterRepository(coach, luca),
                FakeShotRepository(),
                MapSettings(),
                plans
            )
        }
        awaitText("Piano")
        onNodeWithText("Piano").performClick()
        awaitText("Scrivi una nota")
        onNodeWithText("Scrivi una nota").performClick()
        awaitText("Salva nota")
        onNodeWithTag("note").performTextInput("  Obiettivo: 75% ai liberi  ")
        onNodeWithText("Salva nota").performScrollTo().performClick()
        awaitText("Nota salvata")
        awaitText("Obiettivo: 75% ai liberi")
        assertEquals(mapOf(("luca" to week) to "Obiettivo: 75% ai liberi"), plans.notes)

        // Only that week.
        onNodeWithContentDescription("Settimana successiva").performClick()
        awaitText("Torna a questa settimana")
        awaitText("Scrivi una nota")
        onNodeWithText("Torna a questa settimana").performClick()
        awaitText("Modifica nota")

        // Emptied, it goes.
        onNodeWithText("Modifica nota").performClick()
        awaitText("Salva nota")
        onNodeWithTag("note").assertTextEquals("Obiettivo: 75% ai liberi")
        onNodeWithTag("note").performTextClearance()
        onNodeWithText("Salva nota").performScrollTo().performClick()
        awaitText("Scrivi una nota")
        assertTrue(plans.notes.isEmpty())
    }

    @Test
    fun playerReadsNote() = runAppTest {
        val plans = plans()
        plans.notes["luca" to week] = "Obiettivo: 75% ai liberi"
        plans.notes["luca" to week.plus(DatePeriod(days = 7))] = "Prossima"
        setContent { App(FakeAuthRepository(luca), FakeRosterRepository(), FakeShotRepository(), MapSettings(), plans) }
        awaitText("Piano")
        onNodeWithText("Piano").performClick()
        awaitText("NOTA DELLO STAFF")
        awaitText("Obiettivo: 75% ai liberi")
        assertTrue(onAllNodesWithText("Prossima").fetchSemanticsNodes().isEmpty())
        assertTrue(onAllNodesWithTag("plan_edit_p1").fetchSemanticsNodes().isEmpty())
        assertTrue(onAllNodesWithText("Modifica nota").fetchSemanticsNodes().isEmpty())
    }
}
