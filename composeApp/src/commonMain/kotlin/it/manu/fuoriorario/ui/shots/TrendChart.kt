package it.manu.fuoriorario.ui.shots

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.stat_field
import fuoriorario.composeapp.generated.resources.stat_free
import fuoriorario.composeapp.generated.resources.trend_description
import fuoriorario.composeapp.generated.resources.trend_title
import fuoriorario.composeapp.generated.resources.trend_too_few
import it.manu.fuoriorario.domain.TrendPoint
import it.manu.fuoriorario.ui.components.Legend
import it.manu.fuoriorario.ui.components.short
import it.manu.fuoriorario.ui.theme.FuoriOrarioTheme
import org.jetbrains.compose.resources.stringResource

// Prototype `trendSvg` viewBox and plot margins.
private const val CHART_WIDTH = 340f
private const val CHART_HEIGHT = 170f
private const val PLOT_LEFT = 32f
private const val PLOT_RIGHT = 10f
private const val PLOT_TOP = 10f
private const val PLOT_BOTTOM = 26f

/**
 * Prototype "Andamento" panel content: dal campo as a line with its area, liberi dashed, axis 0–100 and the
 * first and last date. A series skips the sessions it has no shots in, the other one is unaffected.
 */
@Composable
fun TrendChart(points: List<TrendPoint>) {
    val c = FuoriOrarioTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(Res.string.trend_title), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Legend(c.accent to Res.string.stat_field, c.cold to Res.string.stat_free)
    }
    if (points.size < 2) {
        Text(stringResource(Res.string.trend_too_few), color = c.muted)
        return
    }
    val measurer = rememberTextMeasurer()
    val body = MaterialTheme.typography.bodyMedium
    val description = stringResource(Res.string.trend_description)
    val first = points.first().date.short()
    val last = points.last().date.short()
    Canvas(
        Modifier.fillMaxWidth().aspectRatio(CHART_WIDTH / CHART_HEIGHT).semantics {
            contentDescription = description
        }
    ) {
        // Everything in viewBox units times s, so text and strokes scale with the width like the SVG.
        val s = size.width / CHART_WIDTH
        val plotWidth = CHART_WIDTH - PLOT_LEFT - PLOT_RIGHT
        val plotHeight = CHART_HEIGHT - PLOT_TOP - PLOT_BOTTOM
        fun x(i: Int) = (PLOT_LEFT + i * plotWidth / (points.size - 1)) * s
        fun y(v: Int) = (PLOT_TOP + plotHeight - v / 100f * plotHeight) * s
        val axis = body.copy(color = c.muted, fontSize = (11f * s).toSp(), lineHeight = (11f * s).toSp())

        /** [end] aligns the text's end to [x], like `text-anchor="end"`. */
        fun label(text: String, x: Float, baseline: Float, end: Boolean = false) {
            val m = measurer.measure(text, axis)
            drawText(m, topLeft = Offset(if (end) x - m.size.width else x, baseline - m.firstBaseline))
        }

        listOf(0, 25, 50, 75, 100).forEach { v ->
            drawLine(c.line, Offset(PLOT_LEFT * s, y(v)), Offset((CHART_WIDTH - PLOT_RIGHT) * s, y(v)), s)
            label("$v", (PLOT_LEFT - 6) * s, y(v) + 4 * s, end = true)
        }
        fun series(value: (TrendPoint) -> Int?) =
            points.indices.mapNotNull { i -> value(points[i])?.let { Offset(x(i), y(it)) } }
        fun line(offsets: List<Offset>) = Path().apply {
            offsets.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
        }
        val field = series { it.fieldGoal }
        val free = series { it.freeThrows }
        if (field.size > 1) {
            val area = line(listOf(Offset(field.first().x, y(0))) + field + Offset(field.last().x, y(0)))
            drawPath(area.apply { close() }, c.accent.copy(alpha = 0.12f))
        }
        drawPath(line(field), c.accent, style = Stroke(2.5f * s))
        drawPath(
            line(free),
            c.cold,
            style = Stroke(2f * s, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5 * s, 4 * s)))
        )
        points.last().fieldGoal?.let { drawCircle(c.accent, 4.5f * s, Offset(x(points.lastIndex), y(it))) }
        points.last().freeThrows?.let { drawCircle(c.cold, 4f * s, Offset(x(points.lastIndex), y(it))) }
        label(first, PLOT_LEFT * s, (CHART_HEIGHT - 6) * s)
        label(last, (CHART_WIDTH - PLOT_RIGHT) * s, (CHART_HEIGHT - 6) * s, end = true)
    }
}
