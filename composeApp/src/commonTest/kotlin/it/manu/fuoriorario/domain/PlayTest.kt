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

/** #45: the editor's rules. */
class PlayEditTest {
    @Test
    fun aNewPlayStartsFromTheDefaultLayout() {
        val step = startingStep(defense = false)
        assertEquals(setOf("1", "2", "3", "4", "5"), step.pos.keys)
        assertEquals("1", step.ball)
        // 1 at the top, 2–3 on the wings, 4–5 on the posts.
        assertTrue(step.pos.getValue("1").y > step.pos.getValue("2").y)
        assertTrue(step.pos.getValue("2").x < BASKET_X && step.pos.getValue("3").x > BASKET_X)
        assertTrue(step.pos.getValue("4").y < KEY_BOTTOM && step.pos.getValue("5").y < KEY_BOTTOM)
    }

    @Test
    fun defendersStandBetweenTheirManAndTheBasket() {
        val step = startingStep(defense = true)
        (1..5).forEach { n ->
            val man = step.pos.getValue("$n")
            val x = step.pos.getValue("X$n")
            val basket = Point(BASKET_X, BASKET_Y)
            assertTrue(distance(x, basket) < distance(man, basket), "X$n nearer the basket")
            assertTrue(distance(man, x) + distance(x, basket) - distance(man, basket) < 0.1f, "X$n on the line")
        }
    }

    @Test
    fun defenseOffTakesTheDefendersAndTheirCurvesAway() {
        val step = startingStep(defense = true).copy(curves = mapOf("X1" to Point(1f, 1f), "2" to Point(2f, 2f)))
        val off = step.withDefense(false)
        assertEquals(startingStep(defense = false).pos, off.pos)
        assertEquals(mapOf("2" to Point(2f, 2f)), off.curves)
        // And back on, each one with its man again.
        assertEquals(startingStep(defense = true).pos, off.withDefense(true).pos)
        // Already on: defenders where staff put them.
        val moved = step.copy(pos = step.pos + ("X1" to Point(10f, 10f)))
        assertEquals(moved, moved.withDefense(true))
    }

    @Test
    fun theFingerTakesTheNearest() {
        val pieces = mapOf("1" to Point(250f, 330f), "2" to Point(60f, 230f))
        assertEquals("2", nearest(pieces, Point(150f, 250f)))
        assertEquals("1", nearest(pieces, Point(240f, 500f)))
    }

    @Test
    fun aHoldFarFromEveryAttackerTakesNone() {
        val pieces = mapOf("1" to Point(250f, 330f))
        assertEquals("1", nearest(pieces, Point(270f, 340f), reach = 40f))
        assertEquals(null, nearest(pieces, Point(250f, 200f), reach = 40f))
    }

    @Test
    fun piecesStayWithinTheMargin() {
        assertEquals(Point(-COURT_MARGIN, 0f), Point(-80f, 0f).within(CourtSize.HALF))
        assertEquals(Point(550f, 520f), Point(600f, 700f).within(CourtSize.HALF))
        assertEquals(Point(550f, 700f), Point(600f, 700f).within(CourtSize.FULL))
        assertEquals(Point(0f, 990f), Point(0f, 1200f).within(CourtSize.FULL))
    }

    @Test
    fun theHandleBendsTheMoveThroughIt() {
        val from = Point(60f, 230f)
        val to = Point(60f, 30f)
        val bent = Step(mapOf("2" to to), "2").bent("2", from, Point(160f, 130f))
        val move = Move(MoveKind.CUT, "2", from, to, bent.curves["2"])
        assertEquals(Point(160f, 130f), move.at(0.5f))
        // Back near the middle of the line: straight again.
        assertEquals(emptyMap(), bent.bent("2", from, Point(64f, 128f)).curves)
    }

    @Test
    fun aPlayNeedsATitle() {
        val play =
            Play(" ", PlayCategory.ATTACK, court = CourtSize.HALF, defense = false, steps = listOf(startingStep(false)))
        assertEquals(PlayError.TITLE, playError(play))
        assertEquals(null, playError(play.copy(title = "Box")))
    }

    @Test
    fun savedTrimmedWithoutBlanks() {
        val step = startingStep(false)
        val play = Play(" Box ", PlayCategory.ATTACK, " ", CourtSize.HALF, false, listOf(step.copy(note = " ")))
        assertEquals(
            Play("Box", PlayCategory.ATTACK, null, CourtSize.HALF, false, listOf(step.copy(note = null))),
            play.cleaned()
        )
    }

    @Test
    fun savedWithoutCurvesThatNoMoveUses() {
        val first = startingStep(false)
        val bend = mapOf("2" to Point(160f, 130f))
        val moved = Step(first.pos + ("2" to Point(60f, 30f)), "1", curves = bend + ("1" to Point(1f, 1f)))
        val play =
            Play("Box", PlayCategory.ATTACK, null, CourtSize.HALF, false, listOf(first.copy(curves = bend), moved))
        // Nothing moves into the first step; 1 stands still in the second.
        assertEquals(listOf(first, moved.copy(curves = bend)), play.cleaned().steps)
    }

    private val first = startingStep(false)

    /** 2 cuts from the wing to the corner, bent through (160, 130). */
    private val cut = Step(first.pos + ("2" to Point(60f, 30f)), "1").bent("2", Point(60f, 230f), Point(160f, 130f))

    private fun halfway(from: Step?, to: Step, piece: String = "2") = Move(
        MoveKind.CUT,
        piece,
        from!!.pos.getValue(piece),
        to.pos.getValue(piece),
        to.curves.getValue(piece)
    ).at(0.5f)

    @Test
    fun movingWhereABentMoveEndsKeepsItsHandle() {
        val steps = listOf(first, cut).with(1, cut.copy(pos = cut.pos + ("2" to Point(30f, 10f))))
        assertNear(Point(160f, 130f), halfway(steps[0], steps[1]))
    }

    @Test
    fun movingWhereABentMoveStartsKeepsItsHandle() {
        val steps = listOf(first, cut).with(0, first.copy(pos = first.pos + ("2" to Point(40f, 300f))))
        assertNear(Point(160f, 130f), halfway(steps[0], steps[1]))
    }

    @Test
    fun aRemovedStepLeavesTheNextOneBentThroughTheSameHandle() {
        val middle = first.copy(pos = first.pos + ("2" to Point(100f, 300f)))
        val next = cut.copy(curves = emptyMap()).bent("2", Point(100f, 300f), Point(160f, 130f))
        val steps = listOf(first, middle, next).without(1)
        assertEquals(2, steps.size)
        assertNear(Point(160f, 130f), halfway(steps[0], steps[1]))
        // Without a step before, nothing moves in: no bends.
        assertEquals(emptyMap(), listOf(first, cut).without(0).single().curves)
    }

    private fun assertNear(expected: Point, actual: Point) = assertTrue(
        abs(actual.x - expected.x) < 0.01f && abs(actual.y - expected.y) < 0.01f,
        "$actual"
    )

    private fun distance(a: Point, b: Point) = kotlin.math.hypot(a.x - b.x, a.y - b.y)
}
