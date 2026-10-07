package it.manu.fuoriorario.ui.shots

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.heat_cold
import fuoriorario.composeapp.generated.resources.heat_even
import fuoriorario.composeapp.generated.resources.heat_hot
import fuoriorario.composeapp.generated.resources.heat_none
import fuoriorario.composeapp.generated.resources.map_description
import fuoriorario.composeapp.generated.resources.map_detail_none
import fuoriorario.composeapp.generated.resources.map_detail_ref
import fuoriorario.composeapp.generated.resources.map_detail_shots
import fuoriorario.composeapp.generated.resources.map_free_line
import fuoriorario.composeapp.generated.resources.map_free_none
import fuoriorario.composeapp.generated.resources.map_hint
import fuoriorario.composeapp.generated.resources.map_no_shots
import fuoriorario.composeapp.generated.resources.map_title
import it.manu.fuoriorario.domain.ARC_CORNER_Y
import it.manu.fuoriorario.domain.ARC_RADIUS
import it.manu.fuoriorario.domain.ARC_SIDE
import it.manu.fuoriorario.domain.BASKET_X
import it.manu.fuoriorario.domain.BASKET_Y
import it.manu.fuoriorario.domain.CORNER_SLICE_Y
import it.manu.fuoriorario.domain.COURT_HEIGHT
import it.manu.fuoriorario.domain.COURT_WIDTH
import it.manu.fuoriorario.domain.KEY_BOTTOM
import it.manu.fuoriorario.domain.KEY_LEFT
import it.manu.fuoriorario.domain.KEY_RIGHT
import it.manu.fuoriorario.domain.Shots
import it.manu.fuoriorario.domain.WING_SLICE_X
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.domain.ZoneHeat
import it.manu.fuoriorario.domain.heat
import it.manu.fuoriorario.domain.zoneAt
import it.manu.fuoriorario.ui.components.Legend
import it.manu.fuoriorario.ui.theme.FuoriOrarioColors
import it.manu.fuoriorario.ui.theme.FuoriOrarioTheme
import kotlin.math.PI
import kotlin.math.atan2
import org.jetbrains.compose.resources.stringResource

/** Prototype `.z-*` fills. */
private fun FuoriOrarioColors.fill(heat: ZoneHeat) = when (heat) {
    ZoneHeat.HOT -> hot.copy(alpha = 0.78f)
    ZoneHeat.EVEN -> even.copy(alpha = 0.7f)
    ZoneHeat.COLD -> cold.copy(alpha = 0.7f)
    ZoneHeat.NONE -> none.copy(alpha = 0.55f)
}

private val heatName = mapOf(
    ZoneHeat.HOT to Res.string.heat_hot,
    ZoneHeat.EVEN to Res.string.heat_even,
    ZoneHeat.COLD to Res.string.heat_cold,
    ZoneHeat.NONE to Res.string.heat_none
)

/** Prototype `ZONES` label anchors (text baseline) in the viewBox; corners only fit the number. */
private class Label(val x: Float, val y: Float, val small: Boolean = false)

private val labels = mapOf(
    Zone.PIT to Label(250f, 100f),
    Zone.MLS to Label(100f, 115f),
    Zone.MLC to Label(250f, 232f),
    Zone.MLD to Label(400f, 115f),
    Zone.ACS to Label(17f, 42f, small = true),
    Zone.ALS to Label(52f, 330f),
    Zone.CEN to Label(250f, 350f),
    Zone.ALD to Label(448f, 330f),
    Zone.ACD to Label(483f, 42f, small = true)
)

/**
 * Prototype "Mappa di tiro" panel content: the court, the free throw box, the detail of the tapped zone
 * (tap again to close) and the legend. [totals] and [refs] cover every zone.
 */
@Composable
fun ShotMap(totals: Map<Zone, Shots>, refs: Map<Zone, Int>) {
    val c = FuoriOrarioTheme.colors
    var selected by remember { mutableStateOf<Zone?>(null) }
    val toggle = { zone: Zone -> selected = if (selected == zone) null else zone }
    val heats = Zone.entries.associateWith { heat(totals.getValue(it), refs.getValue(it)) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(Res.string.map_title), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(Res.string.map_hint).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = c.muted
        )
    }
    Court(totals, heats, selected, toggle)
    FreeThrows(totals.getValue(Zone.TL), heats.getValue(Zone.TL), selected == Zone.TL) { toggle(Zone.TL) }
    selected?.let { Detail(it, totals.getValue(it), refs.getValue(it)) }
    Legend(
        c.cold to heatName.getValue(ZoneHeat.COLD),
        c.even to heatName.getValue(ZoneHeat.EVEN),
        c.hot to heatName.getValue(ZoneHeat.HOT)
    )
}

