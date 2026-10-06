package it.manu.fuoriorario.ui.plan

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.russhwolf.settings.MapSettings
import it.manu.fuoriorario.App
import it.manu.fuoriorario.core.today
import it.manu.fuoriorario.domain.Category
import it.manu.fuoriorario.domain.LibraryExercise
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanItem
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.weekOf
import it.manu.fuoriorario.ui.auth.FakeAuthRepository
import it.manu.fuoriorario.ui.auth.awaitText
import it.manu.fuoriorario.ui.roster.FakeRosterRepository
import it.manu.fuoriorario.ui.shots.FakeShotRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val ACK = "2026-10-01T10:00:00Z"

/** UI test 5 (#17): staff assign an exercise from the team's library, changing it before saving. */
@OptIn(ExperimentalTestApi::class)
class LibraryExerciseTest {
    private val coach = Member("Coach", Role.STAFF, ACK, id = "coach")
    private val luca = Member("Luca B.", Role.PLAYER, ACK, jerseyNumber = "7", id = "luca")
    private val mikan = LibraryExercise(
        "Mikan drill",
        Category.FOOTWORK,
        "3 × 20 canestri",
        "Piedi rapidi, palla alta.",
        id = "l1"
    )
    private val liberi = LibraryExercise("Tiri liberi sotto fatica", Category.SHOOTING, id = "l2")

    @Test
    fun staffAssignsFromLibrary() = runComposeUiTest {
        val plans = FakePlanRepository(library = listOf(mikan, liberi))
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
        awaitText("+ Aggiungi esercizio")
        onNodeWithText("+ Aggiungi esercizio").performClick()
        awaitText("DALLA LIBRERIA")
        awaitText("Tiri liberi sotto fatica")
        onNodeWithText("Mikan drill").performClick()
        onNodeWithTag("exercise_title").assertTextEquals("Mikan drill")
        onNodeWithTag("exercise_volume").assertTextEquals("3 × 20 canestri")
        onNodeWithTag("exercise_description").assertTextEquals("Piedi rapidi, palla alta.")
        onNodeWithTag("exercise_category").assertTextEquals("Footwork")

        // Changed before saving: only the plan gets it.
        onNodeWithTag("exercise_volume").performScrollTo().performTextReplacement("2 × 20")
        onNodeWithText("Aggiungi al piano").performScrollTo().performClick()
        awaitText("Esercizio aggiunto")
        awaitText("2 × 20")
        assertEquals(
            PlanItem(
                weekOf(today()),
                "Mikan drill",
                Category.FOOTWORK,
                "2 × 20",
                "Piedi rapidi, palla alta.",
                days = listOf(0, 2, 4),
                id = "p1",
                memberId = "luca"
            ),
            plans.items.single()
        )

        // Editing an exercise offers no library.
        onNodeWithText("Modifica").performClick()
        awaitText("Salva modifiche")
        assertTrue(onAllNodesWithText("DALLA LIBRERIA").fetchSemanticsNodes().isEmpty())
    }
}
