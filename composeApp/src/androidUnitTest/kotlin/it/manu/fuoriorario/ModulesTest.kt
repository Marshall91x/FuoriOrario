package it.manu.fuoriorario

import it.manu.fuoriorario.di.appModule
import it.manu.fuoriorario.di.dataModule
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

class ModulesTest {
    /** A constructor Koin can't fill compiles and passes every other test, then crashes on the first screen. */
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun everyDefinitionResolves() {
        module { includes(appModule, dataModule) }.verify()
    }
}