/** The three-point line, open at the baseline; [closed] runs back along the baseline to bound the inside. */
internal fun arcPath(closed: Boolean) = Path().apply {
    val corner = atan2(ARC_CORNER_Y - BASKET_Y, BASKET_X - ARC_SIDE) * 180f / PI.toFloat()
    moveTo(ARC_SIDE, 0f)
    lineTo(ARC_SIDE, ARC_CORNER_Y)
    // From the left corner down through the top of the arc to the right one.
    arcTo(Rect(Offset(BASKET_X, BASKET_Y), ARC_RADIUS), 180f - corner, -(180f - 2 * corner), false)
    lineTo(COURT_WIDTH - ARC_SIDE, 0f)
    if (closed) close()
}

/** The 9 zones in viewBox units, disjoint like [zoneAt]: the slices outside the arc, mid-range inside it, the key. */
private fun zonePaths(): Map<Zone, Path> {
    val inside = arcPath(closed = true)
    fun polygon(vararg p: Float) = Path().apply {
        moveTo(p[0], p[1])
        for (i in 2 until p.size step 2) lineTo(p[i], p[i + 1])
        close()
    }
    fun three(vararg p: Float) = Path().apply { op(polygon(*p), inside, PathOperation.Difference) }
    fun rect(l: Float, t: Float, r: Float, b: Float) = Path().apply { addRect(Rect(l, t, r, b)) }
    fun mid(l: Float, t: Float, r: Float) =
        Path().apply { op(rect(l, t, r, COURT_HEIGHT), inside, PathOperation.Intersect) }
    val w = COURT_WIDTH
    val h = COURT_HEIGHT
    return mapOf(
        Zone.ACS to three(0f, 0f, BASKET_X, 0f, BASKET_X, BASKET_Y, 0f, CORNER_SLICE_Y),
        Zone.ALS to three(BASKET_X, BASKET_Y, 0f, CORNER_SLICE_Y, 0f, h, WING_SLICE_X, h),
        Zone.CEN to three(BASKET_X, BASKET_Y, WING_SLICE_X, h, w - WING_SLICE_X, h),
        Zone.ALD to three(BASKET_X, BASKET_Y, w, CORNER_SLICE_Y, w, h, w - WING_SLICE_X, h),
        Zone.ACD to three(w, 0f, BASKET_X, 0f, BASKET_X, BASKET_Y, w, CORNER_SLICE_Y),
        Zone.MLS to mid(0f, 0f, KEY_LEFT),
        Zone.MLC to mid(KEY_LEFT, KEY_BOTTOM, KEY_RIGHT),
        Zone.MLD to mid(KEY_RIGHT, 0f, w),
        Zone.PIT to rect(KEY_LEFT, 0f, KEY_RIGHT, KEY_BOTTOM)
    )
}

/** Prototype `courtSvg`: drawn in the 500 × 470 viewBox scaled to the width; taps go through [zoneAt]. */
@Composable
private fun Court(totals: Map<Zone, Shots>, heats: Map<Zone, ZoneHeat>, selected: Zone?, onTap: (Zone) -> Unit) {
    val c = FuoriOrarioTheme.colors
    val zones = remember { zonePaths() }
    val arc = remember { arcPath(closed = false) }
    val tap by rememberUpdatedState(onTap)
    val description = stringResource(Res.string.map_description)
    BoxWithConstraints(
        Modifier.fillMaxWidth().aspectRatio(COURT_WIDTH / COURT_HEIGHT).clip(RoundedCornerShape(10.dp))
    ) {
        val unit = maxWidth / COURT_WIDTH
        Canvas(
            Modifier
                .matchParentSize()
                .semantics { contentDescription = description }
                .pointerInput(Unit) {
                    detectTapGestures { p ->
                        val s = size.width / COURT_WIDTH
                        zoneAt(p.x / s, p.y / s)?.let(tap)
                    }
                }
        ) {
            scale(size.width / COURT_WIDTH, pivot = Offset.Zero) {
                drawRect(c.court)
                zones.forEach { (zone, path) ->
                    drawPath(path, c.fill(heats.getValue(zone)))
                    drawPath(path, c.courtLine.copy(alpha = 0.25f), style = Stroke(1f))
                }
                zones[selected]?.let { drawPath(it, c.ink, style = Stroke(3f)) }
                drawLines(arc, c.courtLine)
            }
        }
        labels.forEach { (zone, label) ->
            ZoneLabel(zone, label, totals.getValue(zone), heats.getValue(zone), zone == selected, unit, onTap)
        }
    }
}

/**
 * Prototype `.lines`: frame, arc, key, free throw circle, restricted area, backboard, rim, centre circle. In viewBox
 * units, [arc] from [arcPath]; the plays draw them too.
 */
