package it.manu.fuoriorario.ui.plays

import it.manu.fuoriorario.data.PlayRepository
import it.manu.fuoriorario.domain.Play

class FakePlayRepository(vararg plays: Play) : PlayRepository {
    val plays = plays.toMutableList()

    override suspend fun plays() = plays.toList()

    override suspend fun add(play: Play) = play.copy(id = "play${plays.size}").also { plays += it }

    override suspend fun update(play: Play) {
        plays[plays.indexOfFirst { it.id == play.id }] = play
    }

    override suspend fun remove(play: Play) {
        plays.removeAll { it.id == play.id }
    }
}
