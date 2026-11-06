package it.manu.fuoriorario.feature.league

import it.manu.fuoriorario.core.ROME
import it.manu.fuoriorario.domain.Fixture
import it.manu.fuoriorario.domain.FixtureStatus
import it.manu.fuoriorario.domain.ImportPreview
import it.manu.fuoriorario.domain.League
import it.manu.fuoriorario.domain.LeagueTeam
import it.manu.fuoriorario.feature.league.data.LeagueRepository
import it.manu.fuoriorario.feature.league.data.UnreadableCalendarException
import kotlin.time.Instant
import kotlinx.datetime.toInstant
import kotlinx.io.IOException

/** Imports like the RPC: found by number, date (Rome time) and venue updated, results kept, nothing removed. */
class FakeLeagueRepository(var league: League? = null) : LeagueRepository {
    /** What the Edge Function reads from any PDF; null: not a calendar. */
    var preview: ImportPreview? = null

    /** The league ids confirmed, in order. */
    val confirmed = mutableListOf<String>()

    /** Next load fails without network, so the screen offers "Riprova". */
    var failLoad = false

    /** Next write fails like a network error. */
    var failNext = false

    override suspend fun league(since: Instant): League? {
        if (failLoad) {
            failLoad = false
            throw IOException("offline")
        }
        return league
    }

    override suspend fun preview(pdf: ByteArray): ImportPreview {
        failIfAsked()
        return preview ?: throw UnreadableCalendarException()
    }

    override suspend fun confirm(id: String, preview: ImportPreview, ours: String) {
        failIfAsked()
        confirmed += id
        val old = league?.takeIf { it.id == id }
        val teams = preview.teams.map { name ->
            LeagueTeam(old?.teams?.firstOrNull { it.name == name }?.id ?: name, name, name == ours)
        }
        val fixtures = preview.fixtures.map { f ->
            val start = f.startsAt?.toInstant(ROME)
            val before = old?.fixtures?.firstOrNull { it.number == f.number }
            when {
                before == null -> null
                before.status == FixtureStatus.PLAYED ->
                    before.copy(startsAt = start ?: before.startsAt, venue = f.venue)
                else -> before.copy(
                    startsAt = start,
                    venue = f.venue,
                    status = if (start == null) FixtureStatus.POSTPONED else FixtureStatus.SCHEDULED
                )
            } ?: Fixture(
                f.number,
                f.round,
                f.leg,
                start,
                teams.first { it.name == f.home }.id,
                teams.first { it.name == f.away }.id,
                f.venue
            )
        }
        val kept = old?.fixtures?.filter { o -> fixtures.none { it.number == o.number } }.orEmpty()
        league = League(id, preview.name, teams, fixtures + kept)
    }

    override suspend fun delete(league: League) {
        failIfAsked()
        this.league = null
    }

    override suspend fun save(league: League, fixture: Fixture) {
        failIfAsked()
        val l = this.league!!
        this.league = l.copy(fixtures = l.fixtures.filter { it.number != fixture.number } + fixture)
    }

    override suspend fun delete(league: League, fixture: Fixture) {
        failIfAsked()
        val l = this.league!!
        this.league = l.copy(fixtures = l.fixtures.filter { it.number != fixture.number })
    }

    private fun failIfAsked() {
        if (failNext) {
            failNext = false
            throw IOException("offline")
        }
    }
}
