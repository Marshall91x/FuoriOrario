package it.manu.fuoriorario.feature.games

import it.manu.fuoriorario.domain.Game
import it.manu.fuoriorario.domain.GameDraft
import it.manu.fuoriorario.feature.games.data.GameRepository
import kotlinx.io.IOException

class FakeGameRepository(vararg games: Game) : GameRepository {
    val games = games.toMutableList()

    /** The drafts saved, in order. */
    val saved = mutableListOf<GameDraft>()

    /** Next load fails without network, so the screen offers "Riprova". */
    var failLoad = false

    /** Next save fails like a network error. */
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
        return draft.toGame().also { games += it }
    }
}
