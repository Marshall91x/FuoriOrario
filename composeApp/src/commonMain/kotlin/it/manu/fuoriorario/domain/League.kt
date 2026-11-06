package it.manu.fuoriorario.domain

import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/** Andata or ritorno of a [Round]. */
@Serializable
enum class Leg { FIRST, SECOND }

/** Glossary *Stato (della gara)*: a postponed fixture has no date, a played one has its result. */
@Serializable
enum class FixtureStatus { SCHEDULED, POSTPONED, PLAYED }

/** Glossary *Squadra del girone*: the FIP name as it is; one is [ours]. */
data class LeagueTeam(val id: String, val name: String, val ours: Boolean = false)

/** Glossary *Gara*: [number] is the FIP one, [homeId] and [awayId] are [LeagueTeam] ids. */
data class Fixture(
    val number: String,
    val round: Int,
    val leg: Leg,
    val startsAt: Instant?,
    val homeId: String,
    val awayId: String,
    val venue: String? = null,
    val status: FixtureStatus = if (startsAt == null) FixtureStatus.POSTPONED else FixtureStatus.SCHEDULED,
    val homeScore: Int? = null,
    val awayScore: Int? = null
) {
    fun plays(team: LeagueTeam) = homeId == team.id || awayId == team.id
}

/** Glossary *Campionato*: a FIP phase with its teams and fixtures. */
data class League(val id: String, val name: String, val teams: List<LeagueTeam>, val fixtures: List<Fixture>) {
    val ours get() = teams.firstOrNull { it.ours }

    fun team(id: String) = teams.first { it.id == id }
}

/** Glossary *Giornata*: "3ª andata". [resting] are the teams without a fixture in it (*Riposo*), whatever is shown. */
data class Round(val number: Int, val leg: Leg, val fixtures: List<Fixture>, val resting: List<LeagueTeam>)

/** Andata then ritorno, each by number; in a round fixtures by time, postponed last. */
fun League.rounds(): List<Round> = fixtures
    .groupBy { it.leg to it.round }
    .entries
    .sortedWith(compareBy({ it.key.first }, { it.key.second }))
    .map { (key, list) ->
        Round(
            key.second,
            key.first,
            list.sortedWith(compareBy(nullsLast()) { it.startsAt }),
            teams.filter { t -> list.none { it.plays(t) } }
        )
    }

/** "Nostre": only [team]'s fixtures; every round stays, a round without them shows our *Riposo*. */
fun List<Round>.ours(team: LeagueTeam?): List<Round> =
    if (team == null) this else map { r -> r.copy(fixtures = r.fixtures.filter { it.plays(team) }) }

/**
 * Where the calendar opens: the round of the first scheduled fixture from [now] on, by time (FIP may play a game
 * of a later round first). The last round once everything is past.
 */
fun List<Round>.nextRound(now: Instant): Int {
    val next = flatMap { r -> r.fixtures.map { r to it } }
        .filter { (_, f) -> f.status == FixtureStatus.SCHEDULED && f.startsAt != null && f.startsAt >= now }
        .minByOrNull { (_, f) -> f.startsAt!! }
    return next?.let { indexOf(it.first) } ?: lastIndex.coerceAtLeast(0)
}

/** A fixture as the import Edge Function reads it from the PDF; [startsAt] is Rome's local time, null if postponed. */
@Serializable
data class ImportFixture(
    val number: String,
    val round: Int,
    val leg: Leg,
    val startsAt: LocalDateTime?,
    val home: String,
    val away: String,
    val venue: String? = null
)

/** *Import del calendario* before the staff confirm it: [teams] are every name in [fixtures]. */
@Serializable
data class ImportPreview(val name: String, val teams: List<String>, val fixtures: List<ImportFixture>)

/** Reimporting the same phase into [league] (FIP keeps its title), rather than a new league. */
fun ImportPreview.reimports(league: League?) = league?.name == name

/** On a reimport, the fixtures no longer in the PDF: they stay, the preview tells. */
fun ImportPreview.missing(league: League?): List<Fixture> {
    if (!reimports(league)) return emptyList()
    val numbers = fixtures.mapTo(HashSet()) { it.number }
    return league!!.fixtures.filter { it.number !in numbers }
}
