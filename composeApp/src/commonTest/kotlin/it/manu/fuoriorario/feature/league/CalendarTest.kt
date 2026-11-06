package it.manu.fuoriorario.feature.league

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import it.manu.fuoriorario.domain.Fixture
import it.manu.fuoriorario.domain.ImportFixture
import it.manu.fuoriorario.domain.ImportPreview
import it.manu.fuoriorario.domain.League
import it.manu.fuoriorario.domain.LeagueTeam
import it.manu.fuoriorario.domain.Leg
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.feature.auth.FakeAuthRepository
import it.manu.fuoriorario.feature.roster.FakeRosterRepository
import it.manu.fuoriorario.testing.TestApp
import it.manu.fuoriorario.testing.awaitNode
import it.manu.fuoriorario.testing.awaitText
import it.manu.fuoriorario.testing.runAppTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.LocalDateTime

private const val ACK = "2026-10-01T10:00:00Z"

/** #100, #101, #102: the league's fixtures for the whole team; staff import the FIP PDF and edit fixtures. */
@OptIn(ExperimentalTestApi::class)
class CalendarTest {
    private val coach = Member("Coach", Role.STAFF, ACK, id = "coach")
    private val luca = Member("Luca B.", Role.PLAYER, ACK, id = "luca", jerseyNumber = "7")

    @Test
    fun playerSeeOurFixturesThenTheWholeLeague() = runAppTest {
        val now = Clock.System.now()
        val league = League(
            "l",
            "U19",
            listOf(LeagueTeam("a", "Artemis", ours = true), LeagueTeam("b", "Vasto"), LeagueTeam("c", "Atessa")),
            listOf(
                Fixture("1", 1, Leg.FIRST, now + 1.days, "c", "a", "Palestra Scuola Media"),
                Fixture("2", 2, Leg.FIRST, now + 8.days, "b", "c")
            )
        )
        setContent {
            TestApp(FakeAuthRepository(luca), FakeRosterRepository(coach, luca), leagues = FakeLeagueRepository(league))
        }

        awaitText("Partite")
        onNodeWithText("Partite").performClick()
        awaitText("1ª ANDATA")
        awaitText("Palestra Scuola Media")
        awaitNode(hasText("Riposo"))
        awaitNode(hasText("Vasto"), count = 0)

        onNodeWithTag("calendar_all").performClick()
        awaitText("Vasto")
        awaitText("Riposa: Artemis")
    }

    @Test
    fun staffImportThePdfMarkOursAndConfirm() = runAppTest {
        val leagues = FakeLeagueRepository().apply {
            preview = ImportPreview(
                "U19 - PRIMA FASE",
                listOf("Artemis", "Atessa"),
                listOf(ImportFixture("000691", 1, Leg.FIRST, LocalDateTime(2026, 11, 9, 18, 30), "Atessa", "Artemis"))
            )
        }
        setContent {
            CompositionLocalProvider(LocalPdfPicker provides { onPicked -> { onPicked { byteArrayOf(1) } } }) {
                TestApp(FakeAuthRepository(coach), FakeRosterRepository(coach, luca), leagues = leagues)
            }
        }

        awaitText("Squadra")
        onNodeWithText("Squadra").performClick()
        onNodeWithTag("team_settings").performClick()
        onNodeWithTag("league_import").performScrollTo().performClick()
        awaitText("U19 - PRIMA FASE")
        onNodeWithTag("ours_Artemis").performScrollTo().performClick()
        onNodeWithTag("league_confirm").performScrollTo().performClick()
        awaitText("Calendario importato")
        assertEquals("Artemis", leagues.league?.ours?.name)

        onNodeWithText("Partite").performClick()
        awaitText("Atessa")
    }

    private fun leagueWithOneFixture() = League(
        "l",
        "U19",
        listOf(LeagueTeam("a", "Artemis", ours = true), LeagueTeam("b", "Vasto")),
        listOf(Fixture("1", 1, Leg.FIRST, Clock.System.now() + 1.days, "a", "b"))
    )

    @Test
    fun staffEnterAResultAndTheRowShowsIt() = runAppTest {
        val leagues = FakeLeagueRepository(leagueWithOneFixture())
        setContent { TestApp(FakeAuthRepository(coach), FakeRosterRepository(coach, luca), leagues = leagues) }

        awaitText("Partite")
        onNodeWithText("Partite").performClick()
        awaitNode(hasText("+ Gara"))
        onNodeWithTag("fixture_1").performClick()
        awaitText("GARA 1")
        onNodeWithTag("fixture_status_PLAYED").performClick()
        onNodeWithTag("fixture_home_score").performTextReplacement("62")
        onNodeWithTag("fixture_away_score").performTextReplacement("55")
        onNodeWithTag("fixture_save").performScrollTo().performClick()

        awaitText("62–55")
        assertEquals(62, leagues.league?.fixtures?.single()?.homeScore)
    }

    @Test
    fun afterTwoResultsTheStandingsShowTheExpectedOrder() = runAppTest {
        val soon = Clock.System.now() + 1.days
        val leagues = FakeLeagueRepository(
            League(
                "l",
                "U19",
                listOf(LeagueTeam("a", "Artemis", ours = true), LeagueTeam("b", "Vasto"), LeagueTeam("c", "Atessa")),
                listOf(Fixture("1", 1, Leg.FIRST, soon, "a", "b"), Fixture("2", 1, Leg.FIRST, soon, "b", "c"))
            )
        )
        setContent { TestApp(FakeAuthRepository(coach), FakeRosterRepository(coach, luca), leagues = leagues) }

        awaitText("Partite")
        onNodeWithText("Partite").performClick()
        onNodeWithTag("calendar_all").performClick()
        result("1", "62", "55")
        result("2", "50", "70")

        // Artemis and Atessa on 2 points, never met: Atessa's +20 beats Artemis' +7.
        onNodeWithTag("games_standings").performClick()
        awaitText("1  Atessa")
        awaitText("2  Artemis")
        awaitText("3  Vasto")
    }

    private suspend fun ComposeUiTest.result(number: String, home: String, away: String) {
        onNodeWithTag("fixture_$number").performScrollTo().performClick()
        awaitText("GARA $number")
        onNodeWithTag("fixture_status_PLAYED").performClick()
        onNodeWithTag("fixture_home_score").performTextReplacement(home)
        onNodeWithTag("fixture_away_score").performTextReplacement(away)
        onNodeWithTag("fixture_save").performScrollTo().performClick()
        awaitText("$home–$away")
    }

    @Test
    fun playerHasNoActionsOnFixtures() = runAppTest {
        setContent {
            TestApp(
                FakeAuthRepository(luca),
                FakeRosterRepository(coach, luca),
                leagues = FakeLeagueRepository(leagueWithOneFixture())
            )
        }

        awaitText("Partite")
        onNodeWithText("Partite").performClick()
        awaitText("Vasto")
        awaitNode(hasText("+ Gara"), count = 0)
        onNodeWithTag("fixture_1").performClick()
        waitForIdle()
        awaitNode(hasText("GARA 1"), count = 0)
    }
}
