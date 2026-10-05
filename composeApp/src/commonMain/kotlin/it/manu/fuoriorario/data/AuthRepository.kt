package it.manu.fuoriorario.data

import it.manu.fuoriorario.domain.Member
import kotlinx.coroutines.flow.Flow

sealed interface Session {
    /** Restoring a saved session or loading the member. */
    data object Loading : Session

    data object SignedOut : Session

    data class SignedIn(val member: Member) : Session
}

/** The email is not in any team's roster. */
class NotInTeamException : Exception()

/** Wrong or expired OTP code. */
class InvalidCodeException : Exception()

interface AuthRepository {
    val session: Flow<Session>

    /** Emails a 6-digit code. Throws [NotInTeamException]. */
    suspend fun sendCode(email: String)

    /** Throws [InvalidCodeException]. On success [session] becomes [Session.SignedIn]. */
    suspend fun verifyCode(email: String, code: String)

    suspend fun signOut()
}
