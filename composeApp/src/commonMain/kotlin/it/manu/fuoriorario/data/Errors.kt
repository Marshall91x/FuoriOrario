package it.manu.fuoriorario.data

import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.serialization.json.JsonObject

/** Runs a write, turning RLS refusals into [PermissionDeniedException] and the codes in [codes] into their exception. */
internal inline fun <T> mapErrors(codes: Map<String, () -> Exception> = emptyMap(), block: () -> T): T = try {
    block()
} catch (e: PostgrestRestException) {
    throw if (e.code == "42501") PermissionDeniedException() else codes[e.code]?.invoke() ?: e
}

/** RLS filters update/delete silently: no row back means not allowed. The request must `select()`. */
internal fun PostgrestResult.requireRow() {
    if (decodeList<JsonObject>().isEmpty()) throw PermissionDeniedException()
}
