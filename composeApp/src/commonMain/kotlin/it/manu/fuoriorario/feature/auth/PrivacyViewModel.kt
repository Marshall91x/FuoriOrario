package it.manu.fuoriorario.feature.auth

import it.manu.fuoriorario.core.error.ErrorManager
import it.manu.fuoriorario.core.viewmodel.ComposeViewModel
import it.manu.fuoriorario.core.viewmodel.DispatcherProvider
import it.manu.fuoriorario.core.viewmodel.UiState
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.feature.auth.data.AuthRepository

/** [failed]: the last "Ho letto" didn't reach the server. */
data class PrivacyScreenState(val busy: Boolean = false, val failed: Boolean = false)

/** First-access privacy notice (ADR 0006). Accepting is the whole outcome: the session then moves on (MainViewModel). */
class PrivacyViewModel(private val auth: AuthRepository, dispatchers: DispatcherProvider, errorManager: ErrorManager) :
    ComposeViewModel<PrivacyScreenState>(
        defaultState = PrivacyScreenState().let { UiState(UseCaseMutableState.ShowData(it), it) },
        dispatcherProvider = dispatchers,
        errorManager = errorManager
    ) {
    fun onAccept() {
        if (uiState.value.data?.busy == true) return
        // Inline error, as before the rework: the notice has no toast host.
        defaultLaunchForChannels(errorFunction = { emitSuccess(PrivacyScreenState(failed = true)) }) {
            emitSuccess(PrivacyScreenState(busy = true))
            auth.acknowledgePrivacy()
            emitSuccess(PrivacyScreenState())
        }
    }
}
