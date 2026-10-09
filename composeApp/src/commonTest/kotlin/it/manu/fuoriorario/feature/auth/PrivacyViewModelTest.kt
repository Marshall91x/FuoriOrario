package it.manu.fuoriorario.feature.auth

import it.manu.fuoriorario.core.error.AppErrorManager
import it.manu.fuoriorario.core.session.Session
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.testing.TestDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class PrivacyViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val auth = FakeAuthRepository(Member("Luca B.", Role.PLAYER))
    private val vm = PrivacyViewModel(auth, TestDispatcherProvider(dispatcher), AppErrorManager(emptyList()) {})
    private val state get() = vm.uiState.value.data!!

    @Test
    fun accept_recordsIt() = runTest(dispatcher) {
        vm.onAccept()
        advanceUntilIdle()

        assertNotNull((auth.session.value as Session.SignedIn).member.privacyAckAt)
        assertFalse(state.busy)
    }

    @Test
    fun accept_failure_showsErrorThenRetryClearsIt() = runTest(dispatcher) {
        auth.failNext = true
        vm.onAccept()
        advanceUntilIdle()

        assertTrue(state.failed)
        assertFalse(state.busy)

        vm.onAccept()
        advanceUntilIdle()

        assertEquals(PrivacyScreenState(), state)
    }
}
