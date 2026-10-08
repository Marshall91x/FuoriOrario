package it.manu.fuoriorario.feature.auth

import androidx.lifecycle.SavedStateHandle
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.login_error_code
import fuoriorario.composeapp.generated.resources.login_error_generic
import fuoriorario.composeapp.generated.resources.login_error_not_in_team
import it.manu.fuoriorario.core.error.AppErrorManager
import it.manu.fuoriorario.core.session.Session
import it.manu.fuoriorario.testing.TestDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val auth = FakeAuthRepository()
    private val saved = SavedStateHandle()
    private val vm = vm()

    private val state get() = vm.uiState.value.data!!

    @Test
    fun sendCode_normalizesEmail() = runTest(dispatcher) {
        vm.onEmailChanged(" Giocatore@Example.com ")
        vm.onSendCode()
        advanceUntilIdle()

        assertEquals("giocatore@example.com", auth.sentTo)
        assertTrue(state.codeSent)
        assertNull(state.error)
        assertFalse(state.busy)
    }

    @Test
    fun sendCode_withoutAt_doesNothing() = runTest(dispatcher) {
        vm.onEmailChanged("giocatore")
        vm.onSendCode()
        advanceUntilIdle()

        assertNull(auth.sentTo)
    }

    @Test
    fun sendCode_notInTeam_showsIt() = runTest(dispatcher) {
        vm.onEmailChanged("sconosciuto@example.com")
        vm.onSendCode()
        advanceUntilIdle()

        assertEquals(Res.string.login_error_not_in_team, state.error)
        assertFalse(state.codeSent)
    }

    @Test
    fun sendCode_failure_showsGenericErrorAndKeepsEmail() = runTest(dispatcher) {
        auth.failNext = true
        vm.onEmailChanged("giocatore@example.com")
        vm.onSendCode()
        advanceUntilIdle()

        assertEquals(Res.string.login_error_generic, state.error)
        assertEquals("giocatore@example.com", state.email)
        assertFalse(state.busy)
    }

    @Test
    fun code_keepsSixDigits() = runTest(dispatcher) {
        vm.onCodeChanged("12a3 45678")

        assertEquals("123456", state.code)
    }

    @Test
    fun verify_wrongCode_showsIt() = runTest(dispatcher) {
        codeSent()
        vm.onCodeChanged("000000")
        vm.onVerify()
        advanceUntilIdle()

        assertEquals(Res.string.login_error_code, state.error)
    }

    @Test
    fun verify_rightCode_signsIn() = runTest(dispatcher) {
        codeSent()
        vm.onCodeChanged("123456")
        vm.onVerify()
        advanceUntilIdle()

        assertIs<Session.SignedIn>(auth.session.value)
    }

    @Test
    fun verify_shortCode_doesNothing() = runTest(dispatcher) {
        codeSent()
        vm.onCodeChanged("123")
        vm.onVerify()
        advanceUntilIdle()

        assertEquals(Session.SignedOut, auth.session.value)
    }

    @Test
    fun changeEmail_backToEmailWithoutCodeOrError() = runTest(dispatcher) {
        codeSent()
        vm.onCodeChanged("000000")
        vm.onVerify()
        advanceUntilIdle()

        vm.onChangeEmail()

        assertFalse(state.codeSent)
        assertEquals("", state.code)
        assertNull(state.error)
        assertEquals("giocatore@example.com", state.email)
    }

    @Test
    fun draftSurvivesProcessDeath() = runTest(dispatcher) {
        codeSent()
        vm.onCodeChanged("123")

        val restored = vm().uiState.value.data!!

        assertEquals("giocatore@example.com", restored.email)
        assertEquals("123", restored.code)
        assertTrue(restored.codeSent)
    }

    /** A new ViewModel on the same [saved]: what Android rebuilds after killing the process. */
    private fun vm() = LoginViewModel(auth, saved, TestDispatcherProvider(dispatcher), AppErrorManager(emptyList()) {})

    private fun TestScope.codeSent() {
        vm.onEmailChanged("giocatore@example.com")
        vm.onSendCode()
        advanceUntilIdle()
    }
}