internal fun DrawScope.drawLines(arc: Path, color: Color) {
    val line = Stroke(2.5f)
    fun half(cx: Float, cy: Float, r: Float, lower: Boolean, style: Stroke = line) =
        drawArc(color, if (lower) 0f else 180f, 180f, false, Offset(cx - r, cy - r), Size(2 * r, 2 * r), style = style)
    drawRect(color, Offset(1.25f, 1.25f), Size(COURT_WIDTH - 2.5f, COURT_HEIGHT - 2.5f), style = line)
    drawPath(arc, color, style = line)
    drawRect(color, Offset(KEY_LEFT, 0f), Size(KEY_RIGHT - KEY_LEFT, KEY_BOTTOM), style = line)
    half(BASKET_X, KEY_BOTTOM, 60f, lower = true)
    half(
        BASKET_X,
        KEY_BOTTOM,
        60f,
        lower = false,
        Stroke(2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 8f)))
    )
    half(BASKET_X, BASKET_Y, 12.5f, lower = true)
    drawLine(color, Offset(220f, 40f), Offset(280f, 40f), 2.5f)
    drawCircle(color, 7.5f, Offset(BASKET_X, BASKET_Y), style = line)
    half(BASKET_X, COURT_HEIGHT, 60f, lower = false)
}

/**
 * Percent and made/attempted over a zone, centred on the prototype's anchor. It is the zone's accessible node:
 * named, classed and clickable; touches elsewhere go to the court's hit test.
 */
@Composable
private fun ZoneLabel(
    zone: Zone,
    label: Label,
    shots: Shots,
    heat: ZoneHeat,
    selected: Boolean,
    unit: Dp,
    onTap: (Zone) -> Unit
) {
    val c = FuoriOrarioTheme.colors
    val name = stringResource(zoneName.getValue(zone))
    val heatLabel = stringResource(heatName.getValue(heat))
    // Text scales with the court like the SVG, not with the font size setting.
    val density = LocalDensity.current
    fun units(v: Float) = with(density) { (unit * v).toSp() }
    val number = MaterialTheme.typography.headlineMedium
    val p = shots.percent
    Column(
        Modifier
            .layout { m, constraints ->
                val placeable = m.measure(constraints.copy(minWidth = 0, minHeight = 0))
                // Baselines in the prototype: the block's centre sits a little below for two lines, above for one.
                val x = (unit * label.x).roundToPx() - placeable.width / 2
                val y = (unit * (label.y + if (label.small) -6f else 2f)).roundToPx() - placeable.height / 2
                layout(placeable.width, placeable.height) { placeable.place(x, y) }
            }
            // Focusable and keyboard-operable like the prototype's tabindex; a tap here hits the same zone anyway.
            .clickable(role = Role.Button) { onTap(zone) }
            .semantics {
                contentDescription = name
                stateDescription = heatLabel
                this.selected = selected
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (label.small) {
            Text(p?.toString() ?: "—", style = number, fontSize = units(16f), lineHeight = units(16f), color = c.ink)
        } else {
            Text(
                p?.let {
                    "$it%"
                } ?: "—",
                style = number,
                fontSize = units(24f),
                lineHeight = units(24f),
                color = c.ink
            )
            Text(
                if (shots.attempted >
                    0
                ) {
                    "${shots.made}/${shots.attempted}"
                } else {
                    stringResource(Res.string.map_no_shots)
                },
                style = number,
                fontSize = units(14f),
                lineHeight = units(16f),
                fontWeight = FontWeight.SemiBold,
                color = c.ink.copy(alpha = 0.75f)
            )
        }
    }
}

/** Prototype `.ft`: the free throw box, coloured like a zone. */
@Composable
private fun FreeThrows(shots: Shots, heat: ZoneHeat, selected: Boolean, onTap: () -> Unit) {
    val c = FuoriOrarioTheme.colors
    val shape = RoundedCornerShape(10.dp)
    val name = stringResource(zoneName.getValue(Zone.TL))
    val heatLabel = stringResource(heatName.getValue(heat))
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.fill(heat))
            .then(if (selected) Modifier.border(3.dp, c.ink, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onTap)
            .semantics {
                contentDescription = name
                stateDescription = heatLabel
                this.selected = selected
            }
            .padding(14.dp, 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(Res.string.map_free_line).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = c.ink
            )
            Text(
                if (shots.attempted >
                    0
                ) {
                    "${shots.made}/${shots.attempted}"
                } else {
                    stringResource(Res.string.map_free_none)
                },
                color = c.ink
            )
        }
        Text(
            shots.percent?.let { "$it%" } ?: "—",
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 26.sp,
            color = c.ink
        )
    }
}

/** Prototype `.zone-detail`: announced when it changes. */
@Composable
private fun Detail(zone: Zone, shots: Shots, ref: Int) {
    val c = FuoriOrarioTheme.colors
    val bold = SpanStyle(fontWeight = FontWeight.Bold)
    val name = stringResource(zoneName.getValue(zone))
    val made = stringResource(Res.string.map_detail_shots, shots.made, shots.attempted)
    val none = stringResource(Res.string.map_detail_none)
    val reference = stringResource(Res.string.map_detail_ref, "$ref%")
    Text(
        buildAnnotatedString {
            withStyle(bold) { append(name) }
            append(": ")
            val p = shots.percent
            if (p == null) {
                append(none)
            } else {
                append("$made · ")
                withStyle(bold) { append("$p%") }
            }
            append(" · $reference")
        },
        Modifier
            .fillMaxWidth()
            .background(c.surface2, RoundedCornerShape(10.dp))
            .padding(12.dp, 10.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodyMedium,
        color = c.ink
    )
}
