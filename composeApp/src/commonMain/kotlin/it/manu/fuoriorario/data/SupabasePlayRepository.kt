package it.manu.fuoriorario.data

import io.github.jan.supabase.postgrest.from
import it.manu.fuoriorario.core.supabase
import it.manu.fuoriorario.domain.Play
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

class SupabasePlayRepository : PlayRepository {
    // RLS shows only the signed-in member's team.
    override suspend fun plays(): List<Play> = supabase.from("plays").select().decodeList()

    override suspend fun add(play: Play): Play = mapErrors {
        supabase.from("plays").insert(play) { select() }.decodeSingle()
    }

    override suspend fun update(play: Play) = mapErrors {
        val fields = JsonObject(Json.encodeToJsonElement(play).jsonObject - "id")
        supabase.from("plays").update(fields) {
            select()
            filter { eq("id", play.id!!) }
        }.requireRow()
    }

    override suspend fun remove(play: Play) = mapErrors {
        supabase.from("plays").delete {
            select()
            filter { eq("id", play.id!!) }
        }.requireRow()
    }
}
