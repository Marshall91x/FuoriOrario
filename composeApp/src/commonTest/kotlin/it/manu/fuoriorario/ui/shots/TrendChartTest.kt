package it.manu.fuoriorario.ui.shots

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
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

/** The Andamento needs two sessions in the period; then the chart shows with its first and last date. */
@OptIn(ExperimentalTestApi::class)
class TrendChartTest {
    private val luca = Member("Luca B.", Role.PLAYER, "2026-10-01T10:00:00Z")

    @Test
    fun chartNeedsTwoSessions() = runAppTest {
        val old = today().minus(DatePeriod(days = 20))
        val shots = FakeShotRepository(
            ShotSession(today(), mapOf(Zone.PIT to Shots(3, 5)), id = "a"),
            ShotSession(old, mapOf(Zone.TL to Shots(8, 10)), id = "b")
        )
        setContent { TestApp(FakeAuthRepository(luca), FakeRosterRepository(), shots) }

        // 30 giorni: both sessions.
        awaitText("Andamento")
        onNodeWithContentDescription("Percentuali per sessione").assertExists()

        onNodeWithText("7 giorni").performClick()
        awaitText("Servono almeno due sessioni per vedere l'andamento.")
        onNodeWithContentDescription("Percentuali per sessione").assertDoesNotExist()
    }
}
