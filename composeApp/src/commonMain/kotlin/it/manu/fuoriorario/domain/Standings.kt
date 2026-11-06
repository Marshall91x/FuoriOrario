package it.manu.fuoriorario.domain

/** A row of the *Classifica*: Pt, G, V, P, PF, PS; [diff] is Diff. */
data class Standing(
    val team: LeagueTeam,
    val points: Int = 0,
    val played: Int = 0,
    val won: Int = 0,
    val lost: Int = 0,
    val scored: Int = 0,
    val conceded: Int = 0
) {
    val diff get() = scored - conceded
}

/**
 * Glossary *Classifica*, from the [League]'s played fixtures (a forfait is a normal 20-0): win 2, loss 0. Ties by
 * the table of the fixtures among the tied teams (points, then diff), then total diff, then points scored.
 */
fun League.standings(): List<Standing> {
    val played = fixtures.filter { it.status == FixtureStatus.PLAYED && it.homeScore != null && it.awayScore != null }
    val total = table(teams, played)
    return total.groupBy { it.points }.entries.sortedByDescending { it.key }.flatMap { (_, tied) ->
        // shortcut: the group table is applied once, not again to who is still tied in it; enough for FIP's U19.
        val ids = tied.mapTo(HashSet()) { it.team.id }
        val among = table(tied.map { it.team }, played.filter { it.homeId in ids && it.awayId in ids })
            .associateBy { it.team.id }
        tied.sortedWith(
            compareByDescending<Standing> { among.getValue(it.team.id).points }
                .thenByDescending { among.getValue(it.team.id).diff }
                .thenByDescending { it.diff }
                .thenByDescending { it.scored }
        )
    }
}

private fun table(teams: List<LeagueTeam>, played: List<Fixture>): List<Standing> = teams.map { team ->
    played.filter { it.plays(team) }.fold(Standing(team)) { s, f ->
        val (us, them) = if (f.homeId == team.id) f.homeScore!! to f.awayScore!! else f.awayScore!! to f.homeScore!!
        s.copy(
            points = s.points + if (us > them) 2 else 0,
            played = s.played + 1,
            won = s.won + if (us > them) 1 else 0,
            lost = s.lost + if (us < them) 1 else 0,
            scored = s.scored + us,
            conceded = s.conceded + them
        )
    }
}
