package it.manu.fuoriorario.domain

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayTest {
    private val start = Step(
        mapOf("1" to Point(250f, 330f), "2" to Point(60f, 230f), "5" to Point(330f, 150f), "X1" to Point(250f, 290f)),
        ball = "1"
    )

    private fun play(title: String, category: PlayCategory) =
        Play(title, category, court = CourtSize.HALF, defense = false, steps = listOf(start))

    @Test
    fun listGroupsByCategoryInOrderThenTitle() {
        val plays = listOf(
            play("zeta", PlayCategory.ATTACK),
            play("Rimessa 1", PlayCategory.SIDELINE_INBOUND),
            play("Alfa", PlayCategory.ATTACK),
            play("beta", PlayCategory.ATTACK)
        )
        val groups = byCategory(plays)
        assertEquals(listOf(PlayCategory.ATTACK, PlayCategory.SIDELINE_INBOUND), groups.map { it.first })
        assertEquals(listOf("Alfa", "beta", "zeta"), groups[0].second.map { it.title })
    }

    @Test
    fun firstStepHasNoMoves() {
        assertEquals(emptyList(), moves(null, start))
        assertEquals(emptyList(), moves(start, start))
    }

    @Test
    fun movesComeFromTheStepBefore() {
        val next = Step(
            start.pos + mapOf(
                "1" to Point(360f, 220f),
                "2" to Point(60f, 100f),
                "5" to Point(285f, 300f),
                "X1" to Point(300f, 260f)
            ),
            ball = "2",
            screens = listOf("5"),
            curves = mapOf("1" to Point(330f, 300f))
        )
        assertEquals(
            listOf(
                Move(MoveKind.DRIBBLE, "1", Point(250f, 330f), Point(360f, 220f), Point(330f, 300f)),
                Move(MoveKind.CUT, "2", Point(60f, 230f), Point(60f, 100f)),
                Move(MoveKind.SCREEN, "5", Point(330f, 150f), Point(285f, 300f)),
                Move(MoveKind.DEFENDER, "X1", Point(250f, 290f), Point(300f, 260f)),
                // From where the passer ends to where the receiver ends: players move first.
                Move(MoveKind.PASS, "1", Point(360f, 220f), Point(60f, 100f))
            ),
            moves(start, next)
        )
    }

    @Test
    fun aScreenSetStandingStillFacesTheBall() {
        val next = start.copy(screens = listOf("5"))
        assertEquals(
            listOf(Move(MoveKind.SCREEN, "5", Point(330f, 150f), Point(330f, 150f), facing = Point(250f, 330f))),
            moves(start, next)
        )
        // Also on the first step.
        assertEquals(
            listOf(Move(MoveKind.SCREEN, "5", Point(330f, 150f), Point(330f, 150f), facing = Point(250f, 330f))),
            moves(null, next)
        )
    }

    @Test
    fun playersMoveInTheFirst600msAlongTheirCurve() {
        val next = Step(
            start.pos + ("2" to Point(60f, 30f)),
            ball = "1",
            curves = mapOf("2" to Point(160f, 130f))
        )
        assertEquals(Point(60f, 230f), frame(start, next, 0f).pos["2"])
        // Halfway along the quadratic curve: (from + 2·control + to) / 4.
        assertNear(Point(110f, 130f), frame(start, next, 0.3f).pos["2"])
        assertEquals(Point(60f, 30f), frame(start, next, 0.6f).pos["2"])
        assertEquals(Point(60f, 30f), frame(start, next, 1f).pos["2"])
        // Who stays still, stays.
        assertEquals(Point(330f, 150f), frame(start, next, 0.3f).pos["5"])
    }

    @Test
    fun ballFollowsItsHolderThenFliesInTheLast400ms() {
        val next = Step(start.pos + ("1" to Point(250f, 230f)), ball = "5")
        assertNear(Point(250f, 280f), frame(start, next, 0.3f).ball)
        assertEquals(Point(250f, 230f), frame(start, next, 0.6f).ball)
        assertNear(Point(290f, 190f), frame(start, next, 0.8f).ball)
        assertEquals(Point(330f, 150f), frame(start, next, 1f).ball)
    }

    @Test
    fun withoutAStepBeforeTheFrameIsTheStep() {
        assertEquals(Frame(start.pos, Point(250f, 330f)), frame(null, start, 0f))
    }

    private fun assertNear(expected: Point, actual: Point?) = assertTrue(
        actual != null && abs(actual.x - expected.x) < 0.01f && abs(actual.y - expected.y) < 0.01f,
        "$actual"
    )
}
