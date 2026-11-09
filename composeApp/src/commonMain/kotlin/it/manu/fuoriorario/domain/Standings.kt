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
 * the table of the fixtures among the tied teams (points, then diff), applied again to who is still tied in it
 * (*classifica avulsa*), then total diff, then points scored.
 */
fun League.standings(): List<Standing> {
    val played = fixtures.filter { it.status == FixtureStatus.PLAYED && it.homeScore != null && it.awayScore != null }
    return untie(rows(teams, played), played) { it.points }
}

/** [tied] in order by [key], best first; each group still tied goes through the group table. */
private fun untie(tied: List<Standing>, played: List<Fixture>, key: (Standing) -> Int): List<Standing> =
    tied.groupBy(key).entries.sortedByDescending { it.key }.flatMap { (_, group) -> amongThemselves(group, played) }

/** Points among [tied], then diff among them: a split starts again on each part. Else total diff, then scored. */
private fun amongThemselves(tied: List<Standing>, played: List<Fixture>): List<Standing> {
    if (tied.size == 1) return tied
    val ids = tied.mapTo(HashSet()) { it.team.id }
    val among = rows(tied.map { it.team }, played.filter { it.homeId in ids && it.awayId in ids })
        .associateBy { it.team.id }
    for (key in listOf<(Standing) -> Int>({ among.getValue(it.team.id).points }, { among.getValue(it.team.id).diff })) {
        if (tied.distinctBy(key).size > 1) return untie(tied, played, key)
    }
    return tied.sortedWith(compareByDescending<Standing> { it.diff }.thenByDescending { it.scored })
}

private fun rows(teams: List<LeagueTeam>, played: List<Fixture>): List<Standing> = teams.map { team ->
    played.filter { it.plays(team) }.fold(Standing(team)) { row, f ->
        val (us, them) = if (f.homeId == team.id) f.homeScore!! to f.awayScore!! else f.awayScore!! to f.homeScore!!
        val won = if (us > them) 1 else 0
        row.copy(
            points = row.points + 2 * won,
            played = row.played + 1,
            won = row.won + won,
            lost = row.lost + if (us < them) 1 else 0,
            scored = row.scored + us,
            conceded = row.conceded + them
        )
    }
}
