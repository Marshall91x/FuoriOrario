package it.manu.fuoriorario.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

class ShotMapTest {
    @Test
    fun heat_comparesWithTheReference() {
        // Reference 40%: hot from 44% (exactly 110%, no float rounding), cold under 34%.
        assertEquals(ZoneHeat.HOT, heat(Shots(44, 100), 40))
        assertEquals(ZoneHeat.EVEN, heat(Shots(43, 100), 40))
        assertEquals(ZoneHeat.EVEN, heat(Shots(34, 100), 40))
        assertEquals(ZoneHeat.COLD, heat(Shots(33, 100), 40))
        assertEquals(ZoneHeat.COLD, heat(Shots(0, 5), 40))
        assertEquals(ZoneHeat.NONE, heat(Shots(), 40))
    }

    @Test
    fun zoneAt_paintAndMidRange() {
        assertEquals(Zone.PIT, zoneAt(250f, 100f))
        // The key wins over the mid-range rectangles at its edges.
        assertEquals(Zone.PIT, zoneAt(171f, 189f))
        assertEquals(Zone.MLS, zoneAt(100f, 115f))
        assertEquals(Zone.MLC, zoneAt(250f, 232f))
        assertEquals(Zone.MLD, zoneAt(400f, 115f))
    }

    @Test
    fun zoneAt_insideAndOutsideTheArc() {
        // Corner: the straight part of the arc is at x 33.5 and 466.5.
        assertEquals(Zone.MLS, zoneAt(40f, 50f))
        assertEquals(Zone.ACS, zoneAt(20f, 50f))
        assertEquals(Zone.MLD, zoneAt(460f, 50f))
        assertEquals(Zone.ACD, zoneAt(480f, 50f))
        // Top of the arc: basket (250, 52.5) + radius 221.5 = 274.
        assertEquals(Zone.MLC, zoneAt(250f, 273f))
        assertEquals(Zone.CEN, zoneAt(250f, 275f))
        // Wings, along the 45° diagonal from the basket (radius / √2 ≈ 156.6).
        assertEquals(Zone.MLS, zoneAt(250f - 155f, 52.5f + 155f))
        assertEquals(Zone.ALS, zoneAt(250f - 158f, 52.5f + 158f))
        assertEquals(Zone.MLD, zoneAt(250f + 155f, 52.5f + 155f))
        assertEquals(Zone.ALD, zoneAt(250f + 158f, 52.5f + 158f))
    }

    @Test
    fun zoneAt_threePointSlices() {
        // Corner / wing border runs from the basket to (0, 140).
        assertEquals(Zone.ACS, zoneAt(10f, 130f))
        assertEquals(Zone.ALS, zoneAt(10f, 145f))
        // Wing / centre border runs from the basket to (81, 470).
        assertEquals(Zone.ALS, zoneAt(75f, 465f))
        assertEquals(Zone.CEN, zoneAt(90f, 465f))
        assertEquals(Zone.CEN, zoneAt(410f, 465f))
        assertEquals(Zone.ALD, zoneAt(425f, 465f))
        assertEquals(Zone.ACD, zoneAt(490f, 130f))
        assertEquals(Zone.ALD, zoneAt(490f, 145f))
    }

    @Test
    fun zoneAt_outsideTheCourt() {
        assertNull(zoneAt(-1f, 100f))
        assertNull(zoneAt(501f, 100f))
        assertNull(zoneAt(250f, 471f))
    }

    private val day = LocalDate(2026, 10, 6)

    @Test
    fun zoneTotals_sumsEachZone() {
        val sessions = listOf(
            ShotSession(day, mapOf(Zone.PIT to Shots(3, 5), Zone.TL to Shots(7, 10))),
            ShotSession(day, mapOf(Zone.PIT to Shots(1, 5)))
        )
        val totals = zoneTotals(sessions)
        assertEquals(Shots(4, 10), totals[Zone.PIT])
        assertEquals(Shots(7, 10), totals[Zone.TL])
        assertEquals(Shots(), totals[Zone.CEN])
    }

    @Test
    fun parseZoneRef_wholePercentFrom1To100() {
        assertEquals(1, parseZoneRef("1"))
        assertEquals(55, parseZoneRef(" 55 "))
        assertEquals(100, parseZoneRef("100"))
        assertNull(parseZoneRef(""))
        assertNull(parseZoneRef("101"))
        assertNull(parseZoneRef("0"))
        assertNull(parseZoneRef("-1"))
        assertNull(parseZoneRef("5,5"))
        assertNull(parseZoneRef("abc"))
    }
}
