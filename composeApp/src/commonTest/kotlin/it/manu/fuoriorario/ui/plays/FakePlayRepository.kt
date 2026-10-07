package it.manu.fuoriorario.ui.plays

import it.manu.fuoriorario.data.PlayRepository
import it.manu.fuoriorario.domain.Play

class FakePlayRepository(private vararg val plays: Play) : PlayRepository {
    override suspend fun plays() = plays.toList()
}
