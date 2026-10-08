package it.manu.fuoriorario.core.session

import it.manu.fuoriorario.domain.Member

sealed interface Session {
    /** Restoring a saved session or loading the member. */
    data object Loading : Session

    data object SignedOut : Session

    /** Ended without a sign-out: the server refused the token (revoked, or expired while away). */
    data object Expired : Session

    data class SignedIn(val member: Member) : Session
}

/** Who's signed in, if anyone. */
val Session.member get() = (this as? Session.SignedIn)?.member

/**
 * Where a session ends. supabase-kt clears it ([sessionCleared]) both on sign-out and when it refuses the refresh;
 * with no saved session at all it never had one. Only the app knows whether it [signOutRequested].
 */
fun sessionEnded(sessionCleared: Boolean, signOutRequested: Boolean): Session =
    if (sessionCleared && !signOutRequested) Session.Expired else Session.SignedOut

/** Forgets a session the server refused (401): the app goes back to login with the expiry notice. */
fun interface SessionExpiry {
    fun expire()
}
