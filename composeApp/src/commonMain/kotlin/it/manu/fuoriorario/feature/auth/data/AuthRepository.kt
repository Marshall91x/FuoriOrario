package it.manu.fuoriorario.feature.auth.data

import it.manu.fuoriorario.core.session.Session
import kotlinx.coroutines.flow.Flow

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

    /** Records the privacy notice acceptance; [session] then emits the updated member. */
    suspend fun acknowledgePrivacy()

    /** Reloads the member after staff changed their own row: [session] emits it again, or [Session.SignedOut] if they were removed. */
    fun refresh()

    suspend fun signOut()
}
