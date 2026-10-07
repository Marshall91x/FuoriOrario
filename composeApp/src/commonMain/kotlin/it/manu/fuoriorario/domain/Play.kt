package it.manu.fuoriorario.domain

import kotlin.math.hypot
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Categoria di uno schema, in the order the list shows them; serial names are the values of `plays.category`. */
@Serializable
enum class PlayCategory {
    @SerialName("Attacco")
    ATTACK,

    @SerialName("Contro zona")
    ZONE_OFFENSE,

    @SerialName("Rimessa laterale")
    SIDELINE_INBOUND,

    @SerialName("Rimessa dal fondo")
    BASELINE_INBOUND,

    @SerialName("Fine partita")
    END_OF_GAME,

    @SerialName("Transizione")
    TRANSITION,

    @SerialName("Difesa")
    DEFENSE
}

/** Half court: the attacking basket at the top, like the shot map. Full court: the other basket at the bottom. */
enum class CourtSize { HALF, FULL }

/** A spot in the shot map's viewBox (500 units = 15 m), y from the attacking baseline; off the lines up to [COURT_MARGIN]. */
@Serializable
data class Point(val x: Float, val y: Float)

/** ~1.5 m around the lines, for inbounds. */
const val COURT_MARGIN = 50f

/** Attackers are "1"–"5", defenders "X1"–"X5". */
fun isDefender(piece: String) = piece.startsWith("X")

/**
 * Passo (ADR 0008): where every piece stands at its end. [ball] is who has the ball, [screens] the attackers setting a
 * screen, [curves] the control point of a piece's curved move from the step before.
 */
@Serializable
data class Step(
    val pos: Map<String, Point>,
    val ball: String,
    val screens: List<String> = emptyList(),
    val curves: Map<String, Point> = emptyMap(),
    val note: String? = null
)

/** Schema: a `plays` row; team is filled by the database from the staff. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Play(
    val title: String,
    val category: PlayCategory,
    val description: String? = null,
    val court: CourtSize,
    /** Defenders X1–X5 are on the court. */
    val defense: Boolean,
    val steps: List<Step>,
    /** Null until saved; never sent, the database generates it. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val id: String? = null
)

/** The list: categories in their order, then titles alphabetically. */
fun byCategory(plays: List<Play>): List<Pair<PlayCategory, List<Play>>> = plays
    .groupBy { it.category }
    .toList()
    .sortedBy { it.first.ordinal }
    .map { (category, list) -> category to list.sortedBy { it.title.lowercase() } }

enum class MoveKind { CUT, DRIBBLE, PASS, SCREEN, DEFENDER }

/**
 * A line on the court. [piece] is who moves, or who passes; [control] bends it. A screen set standing still has [from]
 * equal to [to] and [facing], the ball, to turn its ⊥ to.
 */
data class Move(
    val kind: MoveKind,
    val piece: String,
    val from: Point,
    val to: Point,
    val control: Point? = null,
    val facing: Point? = null
) {
    /** Where the line is at [t] in 0..1: a quadratic curve through [control], else straight. */
    fun at(t: Float): Point {
        val u = 1 - t
        return if (control == null) {
            Point(u * from.x + t * to.x, u * from.y + t * to.y)
        } else {
            Point(
                u * u * from.x + 2 * u * t * control.x + t * t * to.x,
                u * u * from.y + 2 * u * t * control.y + t * t * to.y
            )
        }
    }
}

/**
 * The lines from [prev] to [step]: whoever changed spot moves (defenders, screeners, the ball handler dribbling, the
 * others cutting), then the ball goes to its new holder. A screener standing still gets just the ⊥, on the first step
 * too; nothing else moves there.
 */
fun moves(prev: Step?, step: Step): List<Move> {
    val players = step.pos.mapNotNull { (piece, to) ->
        val from = prev?.pos?.get(piece) ?: to
        val kind = when {
            from == to -> null
            isDefender(piece) -> MoveKind.DEFENDER
            piece in step.screens -> MoveKind.SCREEN
            piece == prev?.ball -> MoveKind.DRIBBLE
            else -> MoveKind.CUT
        }
        when {
            kind != null -> Move(kind, piece, from, to, step.curves[piece])
            piece in step.screens -> Move(MoveKind.SCREEN, piece, to, to, facing = step.pos[step.ball])
            else -> null
        }
    }
    val pass = if (prev != null && prev.ball != step.ball) {
        listOfNotNull(
            step.pos[prev.ball]?.let { from ->
                step.pos[step.ball]?.let { to -> Move(MoveKind.PASS, prev.ball, from, to) }
            }
        )
    } else {
        emptyList()
    }
    return players + pass
}

