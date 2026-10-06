package it.manu.fuoriorario.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

class ShotSessionTest {
    private val day = LocalDate(2026, 10, 6)

    @Test
    fun steppers_madeByOneRaisingAttempted_attemptedByFive_neverBelowZero() {
        assertEquals(Shots(1, 1), Shots().stepMade(up = true))
        assertEquals(Shots(3, 5), Shots(2, 5).stepMade(up = true))
        assertEquals(Shots(0, 5), Shots(0, 5).stepMade(up = false))
        assertEquals(Shots(2, 10), Shots(2, 5).stepAttempted(up = true))
        assertEquals(Shots(2, 0), Shots(2, 3).stepAttempted(up = false))
        assertEquals(Shots(12, 12), Shots(1, 5).withMade(12))
        assertEquals(Shots(3, 5), Shots(1, 5).withMade(3))
    }

    @Test
    fun sessionError_firstZoneWithMadeOverAttempted_thenNoAttempts() {
        val zones = mapOf(Zone.TL to Shots(5, 4), Zone.PIT to Shots(3, 2), Zone.MLS to Shots(1, 5))
        assertEquals(SessionError.MadeOverAttempted(Zone.PIT), sessionError(zones))
        assertEquals(SessionError.NoAttempts, sessionError(mapOf(Zone.PIT to Shots(0, 0))))
        assertEquals(SessionError.NoAttempts, sessionError(emptyMap()))
        assertNull(sessionError(mapOf(Zone.PIT to Shots(0, 0), Zone.TL to Shots(0, 5))))
    }

    @Test
    fun newSession_keepsZonesWithAttempts_blankNoteIsNull() {
        val zones = mapOf(Zone.PIT to Shots(0, 0), Zone.TL to Shots(7, 10))
        assertEquals(ShotSession(day, mapOf(Zone.TL to Shots(7, 10))), newSession(day, zones, "  "))
        assertEquals("Gambe stanche", newSession(day, zones, " Gambe stanche ").note)
    }

    @Test
    fun fieldGoalExcludesFreeThrows_percentRounded() {
        val s = ShotSession(day, mapOf(Zone.PIT to Shots(3, 5), Zone.CEN to Shots(2, 3), Zone.TL to Shots(1, 8)))
        assertEquals(Shots(5, 8), s.fieldGoal)
        assertEquals(Shots(1, 8), s.freeThrows)
        assertEquals(63, s.fieldGoal.percent) // 62.5 rounds up, like the prototype's Math.round
        assertEquals(13, s.freeThrows.percent)
        assertNull(ShotSession(day, mapOf(Zone.TL to Shots(1, 2))).fieldGoal.percent)
        assertEquals(Shots(), ShotSession(day, mapOf(Zone.PIT to Shots(1, 2))).freeThrows)
    }

    @Test
    fun json_matchesTheDatabaseRow() {
        val s = ShotSession(day, mapOf(Zone.PIT to Shots(3, 5), Zone.TL to Shots(7, 10)), "ok")
        val json = """{"date":"2026-10-06","zones":{"pit":[3,5],"tl":[7,10]},"note":"ok"}"""
        assertEquals(json, Json.encodeToString(ShotSession.serializer(), s))
        assertEquals(
            """{"date":"2026-10-06","zones":{"pit":[3,5],"tl":[7,10]},"note":"ok","member_id":"m"}""",
            Json.encodeToString(ShotSession.serializer(), s.copy(memberId = "m"))
        )
        assertEquals(
            s.copy(id = "x", memberId = "m"),
            Json { ignoreUnknownKeys = true }.decodeFromString(
                ShotSession.serializer(),
                """{"id":"x","member_id":"m","date":"2026-10-06","zones":{"pit":[3,5],"tl":[7,10]},"note":"ok"}"""
            )
        )
    }
}
