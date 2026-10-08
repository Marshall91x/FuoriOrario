package it.manu.fuoriorario.di

import com.russhwolf.settings.Settings
import it.manu.fuoriorario.MainViewModel
import it.manu.fuoriorario.core.error.AppErrorManager
import it.manu.fuoriorario.core.error.ErrorManager
import it.manu.fuoriorario.core.error.FallbackHandler
import it.manu.fuoriorario.core.error.NetworkErrorHandler
import it.manu.fuoriorario.core.error.PermissionDeniedErrorHandler
import it.manu.fuoriorario.core.error.SessionExpiredErrorHandler
import it.manu.fuoriorario.core.session.SelectedPlayer
import it.manu.fuoriorario.core.session.SessionExpiry
import it.manu.fuoriorario.core.viewmodel.DefaultDispatcherProvider
import it.manu.fuoriorario.core.viewmodel.DispatcherProvider
import it.manu.fuoriorario.data.PlanRepository
import it.manu.fuoriorario.data.RosterRepository
import it.manu.fuoriorario.data.SupabasePlanRepository
import it.manu.fuoriorario.data.SupabaseRosterRepository
import it.manu.fuoriorario.feature.auth.LoginViewModel
import it.manu.fuoriorario.feature.auth.PrivacyViewModel
import it.manu.fuoriorario.feature.auth.data.AuthRepository
import it.manu.fuoriorario.feature.auth.data.SupabaseAuthRepository
import it.manu.fuoriorario.feature.plays.PlaysViewModel
import it.manu.fuoriorario.feature.plays.data.PlayRepository
import it.manu.fuoriorario.feature.plays.data.SupabasePlayRepository
import it.manu.fuoriorario.feature.shots.ShotLogViewModel
import it.manu.fuoriorario.feature.shots.data.ShotRepository
import it.manu.fuoriorario.feature.shots.data.SupabaseShotRepository
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.binds
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/** Everything but the backend and the device: the UI tests start it with fakes in their place. */
val appModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    // Handlers hold the failure and its retry: new ones per ViewModel, or a retry would run another screen's load.
    // In order: session expired, permission denied, network, anything else (ADR 0009).
    factory<ErrorManager> {
        AppErrorManager(
            listOf(
                SessionExpiredErrorHandler(),
                PermissionDeniedErrorHandler(),
                NetworkErrorHandler(),
                FallbackHandler()
            ),
            get()
        )
    }
    singleOf(::SelectedPlayer)

    viewModelOf(::MainViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::PrivacyViewModel)
    // The player comes from the screen: koinViewModel { parametersOf(player) }.
    viewModelOf(::ShotLogViewModel)
    viewModelOf(::PlaysViewModel)
}

/** Supabase and the device's own storage (only the staff's picked player). */
val dataModule = module {
    single<Settings> { Settings() }
    single { SupabaseAuthRepository() } binds arrayOf(AuthRepository::class, SessionExpiry::class)
    single<RosterRepository> { SupabaseRosterRepository() }
    single<ShotRepository> { SupabaseShotRepository() }
    single<PlanRepository> { SupabasePlanRepository() }
    single<PlayRepository> { SupabasePlayRepository() }
}

/** One per process: ViewModels outlive an Activity, so what they hold must too. */
val appKoin by lazy { koinApplication { modules(appModule, dataModule) } }
