package it.manu.fuoriorario.core.session

import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class SessionTest {
    private val signedIn = Session.SignedIn(Member("Luca B.", Role.PLAYER))

    private suspend fun marked(vararg sessions: Session, requested: Boolean = false) =
        flowOf(*sessions).markExpired { requested }.toList()

    @Test
    fun signedOutAfterSignedIn_isExpired() = runTest {
        assertEquals(
            listOf(Session.Loading, signedIn, Session.Loading, Session.Expired),
            marked(Session.Loading, signedIn, Session.Loading, Session.SignedOut)
        )
    }

    @Test
    fun requestedSignOut_isSignedOut() = runTest {
        assertEquals(listOf(signedIn, Session.SignedOut), marked(signedIn, Session.SignedOut, requested = true))
    }

    @Test
    fun neverSignedIn_isSignedOut() = runTest {
        assertEquals(listOf(Session.Loading, Session.SignedOut), marked(Session.Loading, Session.SignedOut))
    }

    @Test
    fun afterExpiry_nextSignOutIsPlain() = runTest {
        assertEquals(
            listOf(signedIn, Session.Expired, Session.SignedOut),
            marked(signedIn, Session.SignedOut, Session.SignedOut)
        )
    }
}
