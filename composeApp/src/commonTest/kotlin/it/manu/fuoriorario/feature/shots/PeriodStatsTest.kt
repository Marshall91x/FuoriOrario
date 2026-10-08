package it.manu.fuoriorario.feature.shots

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import it.manu.fuoriorario.core.today
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.ShotSession
import it.manu.fuoriorario.domain.Shots
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.feature.auth.FakeAuthRepository
import it.manu.fuoriorario.testing.TestApp
import it.manu.fuoriorario.testing.awaitText
import it.manu.fuoriorario.testing.runAppTest
import it.manu.fuoriorario.ui.roster.FakeRosterRepository
import kotlin.test.Test
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus

/** Changing the period updates the stats and the session list. */
@OptIn(ExperimentalTestApi::class)
class PeriodStatsTest {
    private val luca = Member("Luca B.", Role.PLAYER, "2026-10-01T10:00:00Z")

    private fun daysAgo(days: Int) = today().minus(DatePeriod(days = days))

    @Test
    fun periodFiltersStatsAndSessions() = runAppTest {
        val shots = FakeShotRepository(
            ShotSession(daysAgo(0), mapOf(Zone.PIT to Shots(3, 5)), "Oggi", id = "a"),
            ShotSession(daysAgo(20), mapOf(Zone.CEN to Shots(2, 4), Zone.TL to Shots(8, 10)), "Venti", id = "b"),
            // Always before this season's 1 September.
            ShotSession(daysAgo(400), mapOf(Zone.PIT to Shots(1, 1)), "Vecchia", id = "c")
        )
        setContent { TestApp(FakeAuthRepository(luca), FakeRosterRepository(), shots) }

        // 30 giorni by default, like the prototype.
        awaitText("Venti")
        onNodeWithTag("stat_attempted").assertTextEquals("19")
        onNodeWithTag("stat_field").assertTextEquals("56%")
        onNodeWithTag("stat_three").assertTextEquals("50%")
        onNodeWithTag("stat_free").assertTextEquals("80%")
        onNodeWithText("Vecchia").assertDoesNotExist()

        awaitText("7 giorni")
        onNodeWithText("7 giorni").performClick()
        awaitText("Oggi")
        onNodeWithText("Venti").assertDoesNotExist()
        onNodeWithTag("stat_attempted").assertTextEquals("5")
        onNodeWithTag("stat_field").assertTextEquals("60%")
        onNodeWithTag("stat_three").assertTextEquals("—")
        onNodeWithTag("stat_free").assertTextEquals("—")

        onNodeWithText("Stagione").performClick()
        awaitText("Oggi")
        onNodeWithText("Vecchia").assertDoesNotExist()
    }
}
