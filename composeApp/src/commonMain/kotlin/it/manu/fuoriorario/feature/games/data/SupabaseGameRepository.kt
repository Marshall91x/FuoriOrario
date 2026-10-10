package it.manu.fuoriorario.feature.games.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import it.manu.fuoriorario.core.error.mapErrors
import it.manu.fuoriorario.core.error.requireRow
import it.manu.fuoriorario.core.supabase
import it.manu.fuoriorario.domain.CallUp
import it.manu.fuoriorario.domain.Game
import it.manu.fuoriorario.domain.GameDraft
import it.manu.fuoriorario.domain.GameEvent
import it.manu.fuoriorario.domain.GameEventType
import it.manu.fuoriorario.domain.Quarter
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.domain.rosterOrder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

// The quarter as text, like its serial names: PostgREST gives a smallint as a JSON number.
private val EVENT_COLUMNS = Columns.raw("game_id,type,quarter::text,member_id,zone,made,value")

// With defaults: a score of 0 is the default, and the RPC needs it.
private val withDefaults = Json { encodeDefaults = true }

class SupabaseGameRepository : GameRepository {
    // RLS shows only the signed-in member's team.
    override suspend fun games(): List<Game> = supabase.from("games").select {
        order("date", Order.DESCENDING)
        order("created_at", Order.DESCENDING)
    }.decodeList()

    override suspend fun save(draft: GameDraft): Game = mapErrors {
        supabase.postgrest.rpc(
            "save_game",
            buildJsonObject {
                put("game", withDefaults.encodeToJsonElement(draft.toGame()))
                put("call_ups", JsonArray(draft.callUps.map { JsonPrimitive(it.id) }))
                put("events", Json.encodeToJsonElement(draft.events))
            }
        ).decodeAs()
    }

    override suspend fun callUps(game: Game): List<CallUp> = supabase.from("game_call_ups")
        .select(Columns.raw("member_id,members(display_name,jersey_number)")) { filter { eq("game_id", game.id!!) } }
        .decodeList<CallUpRow>()
        .map { CallUp(it.memberId, it.member.displayName, it.member.jerseyNumber) }
        .rosterOrder()

    override suspend fun events(game: Game): List<GameEvent> = supabase.from("game_events").select(EVENT_COLUMNS) {
        filter { eq("game_id", game.id!!) }
        order("seq", Order.ASCENDING)
    }.decodeList<EventRow>().map { it.event }

    // RLS shows a player only their own events.
    override suspend fun ownEvents(): Map<String, List<GameEvent>> = supabase.from("game_events")
        .select(EVENT_COLUMNS) { order("seq", Order.ASCENDING) }
        .decodeList<EventRow>()
        .groupBy({ it.gameId }, { it.event })

    override suspend fun delete(game: Game) = mapErrors {
        supabase.from("games").delete {
            select()
            filter { eq("id", game.id!!) }
        }.requireRow()
    }
}

@Serializable
private class CallUpRow(@SerialName("member_id") val memberId: String, @SerialName("members") val member: CallUpMember)

@Serializable
private class CallUpMember(
    @SerialName("display_name") val displayName: String,
    @SerialName("jersey_number") val jerseyNumber: String? = null
)

/** A [GameEvent] with its game, which the domain doesn't keep. */
@Serializable
private class EventRow(
    @SerialName("game_id") val gameId: String,
    val type: GameEventType,
    val quarter: Quarter,
    @SerialName("member_id") val memberId: String? = null,
    val zone: Zone? = null,
    val made: Boolean? = null,
    val value: Int? = null
) {
    val event get() = GameEvent(type, quarter, memberId, zone, made, value)
}
