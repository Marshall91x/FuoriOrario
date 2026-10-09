package it.manu.fuoriorario.feature.games.data

import it.manu.fuoriorario.core.error.PermissionDeniedException
import it.manu.fuoriorario.domain.Game
import it.manu.fuoriorario.domain.GameDraft

/** Partite: RLS lets the whole team read the games and staff save them. */
interface GameRepository {
    /** The team's games, newest first. */
    suspend fun games(): List<Game>

    /**
     * The whole game at once: the game, its call-ups and its events (ADR 0010). Saving the same draft again gives back
     * the game already saved. Throws [PermissionDeniedException].
     */
    suspend fun save(draft: GameDraft): Game
}
