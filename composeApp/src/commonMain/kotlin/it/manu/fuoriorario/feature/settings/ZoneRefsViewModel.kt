package it.manu.fuoriorario.feature.settings

import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.refs_saved
import it.manu.fuoriorario.core.error.ErrorManager
import it.manu.fuoriorario.core.viewmodel.DispatcherProvider
import it.manu.fuoriorario.core.viewmodel.ScreenModel
import it.manu.fuoriorario.domain.DEFAULT_ZONE_REFS
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.domain.parseZoneRef
import it.manu.fuoriorario.feature.shots.data.ShotRepository

/** [draft] is the fields as typed, null while loading; [invalid]: the last save found a field out of range. */
data class ZoneRefsScreenState(
    val draft: Map<Zone, String>? = null,
    val invalid: Boolean = false,
    val busy: Boolean = false
)

/** Staff's Riferimenti (PRD F5): the expected percentage per zone, which colours every player's map. */
class ZoneRefsViewModel(
    private val shots: ShotRepository,
    dispatchers: DispatcherProvider,
    errorManager: ErrorManager
) : ScreenModel<ZoneRefsScreenState>(ZoneRefsScreenState(), dispatchers, errorManager, busy = { copy(busy = it) }) {
    init {
        defaultLaunch(dispatchers.main()) {
            val refs = shots.zoneRefs()
            set { copy(draft = refs.asText()) }
        }
    }

    fun onRefChanged(zone: Zone, value: String) = update { copy(draft = draft?.plus(zone to value)) }

    /** Only fills in the defaults: nothing is stored until [onSave]. */
    fun onRestore() = update { copy(draft = DEFAULT_ZONE_REFS.asText(), invalid = false) }

    fun onSave() {
        val refs = state.draft?.mapValues { parseZoneRef(it.value) } ?: return
        val invalid = refs.values.any { it == null }
        update { copy(invalid = invalid) }
        if (invalid || state.busy) return
        act {
            shots.setZoneRefs(refs.mapValues { it.value!! })
            toast(Res.string.refs_saved)
        }
    }

    private fun Map<Zone, Int>.asText() = mapValues { it.value.toString() }
}
