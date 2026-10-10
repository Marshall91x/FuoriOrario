package it.manu.fuoriorario.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

class GameTest {
    private fun shot(zone: Zone, made: Boolean, quarter: Quarter = Quarter.Q1) =
        GameEvent(GameEventType.SHOT, quarter, "luca", zone, made)

    private fun freeThrow(made: Boolean, quarter: Quarter = Quarter.Q1) =
        GameEvent(GameEventType.FREE_THROW, quarter, "luca", made = made)

    private fun opponent(points: Int, quarter: Quarter = Quarter.Q1) =
        GameEvent(GameEventType.OPPONENT, quarter, value = points)

    @Test
    fun points_twoInsideTheArcThreeBeyondOneAFreeThrowNoneMissed() {
        assertEquals(2, shot(Zone.PIT, made = true).points)
        assertEquals(2, shot(Zone.MLC, made = true).points)
        assertEquals(3, shot(Zone.ACS, made = true).points)
        assertEquals(3, shot(Zone.CEN, made = true).points)
        assertEquals(0, shot(Zone.CEN, made = false).points)
        assertEquals(1, freeThrow(made = true).points)
        assertEquals(0, freeThrow(made = false).points)
    }

    @Test
    fun score_oursFromShotsTheirsFromOpponentPoints() {
        val events = listOf(
            shot(Zone.CEN, made = true),
            shot(Zone.PIT, made = false),
            freeThrow(made = true),
            opponent(2),
            opponent(3),
            GameEvent(GameEventType.REBOUND, Quarter.Q1, "luca")
        )

        assertEquals(Score(4, 5), score(events))
    }

    @Test
    fun score_noEvents_nilNil() {
        assertEquals(Score(0, 0), score(emptyList()))
    }

    @Test
    fun quarterScores_fourQuartersAlwaysOvertimeOnlyIfPlayed() {
        val regular = listOf(shot(Zone.PIT, made = true), opponent(1, Quarter.Q3))

        assertEquals(
            mapOf(Quarter.Q1 to Score(2, 0), Quarter.Q2 to Score(), Quarter.Q3 to Score(0, 1), Quarter.Q4 to Score()),
            quarterScores(regular)
        )
        assertEquals(Score(3, 0), quarterScores(regular + shot(Zone.ACD, true, Quarter.OT)).getValue(Quarter.OT))
    }

    @Test
    fun shotZones_madeAndAttemptedPerZoneEveryZonePresentFreeThrowsApart() {
        val zones = shotZones(listOf(shot(Zone.CEN, true), shot(Zone.CEN, false), freeThrow(true), opponent(3)))

        assertEquals(Shots(1, 2), zones.getValue(Zone.CEN))
        assertEquals(Shots(1, 1), zones.getValue(Zone.TL))
        assertEquals(Shots(), zones.getValue(Zone.PIT))
        assertEquals(Zone.entries.toSet(), zones.keys)
    }

    @Test
    fun inQuarter_nullIsTheWholeGame() {
        val events = listOf(shot(Zone.PIT, true), opponent(2, Quarter.Q2), shot(Zone.CEN, false, Quarter.OT))

        assertEquals(events, events.inQuarter(null))
        assertEquals(listOf(opponent(2, Quarter.Q2)), events.inQuarter(Quarter.Q2))
    }

    @Test
    fun boxScore_aRowPerCallUpInTheirOrderWithTwosThreesFreeThrowsAndTheRest() {
        val luca = CallUp("luca", "Luca B.", "7")
        val anna = CallUp("anna", "Anna", "4")
        fun luca(type: GameEventType) = GameEvent(type, Quarter.Q1, "luca")
        val events = listOf(
            shot(Zone.PIT, true),
            shot(Zone.MLS, false),
            shot(Zone.CEN, true),
            shot(Zone.ACD, false),
            freeThrow(true),
            freeThrow(false),
            opponent(3),
            luca(GameEventType.REBOUND),
            luca(GameEventType.REBOUND),
            luca(GameEventType.ASSIST),
            luca(GameEventType.TURNOVER),
            luca(GameEventType.STEAL),
            luca(GameEventType.FOUL)
        )

        assertEquals(
            listOf(
                BoxLine(luca, 6, Shots(1, 2), Shots(1, 2), Shots(1, 2), 2, 1, 1, 1, 1),
                BoxLine(anna, 0, Shots(), Shots(), Shots(), 0, 0, 0, 0, 0)
            ),
            boxScore(listOf(luca, anna), events)
        )
    }

    @Test
    fun fouledOut_whoeverHasFiveFoulsOrMore() {
        fun foul(who: String) = GameEvent(GameEventType.FOUL, Quarter.Q1, who)
        val rebound = GameEvent(GameEventType.REBOUND, Quarter.Q1, "anna")
        val events = List(5) { foul("luca") } + List(4) { foul("anna") } + rebound

        assertEquals(setOf("luca"), fouledOut(events))
        assertEquals(setOf("luca", "anna"), fouledOut(events + foul("anna") + foul("luca")))
    }

    @Test
    fun toGame_carriesTheScore() {
        val draft = GameDraft(
            "g1",
            LocalDate(2026, 10, 10),
            "Virtus",
            home = false,
            callUps = listOf(CallUp("luca", "Luca B.", "7")),
            events = listOf(shot(Zone.CEN, made = true), opponent(2))
        )

        assertEquals(Game(LocalDate(2026, 10, 10), "Virtus", false, null, 3, 2, "g1"), draft.toGame())
    }

    @Test
    fun draft_survivesJson() {
        val draft = GameDraft(
            "g1",
            LocalDate(2026, 10, 10),
            "Virtus",
            home = true,
            note = "Amichevole",
            callUps = listOf(CallUp("luca", "Luca B.", "7"), CallUp("anna", "Anna")),
            events = listOf(shot(Zone.CEN, true, Quarter.OT), freeThrow(false), opponent(3)),
            quarter = Quarter.OT,
            selected = "anna"
        )

        assertEquals(draft, Json.decodeFromString<GameDraft>(Json.encodeToString(draft)))
    }

    @Test
    fun gameError_needsAnOpponent() {
        assertEquals(GameError.NO_OPPONENT, gameError(" "))
        assertEquals(null, gameError("Virtus"))
    }
}
