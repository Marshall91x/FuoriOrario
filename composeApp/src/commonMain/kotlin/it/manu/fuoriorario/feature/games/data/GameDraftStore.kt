package it.manu.fuoriorario.feature.games.data

import com.russhwolf.settings.Settings
import it.manu.fuoriorario.domain.GameDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/** Partita in corso (ADR 0010): the one draft on this device, copied to [settings] at every tap so a restart keeps it. */
class GameDraftStore(private val settings: Settings) {
    // A draft from an older app version that no longer reads is lost rather than crashing every start.
    private val state = MutableStateFlow(
        settings.getStringOrNull(KEY)?.let { runCatching { Json.decodeFromString<GameDraft>(it) }.getOrNull() }
    )

    val draft: StateFlow<GameDraft?> = state.asStateFlow()

    /** Null forgets it: saved, abandoned or signed out. */
    fun save(draft: GameDraft?) {
        if (draft == null) settings.remove(KEY) else settings.putString(KEY, Json.encodeToString(draft))
        state.value = draft
    }

    private companion object {
        const val KEY = "game_draft"
    }
}