/** Players move in the first 60% of a step, a pass flies in the rest. */
const val STEP_MILLIS = 1000
private const val PLAYERS_END = 0.6f

/** Where the pieces and the ball are at one moment. */
data class Frame(val pos: Map<String, Point>, val ball: Point)

/** The court at [t] (0..1) of the way from [prev] to [step]; without [prev], [step] as it is. */
fun frame(prev: Step?, step: Step, t: Float): Frame {
    val lines = moves(prev, step)
    val walk = (t / PLAYERS_END).coerceIn(0f, 1f)
    val pos = step.pos.mapValues { (piece, to) ->
        lines.find { it.piece == piece && it.kind != MoveKind.PASS }?.at(walk)
            ?: to
    }
    val pass = lines.find { it.kind == MoveKind.PASS }
    val ball = when {
        pass == null -> pos.getValue(step.ball)
        t < PLAYERS_END -> pos.getValue(pass.piece)
        else -> pass.at(((t - PLAYERS_END) / (1 - PLAYERS_END)).coerceIn(0f, 1f))
    }
    return Frame(pos, ball)
}

/** Limits of the `plays` row, kept by the editor's fields. */
const val PLAY_TITLE_MAX = 60
const val PLAY_DESCRIPTION_MAX = 400
const val STEP_NOTE_MAX = 200
const val STEPS_MAX = 20

private val ATTACKERS = listOf("1", "2", "3", "4", "5")

/** How far a defender stands from his man, towards the basket. */
private const val GUARD_DISTANCE = 45f

/** A new play's first step: 1 at the top, 2–3 on the wings, 4–5 on the posts, the ball to 1; with [defense], the Xs too. */
fun startingStep(defense: Boolean) = Step(
    mapOf(
        "1" to Point(250f, 330f),
        "2" to Point(60f, 230f),
        "3" to Point(440f, 230f),
        "4" to Point(150f, 110f),
        "5" to Point(350f, 110f)
    ),
    ball = "1"
).withDefense(defense)

/** With [on], each missing Xn between n and the attacking basket; without, no defenders nor their curves. */
fun Step.withDefense(on: Boolean): Step {
    if (!on) return copy(pos = pos.filterKeys { !isDefender(it) }, curves = curves.filterKeys { !isDefender(it) })
    val basket = Point(BASKET_X, BASKET_Y)
    val guards = ATTACKERS.filter { "X$it" !in pos }.mapNotNull { n ->
        pos[n]?.let { man ->
            val dx = basket.x - man.x
            val dy = basket.y - man.y
            val k = (GUARD_DISTANCE / hypot(dx, dy)).coerceAtMost(1f)
            "X$n" to Point(man.x + dx * k, man.y + dy * k)
        }
    }
    return copy(pos = pos + guards)
}

/** The key of [candidates] nearest [at]: the finger takes the nearest piece. */
fun nearest(candidates: Map<String, Point>, at: Point): String =
    candidates.minBy { (_, p) -> hypot(p.x - at.x, p.y - at.y) }.key

/** This point kept on the court of [court] or in the margin around it. */
fun Point.within(court: CourtSize): Point {
    val length = if (court == CourtSize.HALF) COURT_HEIGHT else 2 * COURT_HEIGHT
    return Point(
        x.coerceIn(-COURT_MARGIN, COURT_WIDTH + COURT_MARGIN),
        y.coerceIn(-COURT_MARGIN, length + COURT_MARGIN)
    )
}

/** Close enough to the line's middle to be straight. */
private const val STRAIGHT = 10f

/**
 * [piece]'s move from [from] bent to pass through [through] halfway, where the editor draws its handle; near the
 * middle of the straight line, straight again.
 */
fun Step.bent(piece: String, from: Point, through: Point): Step {
    val to = pos.getValue(piece)
    val midX = (from.x + to.x) / 2
    val midY = (from.y + to.y) / 2
    if (hypot(through.x - midX, through.y - midY) < STRAIGHT) return copy(curves = curves - piece)
    // A quadratic curve is at (from + 2·control + to) / 4 halfway.
    return copy(curves = curves + (piece to Point(2 * through.x - midX, 2 * through.y - midY)))
}

enum class PlayError { TITLE }

/** First problem with the play as drawn, or null; lengths are capped by the fields. */
fun playError(play: Play): PlayError? = if (play.title.isBlank()) PlayError.TITLE else null

/** The play to save: trimmed, blanks null. */
fun Play.cleaned() = copy(
    title = title.trim(),
    description = description?.trim()?.ifEmpty { null },
    steps = steps.map { it.copy(note = it.note?.trim()?.ifEmpty { null }) }
)
