package it.manu.fuoriorario.feature.games

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.russhwolf.settings.MapSettings
import it.manu.fuoriorario.domain.CallUp
import it.manu.fuoriorario.domain.Game
import it.manu.fuoriorario.domain.GameEvent
import it.manu.fuoriorario.domain.GameEventType
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Quarter
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.feature.auth.FakeAuthRepository
import it.manu.fuoriorario.feature.roster.FakeRosterRepository
import it.manu.fuoriorario.testing.TestApp
import it.manu.fuoriorario.testing.awaitNode
import it.manu.fuoriorario.testing.awaitText
import it.manu.fuoriorario.testing.runAppTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

private const val ACK = "2026-10-01T10:00:00Z"

/** #80: staff record a game live and save it at the end. #82: its Riepilogo. */
@OptIn(ExperimentalTestApi::class)
class GamesTest {
    private val coach = Member("Coach", Role.STAFF, ACK, id = "coach")
    private val luca = Member("Luca B.", Role.PLAYER, ACK, id = "luca", jerseyNumber = "7")

    @Test
    fun staffCreateAGameScoreAShotAndFinishIt() = runAppTest {
        val games = FakeGameRepository()
        setContent {
            TestApp(FakeAuthRepository(coach), FakeRosterRepository(coach, luca), games = games)
        }

        awaitText("Partite")
        onNodeWithText("Partite").performClick()
        awaitText("Ancora nessuna partita.")
        onNodeWithTag("game_new").performClick()

        onNodeWithTag("game_opponent").performTextInput("Virtus")
        onNodeWithTag("call_up_luca").performClick()
        onNodeWithTag("game_start").performScrollTo().performClick()

        // Live is a second-level screen: no tab bar.
        awaitText("NOI 0 – 0 VIRTUS")
        awaitNode(hasText("Piano"), count = 0)
        onNodeWithTag("pick_luca").performClick()
        onNodeWithContentDescription("Tripla centrale").performScrollTo().performClick()
        onNodeWithTag("shot_made").performScrollTo().performClick()
        awaitText("NOI 3 – 0 VIRTUS")
        awaitText("1Q · #7 Luca B. · Tripla centrale ✓")

        onNodeWithTag("game_finish").performScrollTo().performClick()
        awaitText("Virtus")
        awaitText("3–0")
        awaitText("Piano")
        assertEquals(1, games.saved.size)
    }

    @Test
    fun aGameInProgressSurvivesARestartAndBlocksANewOne() = runAppTest {
        val prefs = MapSettings()
        var launch by mutableIntStateOf(0)
        setContent {
            key(launch) { TestApp(FakeAuthRepository(coach), FakeRosterRepository(coach, luca), prefs = prefs) }
        }

        awaitText("Partite")
        onNodeWithText("Partite").performClick()
        onNodeWithTag("game_new").performClick()
        onNodeWithTag("game_opponent").performTextInput("Virtus")
        onNodeWithTag("game_start").performScrollTo().performClick()
        onNodeWithTag("opponent_2").performScrollTo().performClick()
        awaitText("NOI 0 – 2 VIRTUS")

        launch++
        awaitText("Partite")
        onNodeWithText("Partite").performClick()
        awaitText("PARTITA IN CORSO")
        onNodeWithTag("game_new").assertIsNotEnabled()
        onNodeWithTag("game_resume").performClick()
        awaitText("NOI 0 – 2 VIRTUS")
    }

    @Test
    fun playersSeeTheGamesWithoutCreatingOne() = runAppTest {
        val games =
            FakeGameRepository(Game(LocalDate(2026, 10, 3), "Fortitudo", home = false, ourScore = 61, theirScore = 58))
        setContent {
            TestApp(FakeAuthRepository(luca), FakeRosterRepository(luca), games = games)
        }

        awaitText("Partite")
        onNodeWithText("Partite").performClick()
        awaitText("Fortitudo")
        awaitText("61–58")
        awaitNode(hasText("+ Nuova partita"), count = 0)
    }

    private val fortitudo =
        Game(LocalDate(2026, 10, 3), "Fortitudo", home = false, ourScore = 61, theirScore = 58, id = "g1")

    @Test
    fun aPlayerOpensAGameAndSeesOnlyTheirOwnRow() = runAppTest {
        // What RLS shows Luca: his call-up and his events only.
        val games = FakeGameRepository(fortitudo).apply {
            callUps["g1"] = listOf(CallUp("luca", "Luca B.", "7"))
            events["g1"] = listOf(
                GameEvent(GameEventType.SHOT, Quarter.Q1, "luca", Zone.CEN, true),
                GameEvent(GameEventType.SHOT, Quarter.Q2, "luca", Zone.PIT, true)
            )
        }
        setContent { TestApp(FakeAuthRepository(luca), FakeRosterRepository(luca), games = games) }

        awaitText("Partite")
        onNodeWithText("Partite").performClick()
        awaitText("5 PT")
        onNodeWithText("Fortitudo").performClick()

        awaitText("NOI 61 – 58 FORTITUDO")
        awaitNode(hasText("Piano"), count = 0)
        onNodeWithTag("box_luca_points", useUnmergedTree = true).assertTextEquals("5")
        awaitNode(hasText("Elimina"), count = 0)
        onNodeWithTag("summary_q2").performClick()
        onNodeWithTag("box_luca_points", useUnmergedTree = true).assertTextEquals("2")
    }

    @Test
    fun staffDeleteAGameWithASecondTap() = runAppTest {
        val games = FakeGameRepository(fortitudo).apply {
            callUps["g1"] = listOf(CallUp("anna", "Anna", "4"), CallUp("luca", "Luca B.", "7"))
            events["g1"] = listOf(GameEvent(GameEventType.OPPONENT, Quarter.Q1, value = 2))
        }
        setContent { TestApp(FakeAuthRepository(coach), FakeRosterRepository(coach, luca), games = games) }

        awaitText("Partite")
        onNodeWithText("Partite").performClick()
        onNodeWithText("Fortitudo").performClick()
        onNodeWithTag("box_anna").assertExists()
        onNodeWithTag("box_luca").assertExists()
        onNodeWithTag("game_delete").performScrollTo().performClick()
        awaitText("Conferma eliminazione")
        onNodeWithTag("game_delete").performScrollTo().performClick()

        awaitText("Ancora nessuna partita.")
        assertEquals(emptyList(), games.games)
    }
}
