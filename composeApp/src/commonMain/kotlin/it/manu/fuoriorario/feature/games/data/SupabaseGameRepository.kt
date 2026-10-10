package it.manu.fuoriorario.feature.games.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import it.manu.fuoriorario.core.error.mapErrors
import it.manu.fuoriorario.core.supabase
import it.manu.fuoriorario.domain.Game
import it.manu.fuoriorario.domain.GameDraft
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

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
}
