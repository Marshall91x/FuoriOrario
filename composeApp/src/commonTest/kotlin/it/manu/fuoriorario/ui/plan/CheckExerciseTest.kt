package it.manu.fuoriorario.ui.plan

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.russhwolf.settings.MapSettings
import it.manu.fuoriorario.core.today
import it.manu.fuoriorario.domain.Category
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanCheck
import it.manu.fuoriorario.domain.PlanItem
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.weekOf
import it.manu.fuoriorario.feature.auth.FakeAuthRepository
import it.manu.fuoriorario.feature.shots.FakeShotRepository
import it.manu.fuoriorario.testing.TestApp
import it.manu.fuoriorario.testing.awaitNode
import it.manu.fuoriorario.testing.awaitText
import it.manu.fuoriorario.testing.runAppTest
import it.manu.fuoriorario.ui.roster.FakeRosterRepository
import kotlin.test.Test
import kotlin.test.assertEquals

private const val ACK = "2026-10-01T10:00:00Z"

/** UI test 3 (#14): the player checks and unchecks the assigned days; staff see the checks but can't change them. */
@OptIn(ExperimentalTestApi::class)
class CheckExerciseTest {
    private val coach = Member("Coach", Role.STAFF, ACK, id = "coach")
    private val luca = Member("Luca B.", Role.PLAYER, ACK, jerseyNumber = "7", id = "luca")
    private val week = weekOf(today())

    private fun plans() = FakePlanRepository(
        PlanItem(week, "Mikan drill", Category.FOOTWORK, days = listOf(0, 2), id = "p1", memberId = "luca"),
        PlanItem(week, "Liberi", Category.SHOOTING, days = listOf(5), id = "p2", memberId = "luca"),
        checks = setOf(PlanCheck("p2", 5))
    )

    @Test
    fun playerChecksAssignedDays() = runAppTest {
        val plans = plans()
        setContent {
            TestApp(FakeAuthRepository(luca), FakeRosterRepository(), FakeShotRepository(), MapSettings(), plans)
        }
        awaitText("Piano")
        onNodeWithText("Piano").performClick()
        awaitText("Mikan drill")
        awaitText("1/3")
        // Day names load asynchronously on web.
        awaitNode(hasContentDescription("Sab: fatto"))

        onNodeWithContentDescription("Lun: da fare").performScrollTo().performClick()
        awaitText("2/3")
        onNodeWithContentDescription("Lun: fatto").assertExists()
        assertEquals(setOf(PlanCheck("p1", 0), PlanCheck("p2", 5)), plans.checks)

        onNodeWithContentDescription("Lun: fatto").performScrollTo().performClick()
        awaitText("1/3")
        assertEquals(setOf(PlanCheck("p2", 5)), plans.checks)

        // A failed check leaves things as they were.
        plans.failNext = true
        onNodeWithContentDescription("Mer: da fare").performScrollTo().performClick()
        awaitText("Salvataggio non riuscito. Riprova tra poco.")
        onNodeWithText("1/3").assertExists()
        onNodeWithContentDescription("Mer: da fare").assertExists()
        assertEquals(setOf(PlanCheck("p2", 5)), plans.checks)
        plans.failNext = true
        onNodeWithContentDescription("Sab: fatto").performScrollTo().performClick()
        awaitText("Salvataggio non riuscito. Riprova tra poco.")
        onNodeWithContentDescription("Sab: fatto").assertExists()
        assertEquals(setOf(PlanCheck("p2", 5)), plans.checks)

        // Rest days can't be checked.
        onAllNodesWithContentDescription("Mar: riposo").onFirst().assertHasNoClickAction()
    }

    @Test
    fun staffSeeChecksButCannotChange() = runAppTest {
        val plans = plans()
        setContent {
            TestApp(
                FakeAuthRepository(coach),
                FakeRosterRepository(coach, luca),
                FakeShotRepository(),
                MapSettings(),
                plans
            )
        }
        awaitText("Piano")
        onNodeWithText("Piano").performClick()
        awaitText("Mikan drill")
        awaitText("1/3")
        awaitNode(hasContentDescription("Sab: fatto"))
        onNodeWithContentDescription("Sab: fatto").assertHasNoClickAction()
        onNodeWithContentDescription("Lun: da fare").assertHasNoClickAction()
    }
}
