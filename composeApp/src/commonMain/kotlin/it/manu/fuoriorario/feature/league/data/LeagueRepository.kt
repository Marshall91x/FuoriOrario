package it.manu.fuoriorario.feature.league.data

import it.manu.fuoriorario.core.error.PermissionDeniedException
import it.manu.fuoriorario.domain.Fixture
import it.manu.fuoriorario.domain.ImportPreview
import it.manu.fuoriorario.domain.League
import kotlin.time.Instant

/** The PDF isn't a FIP calendar, or FIP changed its layout (ADR 0011). */
class UnreadableCalendarException : Exception()

/** Calendario (ADR 0011): RLS lets the whole team read the league, staff import, edit and delete it. */
interface LeagueRepository {
    /** The latest league created from [since] on (the season's start) with its teams and fixtures; null if none. */
    suspend fun league(since: Instant): League?

    /** The FIP PDF read by the Edge Function: writes nothing. Throws [UnreadableCalendarException]. */
    suspend fun preview(pdf: ByteArray): ImportPreview

    /**
     * Writes [preview] into league [id], a new one or the one reimported, with [ours] (a name in it) as ours. Sending
     * it again changes nothing; results are never touched. Throws [PermissionDeniedException].
     */
    suspend fun confirm(id: String, preview: ImportPreview, ours: String)

    /** The league with its teams and fixtures; recorded games stay. Throws [PermissionDeniedException]. */
    suspend fun delete(league: League)

    /** [fixture] into [league], found by number, else added: sending it again changes nothing. Throws [PermissionDeniedException]. */
    suspend fun save(league: League, fixture: Fixture)

    /** Throws [PermissionDeniedException]. */
    suspend fun delete(league: League, fixture: Fixture)
}
