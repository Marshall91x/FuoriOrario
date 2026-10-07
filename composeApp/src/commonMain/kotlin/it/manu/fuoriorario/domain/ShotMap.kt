package it.manu.fuoriorario.domain

/** Classe zona: how a zone's percentage compares with its riferimento. */
enum class ZoneHeat { HOT, EVEN, COLD, NONE }

/** HOT from 110% of [refPercent], COLD under 85%; integer maths so 44/100 on 40% is exactly 110%. */
fun heat(shots: Shots, refPercent: Int): ZoneHeat {
    if (shots.attempted == 0) return ZoneHeat.NONE
    val ratio = shots.made * 100L * 100
    return when {
        ratio >= 110L * refPercent * shots.attempted -> ZoneHeat.HOT
        ratio < 85L * refPercent * shots.attempted -> ZoneHeat.COLD
        else -> ZoneHeat.EVEN
    }
}

/** The prototype's riferimenti; the database gives each team the same ones (`teams.zone_refs` default). */
val DEFAULT_ZONE_REFS = mapOf(
    Zone.PIT to 55, Zone.MLS to 40, Zone.MLC to 40, Zone.MLD to 40, Zone.ACS to 36,
    Zone.ALS to 33, Zone.CEN to 33, Zone.ALD to 33, Zone.ACD to 36, Zone.TL to 70
)

/** A riferimento as typed: a whole percentage 1–100, else null. At 0 every zone with shots would be HOT, even 0 made. */
fun parseZoneRef(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in 1..100 }

/** Made / attempted per zone over [sessions]; every zone present. */
fun zoneTotals(sessions: List<ShotSession>): Map<Zone, Shots> =
    Zone.entries.associateWith { z -> sessions.fold(Shots()) { sum, s -> sum + (s.zones[z] ?: Shots()) } }

// Prototype `courtSvg` geometry: the FIBA half court in a 500 × 470 viewBox, baseline at the top.
const val COURT_WIDTH = 500f
const val COURT_HEIGHT = 470f
const val BASKET_X = 250f
const val BASKET_Y = 52.5f
const val ARC_RADIUS = 221.5f

/** Where the arc's straight corner lines end. */
const val ARC_SIDE = 33.5f
const val ARC_CORNER_Y = 99.3f
const val KEY_LEFT = 170f
const val KEY_RIGHT = 330f
const val KEY_BOTTOM = 190f

/** The three-point slices start at the basket and end on the sideline at y 140 and the far edge at x 81. */
const val CORNER_SLICE_Y = 140f
const val WING_SLICE_X = 81f

/** The zone at ([x], [y]) in viewBox coordinates, null off the court; free throws are not on the map. */
fun zoneAt(x: Float, y: Float): Zone? {
    if (x !in 0f..COURT_WIDTH || y !in 0f..COURT_HEIGHT) return null
    if (x in KEY_LEFT..KEY_RIGHT && y <= KEY_BOTTOM) return Zone.PIT
    val dx = x - BASKET_X
    val dy = y - BASKET_Y
    // Straight corner lines down to y 99.3, then the circle around the basket.
    val insideArc = when {
        y <= ARC_CORNER_Y -> x in ARC_SIDE..COURT_WIDTH - ARC_SIDE
        else -> dx * dx + dy * dy <= ARC_RADIUS * ARC_RADIUS
    }
    if (insideArc) {
        return when {
            x < KEY_LEFT -> Zone.MLS
            x > KEY_RIGHT -> Zone.MLD
            else -> Zone.MLC
        }
    }
    // Mirror the right half onto the left: the slices are symmetric.
    val left = x < BASKET_X
    val mx = if (left) x else COURT_WIDTH - x
    val cornerLineY = BASKET_Y + (BASKET_X - mx) * (CORNER_SLICE_Y - BASKET_Y) / BASKET_X
    val wingLineX = BASKET_X - (BASKET_X - WING_SLICE_X) * (y - BASKET_Y) / (COURT_HEIGHT - BASKET_Y)
    return when {
        y < cornerLineY -> if (left) Zone.ACS else Zone.ACD
        mx < wingLineX -> if (left) Zone.ALS else Zone.ALD
        else -> Zone.CEN
    }
}
