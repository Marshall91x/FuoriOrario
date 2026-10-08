package it.manu.fuoriorario

import androidx.lifecycle.SavedStateHandle
import it.manu.fuoriorario.di.appModule
import it.manu.fuoriorario.di.dataModule
import it.manu.fuoriorario.domain.Member
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

class ModulesTest {
    /** A constructor Koin can't fill compiles and passes every other test, then crashes on the first screen. */
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun everyDefinitionResolves() {
        // SavedStateHandle and the followed player come from the screen, not from a definition.
        module { includes(appModule, dataModule) }.verify(extraTypes = listOf(SavedStateHandle::class, Member::class))
    }
}
