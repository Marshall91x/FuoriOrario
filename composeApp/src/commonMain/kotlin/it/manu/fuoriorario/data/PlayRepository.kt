package it.manu.fuoriorario.data

import it.manu.fuoriorario.domain.Play

/** Schemi: RLS lets the whole team read them; staff write them in the editor (#45). */
interface PlayRepository {
    /** The team's plays, in no particular order. */
    suspend fun plays(): List<Play>
}
