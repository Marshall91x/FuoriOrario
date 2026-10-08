package it.manu.fuoriorario.ui.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.russhwolf.settings.MapSettings
import it.manu.fuoriorario.domain.Category
import it.manu.fuoriorario.domain.LibraryExercise
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.feature.auth.FakeAuthRepository
import it.manu.fuoriorario.feature.plan.FakePlanRepository
import it.manu.fuoriorario.feature.roster.FakeRosterRepository
import it.manu.fuoriorario.feature.shots.FakeShotRepository
import it.manu.fuoriorario.testing.TestApp
import it.manu.fuoriorario.testing.awaitText
import it.manu.fuoriorario.testing.runAppTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** #18: staff manage the team's library from Squadra → Impostazioni. */
@OptIn(ExperimentalTestApi::class)
class LibrarySettingsTest {
    private val coach = Member("Coach", Role.STAFF, "2026-10-01T10:00:00Z", id = "coach")
    private val mikan = LibraryExercise("Mikan drill", Category.FOOTWORK, "3 × 20 canestri", sort = 0, id = "l1")
    private val liberi = LibraryExercise("Tiri liberi sotto fatica", Category.SHOOTING, sort = 1, id = "l2")

    @Test
    fun staffManageLibrary() = runAppTest {
        val plans = FakePlanRepository(library = listOf(mikan, liberi))
        setContent {
            TestApp(FakeAuthRepository(coach), FakeRosterRepository(coach), FakeShotRepository(), MapSettings(), plans)
        }
        awaitText("Squadra")
        onNodeWithText("Squadra").performClick()
        onNodeWithTag("team_settings").performClick()
        awaitText("Mikan drill")

        // Add, last in the order.
        awaitText("+ Aggiungi alla libreria")
        onNodeWithText("+ Aggiungi alla libreria").performScrollTo().performClick()
        awaitText("Salva nella libreria")
        onNodeWithText("Salva nella libreria").performScrollTo().performClick()
        awaitText("Scrivi il nome dell'esercizio.")
        onNodeWithTag("exercise_title").performTextReplacement("  Arresto e tiro ")
        onNodeWithTag("exercise_volume").performTextReplacement("5 × 10")
        onNodeWithTag("exercise_video").performScrollTo().performTextReplacement("youtube.com")
        onNodeWithText("Salva nella libreria").performScrollTo().performClick()
        awaitText("Il link deve iniziare con https://")
        onNodeWithTag("exercise_video").performScrollTo().performTextReplacement("https://youtu.be/x")
        onNodeWithText("Salva nella libreria").performScrollTo().performClick()
        awaitText("Aggiunto alla libreria")
        awaitText("Arresto e tiro")
        assertEquals(
            LibraryExercise(
                "Arresto e tiro",
                Category.BALL_HANDLING,
                "5 × 10",
                videoUrl = "https://youtu.be/x",
                sort = 2,
                id = "l3"
            ),
            plans.library.last()
        )

        // Edit.
        onNodeWithText("Mikan drill").performClick()
        awaitText("Salva modifiche")
        onNodeWithTag("exercise_volume").performTextReplacement("")
        onNodeWithText("Salva modifiche").performScrollTo().performClick()
        awaitText("Esercizio aggiornato")
        assertEquals(mikan.copy(volume = null), plans.library.first())

        // Reorder.
        onNodeWithTag("library_down_l1").performClick()
        awaitText("Tiri liberi sotto fatica")
        assertEquals(listOf("l2", "l1", "l3"), plans.library.map { it.id })
        onNodeWithTag("library_up_l3").performScrollTo().performClick()
        onNodeWithTag("library_up_l3").performScrollTo().performClick()
        waitUntil { plans.library.first().id == "l3" }
        assertEquals(listOf("l3", "l2", "l1"), plans.library.map { it.id })

        // Delete, on the second tap.
        onNodeWithText("Tiri liberi sotto fatica").performScrollTo().performClick()
        awaitText("Elimina")
        onNodeWithText("Elimina").performScrollTo().performClick()
        awaitText("Conferma eliminazione")
        assertEquals(3, plans.library.size)
        onNodeWithText("Conferma eliminazione").performClick()
        awaitText("Eliminato dalla libreria")
        assertEquals(listOf("l3", "l1"), plans.library.map { it.id })
        assertTrue(onAllNodesWithText("Tiri liberi sotto fatica").fetchSemanticsNodes().isEmpty())
    }
}
