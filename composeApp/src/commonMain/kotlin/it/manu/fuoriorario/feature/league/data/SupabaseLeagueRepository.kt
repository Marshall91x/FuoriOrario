package it.manu.fuoriorario.feature.league.data

import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import it.manu.fuoriorario.core.error.mapErrors
import it.manu.fuoriorario.core.error.requireRow
import it.manu.fuoriorario.core.supabase
import it.manu.fuoriorario.domain.Fixture
import it.manu.fuoriorario.domain.FixtureStatus
import it.manu.fuoriorario.domain.ImportPreview
import it.manu.fuoriorario.domain.League
import it.manu.fuoriorario.domain.LeagueTeam
import it.manu.fuoriorario.domain.Leg
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

private val json = Json { ignoreUnknownKeys = true }

class SupabaseLeagueRepository : LeagueRepository {
    // RLS shows only the signed-in member's team.
    override suspend fun league(since: Instant): League? {
        val league = supabase.from("leagues").select {
            filter { gte("created_at", since.toString()) }
            order("created_at", Order.DESCENDING)
            limit(1)
        }.decodeList<LeagueRow>().firstOrNull() ?: return null
        val teams = supabase.from("league_teams").select { filter { eq("league_id", league.id) } }
            .decodeList<TeamRow>()
            .map { LeagueTeam(it.id, it.name, it.ours) }
        val fixtures = supabase.from("fixtures").select { filter { eq("league_id", league.id) } }
            .decodeList<FixtureRow>()
            .map { it.fixture }
        return League(league.id, league.name, teams, fixtures)
    }

    override suspend fun preview(pdf: ByteArray): ImportPreview {
        val response = try {
            supabase.functions.invoke("import-league") {
                contentType(ContentType.Application.Pdf)
                setBody(pdf)
            }
        } catch (e: RestException) {
            throw if (e.statusCode == 422) UnreadableCalendarException() else e
        }
        return json.decodeFromString(response.bodyAsText())
    }

    override suspend fun confirm(id: String, preview: ImportPreview, ours: String) = mapErrors {
        supabase.postgrest.rpc(
            "import_league",
            buildJsonObject {
                put(
                    "league",
                    buildJsonObject {
                        put("id", id)
                        put("name", preview.name)
                    }
                )
                put("ours", ours)
                put("fixtures", Json.encodeToJsonElement(preview.fixtures))
            }
        )
        Unit
    }

    override suspend fun delete(league: League) = mapErrors {
        supabase.from("leagues").delete {
            select()
            filter { eq("id", league.id) }
        }.requireRow()
    }

    override suspend fun save(league: League, fixture: Fixture) = mapErrors {
        supabase.from("fixtures").upsert(FixtureWrite(league.id, fixture)) {
            onConflict = "league_id,number"
            select()
        }.requireRow()
    }

    override suspend fun delete(league: League, fixture: Fixture) = mapErrors {
        supabase.from("fixtures").delete {
            select()
            filter {
                eq("league_id", league.id)
                eq("number", fixture.number)
            }
        }.requireRow()
    }
}

@Serializable
private class LeagueRow(val id: String, val name: String)

@Serializable
private class TeamRow(val id: String, val name: String, val ours: Boolean)

@Serializable
private class FixtureRow(
    val number: String,
    val round: Int,
    val leg: Leg,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("home_team_id") val homeId: String,
    @SerialName("away_team_id") val awayId: String,
    val venue: String? = null,
    val status: FixtureStatus,
    @SerialName("home_score") val homeScore: Int? = null,
    @SerialName("away_score") val awayScore: Int? = null
) {
    val fixture get() = Fixture(
        number, round, leg, startsAt?.let(Instant::parse), homeId, awayId, venue, status, homeScore, awayScore
    )
}

/** No defaults: every column is sent, so a cleared date or result is written as null. The team id is the DB's. */
@Serializable
private class FixtureWrite(
    @SerialName("league_id") val leagueId: String,
    val number: String,
    val round: Int,
    val leg: Leg,
    @SerialName("starts_at") val startsAt: String?,
    @SerialName("home_team_id") val homeId: String,
    @SerialName("away_team_id") val awayId: String,
    val venue: String?,
    val status: FixtureStatus,
    @SerialName("home_score") val homeScore: Int?,
    @SerialName("away_score") val awayScore: Int?
) {
    constructor(leagueId: String, f: Fixture) : this(
        leagueId, f.number, f.round, f.leg, f.startsAt?.toString(), f.homeId, f.awayId, f.venue, f.status, f.homeScore,
        f.awayScore
    )
}
