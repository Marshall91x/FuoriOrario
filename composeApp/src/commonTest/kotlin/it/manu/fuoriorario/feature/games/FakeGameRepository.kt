package it.manu.fuoriorario.feature.games

import it.manu.fuoriorario.domain.CallUp
import it.manu.fuoriorario.domain.Game
import it.manu.fuoriorario.domain.GameDraft
import it.manu.fuoriorario.domain.GameEvent
import it.manu.fuoriorario.feature.games.data.GameRepository
import kotlinx.io.IOException

class FakeGameRepository(vararg games: Game) : GameRepository {
    val games = games.toMutableList()

    /** The drafts saved, in order. */
    val saved = mutableListOf<GameDraft>()

    /** Next load, of the list or of a Riepilogo, fails without network, so the screen offers "Riprova". */
    var failLoad = false

    /** Per game id, what RLS shows the signed-in member: a player only their own. */
    val callUps = mutableMapOf<String, List<CallUp>>()
    val events = mutableMapOf<String, List<GameEvent>>()

    /** Next save or delete fails like a network error. */
    var failNext = false

    override suspend fun games(): List<Game> {
        if (failLoad) {
            failLoad = false
            throw IOException("offline")
        }
        return games.sortedByDescending { it.date }
    }

    override suspend fun save(draft: GameDraft): Game {
        if (failNext) {
            failNext = false
            throw IOException("offline")
        }
        saved += draft
        callUps[draft.id] = draft.callUps
        events[draft.id] = draft.events
        return draft.toGame().also { games += it }
    }

    override suspend fun callUps(game: Game) = callUps[game.id].orEmpty()

    override suspend fun events(game: Game): List<GameEvent> {
        if (failLoad) {
            failLoad = false
            throw IOException("offline")
        }
        return events[game.id].orEmpty()
    }

    override suspend fun ownEvents(): Map<String, List<GameEvent>> =
        callUps.mapValues { (id, _) -> events[id].orEmpty() }

    override suspend fun delete(game: Game) {
        if (failNext) {
            failNext = false
            throw IOException("offline")
        }
        games.remove(game)
    }
}
