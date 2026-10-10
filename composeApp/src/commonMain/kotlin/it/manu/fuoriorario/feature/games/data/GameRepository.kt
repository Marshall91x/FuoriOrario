package it.manu.fuoriorario.feature.games.data

import it.manu.fuoriorario.core.error.PermissionDeniedException
import it.manu.fuoriorario.domain.CallUp
import it.manu.fuoriorario.domain.Game
import it.manu.fuoriorario.domain.GameDraft
import it.manu.fuoriorario.domain.GameEvent

/** Partite: RLS lets the whole team read the games, staff save and delete them; a player reads only their own call-ups and events. */
interface GameRepository {
    /** The team's games, newest first. */
    suspend fun games(): List<Game>

    /**
     * The whole game at once: the game, its call-ups and its events (ADR 0010). Saving the same draft again gives back
     * the game already saved. Throws [PermissionDeniedException].
     */
    suspend fun save(draft: GameDraft): Game

    /** [game]'s convocati in roster order: a player gets only themselves. */
    suspend fun callUps(game: Game): List<CallUp>

    /** [game]'s events in order: a player gets only their own. */
    suspend fun events(game: Game): List<GameEvent>

    /** The signed-in player's events by game id. Players only: staff would get the whole team's. */
    suspend fun ownEvents(): Map<String, List<GameEvent>>

    /** The game with its call-ups and events. Throws [PermissionDeniedException]. */
    suspend fun delete(game: Game)
}
