package it.manu.fuoriorario.feature.plays

import it.manu.fuoriorario.domain.Play
import it.manu.fuoriorario.feature.plays.data.PlayRepository
import kotlinx.io.IOException

class FakePlayRepository(vararg plays: Play) : PlayRepository {
    val plays = plays.toMutableList()

    /** Next load fails without network, so the screen offers "Riprova". */
    var failLoad = false

    /** Next write fails like a network error. */
    var failNext = false

    override suspend fun plays(): List<Play> {
        if (failLoad) {
            failLoad = false
            throw IOException("offline")
        }
        return plays.toList()
    }

    override suspend fun add(play: Play): Play {
        failIfAsked()
        return play.copy(id = "play${plays.size}").also { plays += it }
    }

    override suspend fun update(play: Play) {
        failIfAsked()
        plays[plays.indexOfFirst { it.id == play.id }] = play
    }

    override suspend fun remove(play: Play) {
        failIfAsked()
        plays.removeAll { it.id == play.id }
    }

    private fun failIfAsked() {
        if (failNext) {
            failNext = false
            error("offline")
        }
    }
}
