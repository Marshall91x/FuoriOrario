package it.manu.fuoriorario.data

import it.manu.fuoriorario.domain.Play

/** Schemi: RLS lets the whole team read them and staff write them. */
interface PlayRepository {
    /** The team's plays, in no particular order. */
    suspend fun plays(): List<Play>

    /** The saved play, with its id. */
    suspend fun add(play: Play): Play

    /** The whole play at once, steps included. */
    suspend fun update(play: Play)

    suspend fun remove(play: Play)
}
