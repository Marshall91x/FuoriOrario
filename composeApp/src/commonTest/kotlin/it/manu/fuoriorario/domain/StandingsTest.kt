package it.manu.fuoriorario.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class StandingsTest {
    private val a = LeagueTeam("a", "Artemis", ours = true)
    private val b = LeagueTeam("b", "Vasto")
    private val c = LeagueTeam("c", "Atessa")
    private val d = LeagueTeam("d", "Lanciano")

    private var n = 0

    private fun played(home: String, away: String, homeScore: Int, awayScore: Int) =
        Fixture("${n++}", 1, Leg.FIRST, null, home, away, null, FixtureStatus.PLAYED, homeScore, awayScore)

    private fun order(vararg fixtures: Fixture) =
        League("l", "U19", listOf(a, b, c, d), fixtures.toList()).standings().map { it.team.id }

    @Test
    fun columns_countOnlyPlayedFixtures_winTwoPoints() {
        val league = League(
            "l",
            "U19",
            listOf(a, b),
            listOf(played("a", "b", 62, 55), Fixture("x", 2, Leg.FIRST, null, "b", "a", homeScore = 90, awayScore = 0))
        )

        val (first, second) = league.standings()

        assertEquals(Standing(a, points = 2, played = 1, won = 1, lost = 0, scored = 62, conceded = 55), first)
        assertEquals(Standing(b, points = 0, played = 1, won = 0, lost = 1, scored = 55, conceded = 62), second)
        assertEquals(7, first.diff)
    }

    @Test
    fun byPoints() {
        assertEquals(
            listOf("c", "a", "b", "d"),
            order(
                played("c", "a", 60, 50),
                played("a", "b", 60, 50),
                played("c", "d", 60, 50),
                played("b", "d", 60, 50)
            )
        )
    }

    @Test
    fun twoTied_headToHeadWins_evenWithWorseTotalDiff() {
        // a and b on 2 points: b beat a by 1, a won its other game by 50.
        assertEquals(listOf("b", "a", "d", "c"), order(played("b", "a", 51, 50), played("a", "c", 90, 40)))
    }

    @Test
    fun threeTied_groupTableByDiffAmongThem() {
        // a, b, c beat each other in a circle: among them a +9, b -1, c -8; against d the totals turn upside down.
        assertEquals(
            listOf("a", "b", "c", "d"),
            order(
                played("a", "b", 60, 50),
                played("b", "c", 60, 51),
                played("c", "a", 60, 59),
                played("d", "a", 50, 51),
                played("d", "b", 50, 100),
                played("d", "c", 50, 250)
            )
        )
    }

    @Test
    fun tiedWithoutHeadToHead_totalDiffThenScored() {
        // a, b, c on 2 points, never met: a +10, b +5 with 80 scored, c +5 with 60.
        assertEquals(
            listOf("a", "b", "c", "d"),
            order(played("a", "d", 60, 50), played("b", "d", 80, 75), played("c", "d", 60, 55))
        )
    }

    @Test
    fun teamWithoutFixtures_isListedWithZeros() {
        val standings = League("l", "U19", listOf(a, b, c), listOf(played("a", "b", 60, 50))).standings()

        assertEquals(Standing(c), standings.single { it.team == c })
        assertEquals(listOf(a, c, b), standings.map { it.team })
    }
}
