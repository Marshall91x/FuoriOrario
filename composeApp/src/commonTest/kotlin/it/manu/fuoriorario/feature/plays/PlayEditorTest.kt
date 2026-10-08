package it.manu.fuoriorario.feature.plays

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.russhwolf.settings.MapSettings
import it.manu.fuoriorario.core.session.Session
import it.manu.fuoriorario.domain.COURT_MARGIN
import it.manu.fuoriorario.domain.COURT_WIDTH
import it.manu.fuoriorario.domain.CourtSize
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Play
import it.manu.fuoriorario.domain.PlayCategory
import it.manu.fuoriorario.domain.Point
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.startingStep
import it.manu.fuoriorario.feature.auth.FakeAuthRepository
import it.manu.fuoriorario.feature.shots.FakeShotRepository
import it.manu.fuoriorario.testing.TestApp
import it.manu.fuoriorario.testing.awaitNode
import it.manu.fuoriorario.testing.awaitText
import it.manu.fuoriorario.testing.runAppTest
import it.manu.fuoriorario.ui.plan.FakePlanRepository
import it.manu.fuoriorario.ui.roster.FakeRosterRepository
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val ACK = "2026-10-01T10:00:00Z"

/** #45: staff draw the plays on the phone. */
@OptIn(ExperimentalTestApi::class)
class PlayEditorTest {
    private val luca = Member("Luca B.", Role.PLAYER, ACK, id = "luca")
    private val coach = Member("Coach", Role.STAFF, ACK, id = "coach")

    private val box = Play(
        "Box",
        PlayCategory.BASELINE_INBOUND,
        court = CourtSize.HALF,
        defense = false,
        steps = listOf(startingStep(defense = false)),
        id = "box"
    )

    private fun ComposeUiTest.open(auth: FakeAuthRepository, plays: FakePlayRepository) {
        setContent {
            TestApp(
                auth,
                FakeRosterRepository(coach, luca),
                FakeShotRepository(),
                MapSettings(),
                FakePlanRepository(),
                plays
            )
        }
    }

    /** Where [at] is on the editor's half court, in the node's pixels. */
    private fun ComposeUiTest.onCourt(at: Point, action: TouchInjectionScope.(Offset) -> Unit) =
        onNodeWithTag("play_court").performScrollTo().performTouchInput {
            val scale = width / (COURT_WIDTH + 2 * COURT_MARGIN)
            action(Offset((at.x + COURT_MARGIN) * scale, (at.y + COURT_MARGIN) * scale))
        }

    @Test
    fun staffDrawATwoStepPlayAndThePlayerSeesIt() = runAppTest {
        val auth = FakeAuthRepository(coach)
        val plays = FakePlayRepository()
        open(auth, plays)

        awaitText("Schemi")
        onNodeWithText("Schemi").performClick()
        awaitText("Nuovo schema")
        onNodeWithText("Nuovo schema").performClick()

        // No title: not saved.
        onNodeWithTag("play_save").performScrollTo().performClick()
        awaitText("Dai un titolo allo schema.")
        onNodeWithTag("play_title").performTextInput("Spain")

        onNodeWithTag("step_add").performScrollTo().performClick()
        awaitText("Passo 2 di 2")
        // 1 drives up, 2 gets the ball, 5 screens.
        onNodeWithTag("play_court").performScrollTo().performTouchInput {
            val scale = width / (COURT_WIDTH + 2 * COURT_MARGIN)
            swipe(Offset(300 * scale, 380 * scale), Offset(300 * scale, 250 * scale))
        }
        onCourt(Point(60f, 230f)) { longClick(it) }
        onNodeWithTag("screen_5").performScrollTo().performClick()
        onNodeWithTag("step_note").performScrollTo().performTextInput("1 sale, palla a 2.")
        onNodeWithTag("play_save").performScrollTo().performClick()

        // Saved, and open in the viewer.
        awaitText("Passo 1 di 2")
        val saved = plays.plays.single()
        assertEquals("Spain", saved.title)
        val step = saved.steps[1]
        assertTrue(abs(step.pos.getValue("1").y - 200f) < 2f, "${step.pos["1"]}")
        assertEquals("2", step.ball)
        assertEquals(listOf("5"), step.screens)
        assertEquals("1 sale, palla a 2.", step.note)
        assertEquals(startingStep(defense = false), saved.steps[0])

        auth.session.value = Session.SignedIn(luca)
        awaitText("Schemi")
        onNodeWithText("Schemi").performClick()
        awaitText("Spain")
        // Players can't edit.
        awaitNode(hasTestTag("play_new"), count = 0)
        onNodeWithText("Spain").performClick()
        awaitText("Passo 1 di 2")
        awaitNode(hasTestTag("play_edit"), count = 0)
    }

    @Test
    fun aFullCourtPlayWithDefenseIsSavedAsDrawn() = runAppTest {
        val plays = FakePlayRepository()
        open(FakeAuthRepository(coach), plays)

        awaitText("Schemi")
        onNodeWithText("Schemi").performClick()
        awaitText("Nuovo schema")
        onNodeWithText("Nuovo schema").performClick()
        onNodeWithTag("play_title").performTextInput("Pressing")
        onNodeWithTag("play_description").performTextInput("Tutto campo.")
        onNodeWithTag("court_FULL").performClick()
        onNodeWithTag("play_defense").performScrollTo().performClick()
        onNodeWithTag("play_save").performScrollTo().performClick()

        awaitText("Passo 1 di 1")
        assertEquals(
            Play(
                "Pressing",
                PlayCategory.ATTACK,
                "Tutto campo.",
                CourtSize.FULL,
                defense = true,
                steps = listOf(startingStep(defense = true)),
                id = "play0"
            ),
            plays.plays.single()
        )

        // Reopened, identical; the court can't change any more.
        onNodeWithTag("play_edit").performScrollTo().performClick()
        awaitNode(hasTestTag("court_FULL"), count = 0)
        onNodeWithTag("play_save").performScrollTo().performClick()
        awaitText("Passo 1 di 1")
        assertEquals(startingStep(defense = true), plays.plays.single().steps.single())
    }

