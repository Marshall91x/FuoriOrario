package it.manu.fuoriorario.core.session

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The player staff follow in Diario and Piano (#12): survives tab changes and, in [settings], restarts. */
class SelectedPlayer(private val settings: Settings) {
    private val state = MutableStateFlow(settings.getStringOrNull(KEY))

    /** The picked member's id; null, or no longer in the roster, means the first player. */
    val id: StateFlow<String?> = state.asStateFlow()

    fun select(id: String) {
        settings.putString(KEY, id)
        state.value = id
    }

    private companion object {
        const val KEY = "picked_player"
    }
}
