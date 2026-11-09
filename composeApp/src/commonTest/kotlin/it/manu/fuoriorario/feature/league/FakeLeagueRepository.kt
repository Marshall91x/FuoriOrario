package it.manu.fuoriorario.feature.league

import it.manu.fuoriorario.core.ROME
import it.manu.fuoriorario.domain.Fixture
import it.manu.fuoriorario.domain.FixtureStatus
import it.manu.fuoriorario.domain.Game
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

    /** Like the upsert: the id is the row's, the live game's link isn't written. */
    override suspend fun save(league: League, fixture: Fixture): Fixture {
        failIfAsked()
        val l = this.league!!
        val old = l.fixtures.firstOrNull { it.number == fixture.number }
        val saved = fixture.copy(id = old?.id ?: fixture.number, gameId = old?.gameId)
        this.league = l.copy(fixtures = l.fixtures.filter { it.number != fixture.number } + saved)
        return saved
    }

    override suspend fun delete(league: League, fixture: Fixture) {
        failIfAsked()
        val l = this.league!!
        this.league = l.copy(fixtures = l.fixtures.filter { it.number != fixture.number })
    }

    /** Like `save_game`: the fixture not linked yet gets the link and [game]'s result as played. */
    fun recorded(fixtureId: String, game: Game) {
        val l = league ?: return
        league = l.copy(
            fixtures = l.fixtures.map { f ->
                when {
                    f.id != fixtureId || f.gameId != null -> f
                    else -> {
                        val home = l.team(f.homeId).ours
                        f.copy(
                            gameId = game.id,
                            status = FixtureStatus.PLAYED,
                            homeScore = if (home) game.ourScore else game.theirScore,
                            awayScore = if (home) game.theirScore else game.ourScore
                        )
                    }
                }
            }
        )
    }

    private fun failIfAsked() {
        if (failNext) {
            failNext = false
            throw IOException("offline")
        }
    }
}