    @Test
    fun stepsComeAndGoUpToTwenty() = runAppTest {
        val plays = FakePlayRepository(box)
        open(FakeAuthRepository(coach), plays)

        awaitText("Schemi")
        onNodeWithText("Schemi").performClick()
        onNodeWithText("Box").performClick()
        onNodeWithTag("play_edit").performScrollTo().performClick()

        repeat(19) { onNodeWithTag("step_add").performScrollTo().performClick() }
        awaitText("Passo 20 di 20")
        onNodeWithTag("step_add").assertIsNotEnabled()
        onNodeWithTag("step_remove").performScrollTo().performClick()
        awaitText("Passo 19 di 19")
        onNodeWithTag("play_save").performScrollTo().performClick()
        awaitText("Passo 1 di 19")
        assertEquals(19, plays.plays.single().steps.size)
    }

    @Test
    fun leavingUnsavedAsksFirst() = runAppTest {
        val plays = FakePlayRepository(box)
        open(FakeAuthRepository(coach), plays)

        awaitText("Schemi")
        onNodeWithText("Schemi").performClick()
        onNodeWithText("Box").performClick()
        onNodeWithTag("play_edit").performScrollTo().performClick()
        // Nothing changed: out at once.
        onNodeWithTag("editor_back").performScrollTo().performClick()
        awaitText("Passo 1 di 1")

        onNodeWithTag("play_edit").performScrollTo().performClick()
        onNodeWithTag("play_title").performTextInput(" 2")
        onNodeWithTag("editor_back").performScrollTo().performClick()
        awaitText("Esci senza salvare")
        onNodeWithTag("editor_back").performClick()
        awaitText("Passo 1 di 1")
        assertEquals("Box", plays.plays.single().title)
    }

    @Test
    fun anotherTabWithUnsavedChangesTakesASecondTap() = runAppTest {
        val plays = FakePlayRepository(box)
        open(FakeAuthRepository(coach), plays)

        awaitText("Schemi")
        onNodeWithText("Schemi").performClick()
        onNodeWithText("Box").performClick()
        onNodeWithTag("play_edit").performScrollTo().performClick()
        onNodeWithTag("play_title").performTextInput(" 2")
        onNodeWithText("Piano").performClick()
        awaitText("Modifiche non salvate: ripeti per uscire senza salvarle.")
        awaitNode(hasTestTag("play_title"))
        onNodeWithText("Piano").performClick()
        awaitNode(hasTestTag("play_title"), count = 0)

        onNodeWithText("Schemi").performClick()
        awaitText("Box")
        assertEquals("Box", plays.plays.single().title)
    }

    @Test
    fun aFingerFarFromEveryPieceMovesNothing() = runAppTest {
        val plays = FakePlayRepository(box)
        open(FakeAuthRepository(coach), plays)

        awaitText("Schemi")
        onNodeWithText("Schemi").performClick()
        onNodeWithText("Box").performClick()
        onNodeWithTag("play_edit").performScrollTo().performClick()
        // The middle of the court, away from the default layout.
        onCourt(Point(250f, 210f)) { swipe(it, it + Offset(0f, -100f)) }
        onNodeWithTag("play_save").performScrollTo().performClick()
        awaitText("Passo 1 di 1")
        assertEquals(startingStep(defense = false), plays.plays.single().steps.single())
    }

    @Test
    fun aSlowDragMovesThePieceAndKeepsTheBall() = runAppTest {
        val plays = FakePlayRepository(box)
        open(FakeAuthRepository(coach), plays)

        awaitText("Schemi")
        onNodeWithText("Schemi").performClick()
        onNodeWithText("Box").performClick()
        onNodeWithTag("play_edit").performScrollTo().performClick()
        // A finger dragging 2 down slowly, 1 px every 30 ms: past the hold time before a big move.
        onCourt(Point(60f, 230f)) { start ->
            down(start)
            repeat(80) {
                advanceEventTime(30)
                moveBy(Offset(0f, 1f))
            }
            up()
        }
        onNodeWithTag("play_save").performScrollTo().performClick()
        awaitText("Passo 1 di 1")
        val step = plays.plays.single().steps.single()
        assertEquals("1", step.ball)
        assertTrue(step.pos.getValue("2").y > 240f, "${step.pos["2"]}")
    }

    @Test
    fun staffDeleteAPlayWithASecondTap() = runAppTest {
        val plays = FakePlayRepository(box)
        open(FakeAuthRepository(coach), plays)

        awaitText("Schemi")
        onNodeWithText("Schemi").performClick()
        onNodeWithText("Box").performClick()
        onNodeWithTag("play_edit").performScrollTo().performClick()
        onNodeWithTag("play_remove").performScrollTo().performClick()
        awaitText("Conferma eliminazione")
        assertEquals(1, plays.plays.size)
        onNodeWithTag("play_remove").performClick()
        awaitText("Ancora nessuno schema.")
        assertEquals(0, plays.plays.size)
    }
}
