package it.manu.fuoriorario.core.error

import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.save_denied
import fuoriorario.composeapp.generated.resources.save_failed
import fuoriorario.composeapp.generated.resources.session_expired
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import org.jetbrains.compose.resources.getString

class AppErrorManagerTest {
    private var expired = 0
    private val manager = AppErrorManager(
        listOf(SessionExpiredErrorHandler(), PermissionDeniedErrorHandler(), NetworkErrorHandler(), FallbackHandler())
    ) { expired++ }

    /** What PostgREST answers with a token it no longer accepts. */
    private suspend fun refused(): Exception {
        val response = HttpClient(MockEngine { respond("", HttpStatusCode.Unauthorized) }).get("https://example.com")
        return UnauthorizedRestException("JWT expired", response)
    }

    @Test
    fun refusedLoad_endsTheSession() = runTest {
        assertIs<SessionExpiredErrorHandler>(manager.handle(refused()) {})
        assertEquals(1, expired)
    }

    @Test
    fun refusedAction_endsTheSessionAndSaysSo() = runTest {
        assertEquals(getString(Res.string.session_expired), manager.checkError(refused()))
        assertEquals(1, expired)
    }

    @Test
    fun networkLoad_retriesAndKeepsTheSession() = runTest {
        var retried = false
        val handler = manager.handle(IOException()) { retried = true }

        assertIs<NetworkErrorHandler>(handler).retryFunction!!()
        assertEquals(true, retried)
        assertEquals(0, expired)
    }

    @Test
    fun actionTexts() = runTest {
        assertEquals(getString(Res.string.save_denied), manager.checkError(PermissionDeniedException()))
        assertEquals(getString(Res.string.save_failed), manager.checkError(IOException()))
        assertEquals(0, expired)
    }
}
