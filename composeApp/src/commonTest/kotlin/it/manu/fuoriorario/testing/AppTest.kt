package it.manu.fuoriorario.testing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.allStringArrayResources
import fuoriorario.composeapp.generated.resources.allStringResources
import it.manu.fuoriorario.App
import it.manu.fuoriorario.core.session.SessionExpiry
import it.manu.fuoriorario.core.viewmodel.DispatcherProvider
import it.manu.fuoriorario.data.RosterRepository
import it.manu.fuoriorario.di.appModule
import it.manu.fuoriorario.feature.auth.FakeAuthRepository
import it.manu.fuoriorario.feature.auth.data.AuthRepository
import it.manu.fuoriorario.feature.plan.FakePlanRepository
import it.manu.fuoriorario.feature.plan.data.PlanRepository
import it.manu.fuoriorario.feature.plays.FakePlayRepository
import it.manu.fuoriorario.feature.plays.data.PlayRepository
import it.manu.fuoriorario.feature.shots.FakeShotRepository
import it.manu.fuoriorario.feature.shots.data.ShotRepository
import it.manu.fuoriorario.ui.roster.FakeRosterRepository
import kotlin.test.fail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getStringArray
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * The real [App] with fakes in place of Supabase and of the device's storage ([prefs]). Leaving the composition
 * is quitting the app: ViewModels and Koin go with it, so a `key` around this is a restart.
 */
@Composable
fun TestApp(
    auth: FakeAuthRepository = FakeAuthRepository(),
    roster: RosterRepository = FakeRosterRepository(),
    shots: ShotRepository = FakeShotRepository(),
    prefs: Settings = MapSettings(),
    plans: PlanRepository = FakePlanRepository(),
    plays: PlayRepository = FakePlayRepository()
) {
    val koin = remember {
        koinApplication {
            modules(
                appModule,
                module {
                    // A fake answers at once: on the caller's thread the screen has the result when the test looks.
                    single<DispatcherProvider> { UnconfinedDispatcherProvider }
                    single<AuthRepository> { auth }
                    single<SessionExpiry> { auth }
                    single { roster }
                    single { shots }
                    single { prefs }
                    single { plans }
                    single { plays }
                }
            )
        }
    }
    val owner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            owner.viewModelStore.clear()
            koin.close()
        }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) { App(koin) }
}

private object UnconfinedDispatcherProvider : DispatcherProvider {
    override fun main() = Dispatchers.Unconfined

    override fun default() = Dispatchers.Unconfined

    override fun io() = Dispatchers.Unconfined
}

/** Like waitUntilExactlyOneExists, but suspends: on web, resources load asynchronously and a blocking wait starves them. */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.awaitText(text: String) = awaitNode(hasText(text))

/** Waits for exactly [count] nodes matching [matcher]: 0 waits for them to go. */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.awaitNode(matcher: SemanticsMatcher, count: Int = 1) {
    var found = 0
    repeat(200) {
        found = onAllNodes(matcher).fetchSemanticsNodes().size
        if (found == count) return
        withContext(Dispatchers.Default) { delay(25) }
    }
    fail("${matcher.description}: $found nodes, expected $count")
}

/**
 * runComposeUiTest with every string already loaded. On web each string is read asynchronously the first time it shows:
 * until then stringResource gives "", so an assert or click right after a screen appears fails depending on which
 * tests ran before. Once loaded they stay in memory and appear from the first frame, as on iOS and Android.
 */
@OptIn(ExperimentalTestApi::class)
fun runAppTest(block: suspend ComposeUiTest.() -> Unit) = runComposeUiTest {
    withContext(Dispatchers.Default) {
        Res.allStringResources.values.forEach { getString(it) }
        Res.allStringArrayResources.values.forEach { getStringArray(it) }
    }
    block()
}
