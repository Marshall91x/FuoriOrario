package it.manu.fuoriorario.feature.plays

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigationevent.DirectNavigationEventInput
import com.russhwolf.settings.MapSettings
import it.manu.fuoriorario.domain.CourtSize
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Play
import it.manu.fuoriorario.domain.PlayCategory
import it.manu.fuoriorario.domain.Point
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.Step
import it.manu.fuoriorario.feature.auth.FakeAuthRepository
import it.manu.fuoriorario.feature.plan.FakePlanRepository
import it.manu.fuoriorario.feature.roster.FakeRosterRepository
import it.manu.fuoriorario.feature.shots.FakeShotRepository
import it.manu.fuoriorario.testing.TestApp
import it.manu.fuoriorario.testing.awaitNode
import it.manu.fuoriorario.testing.awaitText
import it.manu.fuoriorario.testing.runAppTest
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred

private const val ACK = "2026-10-01T10:00:00Z"

/** #44: the whole team browses the plays and steps through one. */
@OptIn(ExperimentalTestApi::class)
class PlaysTest {
    private val luca = Member("Luca B.", Role.PLAYER, ACK, id = "luca")
    private val coach = Member("Coach", Role.STAFF, ACK, id = "coach")

    private val start = mapOf(
        "1" to Point(250f, 330f),
        "2" to Point(60f, 230f),
        "3" to Point(440f, 230f),
        "4" to Point(40f, 40f),
        "5" to Point(330f, 150f)
    )

    /** The one in supabase/seed.sql. */
    private val seed = Play(
        "Pick and roll centrale",
        PlayCategory.ATTACK,
        "Blocco del centro per il playmaker in punta, poi taglio a canestro.",
        CourtSize.HALF,
        defense = false,
        steps = listOf(
            Step(start, "1", note = "1 porta palla in punta, 5 al gomito destro."),
            Step(
                start + ("5" to Point(285f, 305f)),
                "1",
                screens = listOf("5"),
                note = "5 sale e porta il blocco sulla destra di 1."
            ),
            Step(
                start + mapOf("1" to Point(370f, 250f), "3" to Point(460f, 60f), "5" to Point(270f, 120f)),
                "1",
                curves = mapOf("1" to Point(340f, 330f)),
                note = "1 sfrutta il blocco in palleggio, 5 rolla verso canestro, 3 scende in angolo."
            ),
            Step(
                start + mapOf("1" to Point(370f, 250f), "3" to Point(460f, 60f), "5" to Point(270f, 120f)),
                "5",
                note = "Passaggio a 5 sul taglio."
            )
        ),
        id = "seed"
    )

    private val inbound = Play(
        "Box",
        PlayCategory.BASELINE_INBOUND,
        court = CourtSize.FULL,
        defense = true,
        steps = listOf(Step(start + ("X1" to Point(250f, 300f)), "1")),
        id = "box"
    )

    @Test
    fun theLoaderShowsUntilThePlaysArrive() = runAppTest {
        val gate = CompletableDeferred<Unit>()
        setContent {
            TestApp(
                FakeAuthRepository(luca),
                FakeRosterRepository(luca),
                FakeShotRepository(),
                MapSettings(),
                FakePlanRepository(),
                FakePlayRepository(seed).apply { loadGate = gate }
            )
        }

        awaitText("Schemi")
        onNodeWithText("Schemi").performClick()
        awaitNode(hasTestTag("loader"))

        gate.complete(Unit)
        awaitText("Pick and roll centrale")
        awaitNode(hasTestTag("loader"), count = 0)
    }

    @Test
    fun aPlayOpenHasNoTabBarAndTheSystemBackLeavesIt() = runAppTest {
        val back = DirectNavigationEventInput()
        setContent {
            TestApp(
                FakeAuthRepository(luca),
                FakeRosterRepository(luca),
                FakeShotRepository(),
                MapSettings(),
                FakePlanRepository(),
                FakePlayRepository(inbound, seed),
                back
            )
        }

        awaitText("Schemi")
        onNodeWithText("Schemi").performClick()
        onNodeWithText("Box").performClick()
        awaitText("Passo 1 di 1")
        awaitNode(hasText("Piano"), count = 0)

        runOnIdle { back.backCompleted() }
        awaitText("Pick and roll centrale")
        awaitText("Piano")
    }

    @Test
    fun playerOpensThePlayAndStepsThrough() = runAppTest {
        setContent {
            TestApp(
                FakeAuthRepository(luca),
                FakeRosterRepository(luca),
                FakeShotRepository(),
                MapSettings(),
                FakePlanRepository(),
                FakePlayRepository(inbound, seed)
            )
        }

        awaitText("Schemi")
        onNodeWithText("Schemi").performClick()
        // Categories in their order.
        awaitText("ATTACCO")
        awaitText("RIMESSA DAL FONDO")
        onNodeWithText("Pick and roll centrale").performClick()

        awaitText("Passo 1 di 4")
        awaitText("1 porta palla in punta, 5 al gomito destro.")
        onNodeWithTag("step_next").performScrollTo().performClick()
        awaitText("Passo 2 di 4")
        awaitText("5 sale e porta il blocco sulla destra di 1.")
        onNodeWithTag("step_prev").performClick()
        awaitText("Passo 1 di 4")

        onNodeWithTag("play_toggle").performScrollTo().performClick()
        awaitText("Passo 4 di 4")
        awaitText("Passaggio a 5 sul taglio.")
        // #71: at the end it plays again from step 1, the first frame already moving into step 2. The clock stands
        // still to catch it there.
        mainClock.autoAdvance = false
        onNodeWithTag("play_toggle").assertIsEnabled().performClick()
        mainClock.advanceTimeByFrame()
        onNodeWithText("Passo 2 di 4").assertExists()
        onNodeWithText("5 sale e porta il blocco sulla destra di 1.").assertExists()
        onNodeWithTag("play_toggle").assertIsNotEnabled()
        mainClock.autoAdvance = true
        awaitText("Passo 4 di 4")

        onNodeWithTag("plays_back").performScrollTo().performClick()
        awaitText("Box")
    }

    @Test
    fun staffSeeThePlaysToo() = runAppTest {
        setContent {
            TestApp(
                FakeAuthRepository(coach),
                FakeRosterRepository(coach),
                FakeShotRepository(),
                MapSettings(),
                FakePlanRepository(),
                FakePlayRepository()
            )
        }

        awaitText("Schemi")
        onNodeWithText("Schemi").performClick()
        awaitText("Ancora nessuno schema.")
        awaitNode(hasText("ATTACCO"), count = 0)
    }
}
