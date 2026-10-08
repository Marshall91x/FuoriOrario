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
                it.parents().any { p -> p.name.substringBefore("<") == "ComposeViewModel" } &&
                    (it.resideInPackage("$ROOT.feature..") || it.name == "MainViewModel" && it.resideInPackage(ROOT))
            }
    }

    @Test
    fun screensLiveInAFeature() {
        production.files.withNameEndingWith("Screen")
            .filterNot { it.packagee?.name.notMigrated() }
            .assertTrue { it.hasPackage("$ROOT.feature..") }
    }

    @Test
    fun noPackageByLayerButDomain() {
        production.packages
            .filterNot { it.name.notMigrated() }
            .assertFalse { it.name.removePrefix("$ROOT.").substringBefore('.') in LAYERS }
    }

    @Test
    fun domainIsPure() {
        production.files
            .filter { it.hasPackage("$ROOT.domain..") }
            .assertFalse { file -> file.imports.any { i -> NOT_IN_DOMAIN.any(i.name::startsWith) } }
    }

    private fun String?.notMigrated() = this != null && NOT_MIGRATED.any { startsWith("$ROOT.$it") }

    private companion object {
        const val ROOT = "it.manu.fuoriorario"
        val LAYERS = setOf("ui", "data", "viewmodel")
        val NOT_IN_DOMAIN = listOf("androidx.compose", "org.jetbrains.compose", "io.github.jan.supabase", "org.koin")

        /** Packages still waiting for their MVVM issue: remove each line when it closes. */
        val NOT_MIGRATED = listOf(
            "ui.plan", // #52
            "ui.plays", // #53
            "ui.roster", // #54
            "ui.team", // #55
            "ui.settings", // #56
            "data" // repositories, #51–#54
        )
    }
}
