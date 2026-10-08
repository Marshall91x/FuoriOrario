package it.manu.fuoriorario.core.session

import it.manu.fuoriorario.domain.Member
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed interface Session {
    /** Restoring a saved session or loading the member. */
    data object Loading : Session

    data object SignedOut : Session

    /** Signed out without asking: supabase-kt couldn't refresh the token (revoked, or expired while away). */
    data object Expired : Session

    data class SignedIn(val member: Member) : Session
}

/**
 * Turns a [Session.SignedOut] that follows a [Session.SignedIn] into [Session.Expired], unless [signOutRequested].
 * supabase-kt reports a refused refresh exactly like a sign-out, so only the app knows which one it asked for.
 */
fun Flow<Session>.markExpired(signOutRequested: () -> Boolean): Flow<Session> = flow {
    var signedIn = false
    collect { session ->
        emit(if (session == Session.SignedOut && signedIn && !signOutRequested()) Session.Expired else session)
        // A refresh in progress shows as Loading: still the same session.
        if (session != Session.Loading) signedIn = session is Session.SignedIn
    }
}
