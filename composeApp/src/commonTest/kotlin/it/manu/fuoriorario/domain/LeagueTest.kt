package it.manu.fuoriorario.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class LeagueTest {
    private val a = LeagueTeam("a", "Artemis", ours = true)
    private val b = LeagueTeam("b", "Vasto")
    private val c = LeagueTeam("c", "Atessa")

    private fun at(s: String) = Instant.parse(s)

    private val league = League(
        "l",
        "U19",
        listOf(a, b, c),
        listOf(
            Fixture("2", 1, Leg.FIRST, at("2026-11-10T18:00:00Z"), "b", "c"),
            Fixture("1", 1, Leg.FIRST, null, "a", "b", status = FixtureStatus.POSTPONED),
            Fixture("4", 1, Leg.SECOND, at("2027-02-08T18:00:00Z"), "c", "a"),
            Fixture("3", 2, Leg.FIRST, at("2026-11-17T18:00:00Z"), "c", "a")
        )
    )

    @Test
    fun rounds_inOrder_firstLegThenSecond_fixturesByTimePostponedLast() {
        val rounds = league.rounds()

        assertEquals(listOf(1 to Leg.FIRST, 2 to Leg.FIRST, 1 to Leg.SECOND), rounds.map { it.number to it.leg })
        assertEquals(listOf("2", "1"), rounds[0].fixtures.map { it.number })
    }

    @Test
    fun rounds_restingIsWhoDoesNotPlay() {
        val rounds = league.rounds()

        assertEquals(emptyList(), rounds[0].resting)
        assertEquals(listOf(b), rounds[1].resting)
    }

    @Test
    fun ours_keepsOurFixturesAndEveryRound() {
        val rounds = league.rounds().ours(a)

        assertEquals(listOf(listOf("1"), listOf("3"), listOf("4")), rounds.map { r -> r.fixtures.map { it.number } })
        assertEquals(listOf(b), rounds[1].resting)
    }

    @Test
    fun nextRound_isTheRoundOfTheNextScheduledFixture() {
        val rounds = league.rounds()

        assertEquals(0, rounds.nextRound(at("2026-11-01T00:00:00Z")))
        assertEquals(1, rounds.nextRound(at("2026-11-12T00:00:00Z")))
        assertEquals(2, rounds.nextRound(at("2026-12-01T00:00:00Z")))
    }

    @Test
    fun nextRound_byTimeNotByRound() {
        // FIP moves a game of round 5 before one of round 4.
        val early = league.copy(
            fixtures =
            league.fixtures + Fixture("5", 2, Leg.FIRST, at("2026-11-05T18:00:00Z"), "a", "b")
        )

        assertEquals(1, early.rounds().nextRound(at("2026-11-01T00:00:00Z")))
    }

    @Test
    fun nextRound_allPlayed_isTheLast() {
        assertEquals(2, league.rounds().nextRound(at("2028-01-01T00:00:00Z")))
        assertEquals(0, emptyList<Round>().nextRound(at("2028-01-01T00:00:00Z")))
    }

    @Test
    fun missing_areTheFixturesOfTheSameLeagueNoLongerInThePdf() {
        val preview = ImportPreview(
            "U19",
            listOf("Artemis", "Vasto"),
            listOf(ImportFixture("2", 1, Leg.FIRST, null, "Vasto", "Atessa"))
        )

        assertEquals(listOf("1", "4", "3"), preview.missing(league).map { it.number })
        assertEquals(emptyList(), preview.copy(name = "U19 - Seconda fase").missing(league))
        assertEquals(emptyList(), preview.missing(null))
    }
}
