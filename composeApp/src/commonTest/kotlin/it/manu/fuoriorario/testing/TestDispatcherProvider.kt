package it.manu.fuoriorario.testing

import it.manu.fuoriorario.core.viewmodel.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/** Every dispatcher a ViewModel asks for is [dispatcher]: `defaultLaunch` overrides the scope's own, so no setMain. */
class TestDispatcherProvider(private val dispatcher: TestDispatcher) : DispatcherProvider {
    override fun main(): CoroutineDispatcher = dispatcher

    override fun default(): CoroutineDispatcher = dispatcher

    override fun io(): CoroutineDispatcher = dispatcher

    /** Runs at once, like Unconfined, on the same clock as [dispatcher]. */
    override fun unconfined(): CoroutineDispatcher = UnconfinedTestDispatcher(dispatcher.scheduler)
}
