package it.manu.fuoriorario.ui.team

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.russhwolf.settings.MapSettings
import it.manu.fuoriorario.App
import it.manu.fuoriorario.core.today
import it.manu.fuoriorario.domain.Category
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanCheck
import it.manu.fuoriorario.domain.PlanItem
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.ShotSession
import it.manu.fuoriorario.domain.Shots
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.domain.weekOf
import it.manu.fuoriorario.ui.auth.FakeAuthRepository
import it.manu.fuoriorario.ui.auth.awaitText
import it.manu.fuoriorario.ui.auth.runAppTest
import it.manu.fuoriorario.ui.plan.FakePlanRepository
import it.manu.fuoriorario.ui.roster.FakeRosterRepository
import it.manu.fuoriorario.ui.shots.FakeShotRepository
import kotlin.test.Test

private const val ACK = "2026-10-01T10:00:00Z"

/** #20: staff see every player's week at a glance and open a Diario from it. */
@OptIn(ExperimentalTestApi::class)
class TeamOverviewTest {
    private val coach = Member("Coach", Role.STAFF, ACK, id = "coach")
    private val luca = Member("Luca B.", Role.PLAYER, ACK, jerseyNumber = "7", position = "Guardia", id = "luca")
    private val anna = Member("Anna", Role.PLAYER, ACK, id = "anna")

    @Test
    fun staffSeesTheTeamAndOpensADiario() = runAppTest {
        val shots = FakeShotRepository(
            ShotSession(today(), mapOf(Zone.CEN to Shots(3, 10), Zone.TL to Shots(7, 10)), id = "l1", memberId = "luca")
        )
        val plans = FakePlanRepository(
            PlanItem(weekOf(today()), "Mikan", Category.FOOTWORK, days = listOf(0, 1), id = "p1", memberId = "luca"),
            checks = setOf(PlanCheck("p1", 0))
        )
        setContent {
            App(FakeAuthRepository(coach), FakeRosterRepository(coach, anna, luca), shots, MapSettings(), plans)
        }

        awaitText("Squadra")
        onNodeWithText("Squadra").performClick()
        awaitText("QUADRO DELLA SQUADRA")
        awaitText("50%")
        onNodeWithTag("overview_luca").assertTextEquals("#7", "Luca B.", "Guardia", "50%", "20", "70%", "30%")
        // No plan, no shots.
        onNodeWithTag("overview_anna").assertTextEquals("Anna", "—", "0", "—", "—")

        onNodeWithTag("overview_anna").performScrollTo().performClick()
        awaitText("ANNA")
        onNodeWithTag("player_picker").assertTextEquals("Anna")
        awaitText("Nessuna sessione nel periodo. Registra la prima dopo il prossimo allenamento.")
    }
}
