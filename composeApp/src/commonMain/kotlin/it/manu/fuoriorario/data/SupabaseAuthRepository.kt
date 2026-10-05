package it.manu.fuoriorario.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.from
import it.manu.fuoriorario.core.supabase
import it.manu.fuoriorario.domain.Member
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

class SupabaseAuthRepository(private val client: SupabaseClient = supabase) : AuthRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override val session: Flow<Session> = client.auth.sessionStatus.mapLatest { status ->
        when (status) {
            is SessionStatus.Authenticated -> {
                val member = loadMember(status.session.user!!.id)
                if (member != null) {
                    Session.SignedIn(member)
                } else {
                    // Removed from the roster after signing up: nothing to show.
                    signOut()
                    Session.SignedOut
                }
            }
            is SessionStatus.NotAuthenticated -> Session.SignedOut
            else -> Session.Loading
        }
    }

    // ponytail: retries forever while offline (app is online-only, ADR 0004); add an error state if users get stuck.
    private suspend fun loadMember(userId: String): Member? {
        while (true) {
            try {
                return client.from("members").select { filter { eq("user_id", userId) } }.decodeSingleOrNull()
            } catch (_: HttpRequestException) {
                delay(3_000)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return null // Server/RLS/decoding error: treat as no member, i.e. sign out.
            }
        }
    }

    override suspend fun sendCode(email: String) {
        try {
            client.auth.signInWith(OTP) { this.email = email }
        } catch (e: AuthRestException) {
            // Raised by the Before User Created hook (supabase/migrations).
            if (e.errorDescription == "not_a_member") throw NotInTeamException()
            throw e
        }
    }

    override suspend fun verifyCode(email: String, code: String) {
        try {
            client.auth.verifyEmailOtp(OtpType.Email.EMAIL, email, code)
        } catch (e: AuthRestException) {
            if (e.errorCode == AuthErrorCode.OtpExpired) throw InvalidCodeException()
            throw e
        }
    }

    override suspend fun signOut() {
        try {
            client.auth.signOut()
        } finally {
            // Offline logout still forgets the session on this device.
            client.auth.clearSession()
        }
    }
}
