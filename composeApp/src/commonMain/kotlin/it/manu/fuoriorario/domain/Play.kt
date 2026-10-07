package it.manu.fuoriorario.domain

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
@Serializable
data class Play(
    val title: String,
    val category: PlayCategory,
    val description: String? = null,
    val court: CourtSize,
    /** Defenders X1–X5 are on the court. */
    val defense: Boolean,
    val steps: List<Step>,
    val id: String? = null
)

/** The list: categories in their order, then titles alphabetically. */
fun byCategory(plays: List<Play>): List<Pair<PlayCategory, List<Play>>> = plays
    .groupBy { it.category }
    .toList()
    .sortedBy { it.first.ordinal }
    .map { (category, list) -> category to list.sortedBy { it.title.lowercase() } }

enum class MoveKind { CUT, DRIBBLE, PASS, SCREEN, DEFENDER }

/** A line on the court. [piece] is who moves, or who passes; [control] bends it. */
data class Move(val kind: MoveKind, val piece: String, val from: Point, val to: Point, val control: Point? = null) {
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
 * others cutting), then the ball goes to its new holder. None on the first step.
 */
fun moves(prev: Step?, step: Step): List<Move> {
    if (prev == null) return emptyList()
    val players = step.pos.mapNotNull { (piece, to) ->
        val from = prev.pos[piece] ?: return@mapNotNull null
        if (from == to) return@mapNotNull null
        val kind = when {
            isDefender(piece) -> MoveKind.DEFENDER
            piece in step.screens -> MoveKind.SCREEN
            piece == prev.ball -> MoveKind.DRIBBLE
            else -> MoveKind.CUT
        }
        Move(kind, piece, from, to, step.curves[piece])
    }
    val pass = if (prev.ball != step.ball) {
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
