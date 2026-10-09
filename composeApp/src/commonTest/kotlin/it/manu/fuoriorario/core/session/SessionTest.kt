package it.manu.fuoriorario.core.session

import kotlin.test.Test
import kotlin.test.assertEquals

class SessionTest {
    @Test
    fun clearedWithoutAsking_isExpired() {
        assertEquals(Session.Expired, sessionEnded(sessionCleared = true, signOutRequested = false))
    }

    @Test
    fun signOut_isSignedOut() {
        assertEquals(Session.SignedOut, sessionEnded(sessionCleared = true, signOutRequested = true))
    }

    @Test
    fun noSavedSession_isSignedOut() {
        assertEquals(Session.SignedOut, sessionEnded(sessionCleared = false, signOutRequested = false))
    }
}
