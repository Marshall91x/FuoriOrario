package it.manu.fuoriorario.ui.plan

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.russhwolf.settings.MapSettings
import it.manu.fuoriorario.core.today
import it.manu.fuoriorario.domain.Category
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanItem
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.weekOf
import it.manu.fuoriorario.feature.auth.FakeAuthRepository
import it.manu.fuoriorario.testing.TestApp
import it.manu.fuoriorario.testing.awaitText
import it.manu.fuoriorario.testing.runAppTest
import it.manu.fuoriorario.ui.roster.FakeRosterRepository
import it.manu.fuoriorario.ui.shots.FakeShotRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus

private const val ACK = "2026-10-01T10:00:00Z"

/** #13: staff add a free exercise to a player's week; the player sees their own week. */
@OptIn(ExperimentalTestApi::class)
class FreeExerciseTest {
    private val coach = Member("Coach", Role.STAFF, ACK, id = "coach")
    private val luca = Member("Luca B.", Role.PLAYER, ACK, jerseyNumber = "7", id = "luca")
    private val marco = Member("Marco", Role.PLAYER, ACK, jerseyNumber = "12", id = "marco")
    private val week = weekOf(today())
    private val nextWeek = week.plus(DatePeriod(days = 7))

    @Test
    fun staffAddsFreeExercise() = runAppTest {
        val plans = FakePlanRepository()
        setContent {
            TestApp(
                FakeAuthRepository(coach),
                FakeRosterRepository(coach, luca, marco),
                FakeShotRepository(),
                MapSettings(),
                plans
            )
        }
        awaitText("Piano")
        onNodeWithText("Piano").performClick()
        awaitText("Nessun esercizio assegnato. Aggiungilo con “Aggiungi esercizio”.")
        awaitText("QUESTA SETTIMANA")

        // The next week, for Luca (first in roster order).
        onNodeWithContentDescription("Settimana successiva").performClick()
        awaitText("Torna a questa settimana")
        onNodeWithText("+ Aggiungi esercizio").performClick()
        awaitText("Aggiungi al piano")
        onNodeWithText("Aggiungi al piano").performScrollTo().performClick()
        awaitText("Scrivi il nome dell'esercizio.")
        onNodeWithTag("exercise_title").performTextInput("Mikan drill")
        // Monday, Wednesday and Friday by default, like the prototype: clear them.
        listOf(0, 2, 4).forEach { onNodeWithTag("exercise_day_$it").performScrollTo().performClick() }
        onNodeWithText("Aggiungi al piano").performScrollTo().performClick()
        awaitText("Scegli almeno un giorno.")
        onNodeWithTag("exercise_day_1").performScrollTo().performClick()
        onNodeWithTag("exercise_day_3").performScrollTo().performClick()
        onNodeWithTag("exercise_category").performScrollTo().performClick()
        onNodeWithText("Footwork").performClick()
        onNodeWithTag("exercise_volume").performScrollTo().performTextInput("3 × 20")
        onNodeWithTag("exercise_description").performScrollTo().performTextInput("Piedi rapidi")
        onNodeWithTag("exercise_video").performScrollTo().performTextInput("http://youtu.be/x")
        onNodeWithText("Aggiungi al piano").performScrollTo().performClick()
        awaitText("Il link deve iniziare con https://")
        onNodeWithTag("exercise_video").performScrollTo().performTextReplacement("https://youtu.be/x")

        // A failed save keeps the sheet as typed.
        plans.failNext = true
        onNodeWithText("Aggiungi al piano").performScrollTo().performClick()
        awaitText("Salvataggio non riuscito. Riprova tra poco.")
        onNodeWithTag("exercise_title").assertTextEquals("Mikan drill")

        onNodeWithText("Aggiungi al piano").performScrollTo().performClick()
        awaitText("Esercizio aggiunto")
        awaitText("Mikan drill")
        awaitText("FOOTWORK")
        assertEquals(
            PlanItem(
                nextWeek, "Mikan drill", Category.FOOTWORK, "3 × 20", "Piedi rapidi", "https://youtu.be/x",
                listOf(
                    1,
                    3
                ),
                id = "p1", memberId = "luca"
            ),
            plans.items.single()
        )

        // Only in that week.
        onNodeWithText("Torna a questa settimana").performClick()
        awaitText("Nessun esercizio assegnato. Aggiungilo con “Aggiungi esercizio”.")
    }

    @Test
    fun playerSeesOwnWeek() = runAppTest {
        val plans = FakePlanRepository(
            PlanItem(
                week, "Mikan drill", Category.FOOTWORK, "3 × 20", "Piedi rapidi", "https://youtu.be/x", listOf(0, 2),
                id = "p1", memberId = "luca"
            ),
            PlanItem(week, "Di Marco", Category.SHOOTING, days = listOf(0), id = "p2", memberId = "marco"),
            PlanItem(nextWeek, "Prossima", Category.SHOOTING, days = listOf(0), id = "p3", memberId = "luca")
        )
        val opened = mutableListOf<String>()
        val uris = object : UriHandler {
            override fun openUri(uri: String) {
                opened += uri
            }
        }
        setContent {
            CompositionLocalProvider(LocalUriHandler provides uris) {
                TestApp(FakeAuthRepository(luca), FakeRosterRepository(), FakeShotRepository(), MapSettings(), plans)
            }
        }
        awaitText("Piano")
        onNodeWithText("Piano").performClick()
        awaitText("Mikan drill")
        awaitText("FOOTWORK")
        awaitText("3 × 20")
        awaitText("Piedi rapidi")
        assertTrue(onAllNodesWithText("Di Marco").fetchSemanticsNodes().isEmpty())
        assertTrue(onAllNodesWithText("Prossima").fetchSemanticsNodes().isEmpty())
        assertTrue(onAllNodesWithText("+ Aggiungi esercizio").fetchSemanticsNodes().isEmpty())
        assertEquals(1, onAllNodesWithContentDescription("Lun: da fare").fetchSemanticsNodes().size)
        assertEquals(1, onAllNodesWithContentDescription("Mer: da fare").fetchSemanticsNodes().size)
        assertEquals(1, onAllNodesWithContentDescription("Mar: riposo").fetchSemanticsNodes().size)

        onNodeWithText("Guarda il video ↗").performScrollTo().performClick()
        assertEquals(listOf("https://youtu.be/x"), opened)

        // Two weeks on: nothing planned yet.
        onNodeWithContentDescription("Settimana successiva").performClick()
        awaitText("Prossima")
        onNodeWithContentDescription("Settimana successiva").performClick()
        awaitText("Lo staff non ha ancora preparato il tuo piano.")
        onNodeWithText("Torna a questa settimana").performClick()
        awaitText("Mikan drill")
    }
}
