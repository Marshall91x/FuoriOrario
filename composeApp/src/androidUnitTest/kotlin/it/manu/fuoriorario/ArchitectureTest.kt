package it.manu.fuoriorario

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/** The "Architettura" rules of AGENTS.md, on commonMain and the platform sources. */
class ArchitectureTest {
    private val production = Konsist.scopeFromProduction()

    @Test
    fun viewModelsExtendComposeViewModelInAFeature() {
        production.classes().withNameEndingWith("ViewModel")
            .filterNot { it.name == "ComposeViewModel" }
            .assertTrue {
                it.parents(indirectParents = true).any { p -> p.name.substringBefore("<") == "ComposeViewModel" } &&
                    (it.resideInPackage("$ROOT.feature..") || it.name == "MainViewModel" && it.resideInPackage(ROOT))
            }
    }

    @Test
    fun screensLiveInAFeature() {
        production.files.withNameEndingWith("Screen")
            .assertTrue { it.hasPackage("$ROOT.feature..") }
    }

    @Test
    fun noPackageByLayerButDomain() {
        production.packages
            .assertFalse { it.name.removePrefix("$ROOT.").substringBefore('.') in LAYERS }
    }

    @Test
    fun domainIsPure() {
        production.files
            .filter { it.hasPackage("$ROOT.domain..") }
            .assertFalse { file -> file.imports.any { i -> NOT_IN_DOMAIN.any(i.name::startsWith) } }
    }

    private companion object {
        const val ROOT = "it.manu.fuoriorario"
        val LAYERS = setOf("ui", "data", "viewmodel")
        val NOT_IN_DOMAIN = listOf("androidx.compose", "org.jetbrains.compose", "io.github.jan.supabase", "org.koin")
    }
}
