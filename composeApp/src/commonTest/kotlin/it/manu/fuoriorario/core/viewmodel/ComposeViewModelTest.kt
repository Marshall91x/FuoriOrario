package it.manu.fuoriorario.core.viewmodel

import it.manu.fuoriorario.core.error.AppErrorManager
import it.manu.fuoriorario.core.error.ErrorHandlerWithRetry
import it.manu.fuoriorario.core.error.NetworkErrorHandler
import it.manu.fuoriorario.testing.TestDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ComposeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private class LoadViewModel(provider: DispatcherProvider) :
        ComposeViewModel<Unit>(
            dispatcherProvider = provider,
            errorManager = AppErrorManager(listOf(NetworkErrorHandler())) {}
        ) {
        var failing = true
        var finished = 0

        fun load() = defaultLaunch {
            delay(1_000)
            if (failing) throw IOException("offline")
            finished++
        }
    }

    private val vm = LoadViewModel(TestDispatcherProvider(dispatcher))

    private fun retry(): () -> Unit {
        val error = vm.uiState.value.state as UseCaseMutableState.Error
        return assertNotNull((error.handler as ErrorHandlerWithRetry).retryFunction)
    }

    @Test
    fun networkError_alwaysOffersRetry() = runTest(dispatcher) {
        vm.load()
        advanceUntilIdle()

        repeat(10) {
            retry()()
            advanceUntilIdle()
        }

        assertNotNull(retry())
    }

    @Test
    fun retry_tappedTwice_loadsOnce() = runTest(dispatcher) {
        vm.load()
        advanceUntilIdle()
        vm.failing = false

        val retry = retry()
        retry()
        runCurrent() // the first retry is now loading
        retry()
        advanceUntilIdle()

        assertEquals(1, vm.finished)
    }
}
