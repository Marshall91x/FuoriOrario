package it.manu.fuoriorario.ui.plays

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.play_court_description
import it.manu.fuoriorario.domain.COURT_HEIGHT
import it.manu.fuoriorario.domain.COURT_MARGIN
import it.manu.fuoriorario.domain.COURT_WIDTH
import it.manu.fuoriorario.domain.CourtSize
import it.manu.fuoriorario.domain.Frame
import it.manu.fuoriorario.domain.Move
import it.manu.fuoriorario.domain.MoveKind
import it.manu.fuoriorario.domain.Point
import it.manu.fuoriorario.domain.isDefender
import it.manu.fuoriorario.ui.shots.arcPath
import it.manu.fuoriorario.ui.shots.drawLines
import it.manu.fuoriorario.ui.theme.FuoriOrarioTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import org.jetbrains.compose.resources.stringResource

private const val PIECE_RADIUS = 17f
private const val BALL_RADIUS = 7f
private const val LINE_WIDTH = 2.5f

/**
 * The court of a play in the shot map's viewBox plus [COURT_MARGIN] all round: [moves] as lines, the pieces where [frame]
 * puts them, the ball beside its holder.
 */
@Composable
fun PlayCourt(court: CourtSize, moves: List<Move>, frame: Frame) {
    val c = FuoriOrarioTheme.colors
    val arc = remember { arcPath(closed = false) }
    val measurer = rememberTextMeasurer()
    val description = stringResource(Res.string.play_court_description)
    val length = if (court == CourtSize.HALF) COURT_HEIGHT else 2 * COURT_HEIGHT
    val width = COURT_WIDTH + 2 * COURT_MARGIN
    val height = length + 2 * COURT_MARGIN
    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(width / height)
            .clip(RoundedCornerShape(10.dp))
            .semantics { contentDescription = description }
    ) {
        scale(size.width / width, pivot = Offset.Zero) {
            drawRect(c.court.copy(alpha = 0.55f), size = Size(width, height))
            translate(COURT_MARGIN, COURT_MARGIN) {
                drawRect(c.court, size = Size(COURT_WIDTH, length))
                drawLines(arc, c.courtLine)
                // The other half, the defending basket at the bottom.
                if (court == CourtSize.FULL) scale(1f, -1f, Offset(0f, COURT_HEIGHT)) { drawLines(arc, c.courtLine) }
                moves.forEach { drawMove(it, if (it.kind == MoveKind.DEFENDER) c.cold else c.ink) }
                frame.pos.forEach { (piece, at) ->
                    if (isDefender(piece)) {
                        drawCircle(c.surface, PIECE_RADIUS, at.offset)
                        drawCircle(c.cold, PIECE_RADIUS, at.offset, style = Stroke(LINE_WIDTH))
                        drawLabel(measurer, piece, at, c.cold)
                    } else {
                        drawCircle(c.ink, PIECE_RADIUS, at.offset)
                        drawLabel(measurer, piece, at, c.bg)
                    }
                }
                val ball = frame.ball.offset + Offset(PIECE_RADIUS * 0.8f, -PIECE_RADIUS * 0.8f)
                drawCircle(c.accent, BALL_RADIUS, ball)
                drawCircle(c.ink, BALL_RADIUS, ball, style = Stroke(1.5f))
            }
        }
    }
}

private val Point.offset get() = Offset(x, y)

/** [label] centred on [at], sized in viewBox units like the court, not with the font size setting. */
private fun DrawScope.drawLabel(measurer: TextMeasurer, label: String, at: Point, color: Color) {
    val layout = measurer.measure(
        label,
        TextStyle(color = color, fontSize = (if (label.length > 1) 14f else 18f).toSp(), fontWeight = FontWeight.Bold)
    )
    drawText(layout, topLeft = at.offset - Offset(layout.size.width / 2f, layout.size.height / 2f))
}

/**
 * Prototype-style move line: cut solid, dribble wavy, pass dashed, defender solid in its colour; arrowhead at the end,
 * a screen ends with ⊥. It stops short of the pieces at its ends.
 */
private fun DrawScope.drawMove(move: Move, color: Color) {
    val samples = 40
    val all = (0..samples).map { move.at(it / samples.toFloat()).offset }
    val start = all.first()
    val end = all.last()
    val gap = PIECE_RADIUS + 3f
    val points = all.filter {
        (it - end).getDistance() > gap && (move.kind != MoveKind.PASS || (it - start).getDistance() > gap)
    }
    if (points.size < 2) return

    val path = Path()
    if (move.kind == MoveKind.DRIBBLE) {
        // A sine wave across the line, flat over the last stretch so the arrowhead sits straight.
        var travelled = 0f
        val total = points.zipWithNext { a, b -> (b - a).getDistance() }.sum()
        points.forEachIndexed { i, p ->
            if (i > 0) travelled += (p - points[i - 1]).getDistance()
            val (a, b) = if (i == 0) points[0] to points[1] else points[i - 1] to p
            val dir = (b - a) / (b - a).getDistance()
            val wave = if (total - travelled < 18f) 0f else sin(travelled / 22f * 2 * PI.toFloat()) * 6f
            val q = p + Offset(-dir.y, dir.x) * wave
            if (i == 0) path.moveTo(q.x, q.y) else path.lineTo(q.x, q.y)
        }
    } else {
        path.moveTo(points[0].x, points[0].y)
        points.drop(1).forEach { path.lineTo(it.x, it.y) }
    }
    val effect = if (move.kind == MoveKind.PASS) PathEffect.dashPathEffect(floatArrayOf(10f, 7f)) else null
    drawPath(path, color, style = Stroke(LINE_WIDTH, cap = StrokeCap.Round, pathEffect = effect))

    val tip = points.last()
    val before = points[points.size - 2]
    val angle = atan2(tip.y - before.y, tip.x - before.x)
    fun at(a: Float, r: Float) = tip + Offset(cos(a) * r, sin(a) * r)
    if (move.kind == MoveKind.SCREEN) {
        val half = PI.toFloat() / 2
        drawLine(color, at(angle + half, 10f), at(angle - half, 10f), LINE_WIDTH, StrokeCap.Round)
    } else {
        val spread = PI.toFloat() * 0.85f
        drawLine(color, tip, at(angle + spread, 12f), LINE_WIDTH, StrokeCap.Round)
        drawLine(color, tip, at(angle - spread, 12f), LINE_WIDTH, StrokeCap.Round)
    }
}
