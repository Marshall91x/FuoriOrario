package it.manu.fuoriorario.feature.plays

import it.manu.fuoriorario.domain.Play
import it.manu.fuoriorario.feature.plays.data.PlayRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.io.IOException

class FakePlayRepository(vararg plays: Play) : PlayRepository {
    val plays = plays.toMutableList()

    /** Next load fails without network, so the screen offers "Riprova". */
    var failLoad = false

    /** Next write fails like a network error. */
    var failNext = false

    /** The load waits for it, while it's set: the screen stays loading. */
    var loadGate: CompletableDeferred<Unit>? = null

    /** Writes wait for it, while it's set: the screen stays mid-save. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun plays(): List<Play> {
        loadGate?.await()
        if (failLoad) {
            failLoad = false
            throw IOException("offline")
        }
        return plays.toList()
    }

    override suspend fun add(play: Play): Play {
        beforeWrite()
        return play.copy(id = "play${plays.size}").also { plays += it }
    }

    override suspend fun update(play: Play) {
        beforeWrite()
        plays[plays.indexOfFirst { it.id == play.id }] = play
    }

    override suspend fun remove(play: Play) {
        beforeWrite()
        plays.removeAll { it.id == play.id }
    }

    private suspend fun beforeWrite() {
        gate?.await()
        if (failNext) {
            failNext = false
            error("offline")
        }
    }
}
