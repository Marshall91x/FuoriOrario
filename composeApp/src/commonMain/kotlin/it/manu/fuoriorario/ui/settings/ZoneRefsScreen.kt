package it.manu.fuoriorario.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.refs_default
import fuoriorario.composeapp.generated.resources.refs_error
import fuoriorario.composeapp.generated.resources.refs_hint
import fuoriorario.composeapp.generated.resources.refs_restore
import fuoriorario.composeapp.generated.resources.refs_save
import fuoriorario.composeapp.generated.resources.refs_saved
import fuoriorario.composeapp.generated.resources.refs_title
import it.manu.fuoriorario.core.designsystem.Field
import it.manu.fuoriorario.core.designsystem.GhostButton
import it.manu.fuoriorario.core.designsystem.LoadFailed
import it.manu.fuoriorario.core.designsystem.LocalToast
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.designsystem.PrimaryButton
import it.manu.fuoriorario.core.designsystem.launchWrite
import it.manu.fuoriorario.core.designsystem.show
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.data.ShotRepository
import it.manu.fuoriorario.domain.DEFAULT_ZONE_REFS
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.domain.parseZoneRef
import it.manu.fuoriorario.ui.shots.zoneName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Staff's Riferimenti (PRD F5): the expected percentage per zone, two per row, each with its default below.
 * Restore only fills in the defaults: nothing is stored until Salva, which colours every player's map.
 */
@Composable
fun ZoneRefsScreen(shots: ShotRepository) {
    val c = FuoriOrarioTheme.colors
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()

    /** The fields as typed, null while loading. */
    var draft by remember { mutableStateOf<Map<Zone, String>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var invalid by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(attempt) {
        loadFailed = false
        try {
            draft = shots.zoneRefs().mapValues { it.value.toString() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            loadFailed = true
        }
    }

    if (loadFailed) LoadFailed { attempt++ }
    val fields = draft ?: return

    fun save() {
        val refs = fields.mapValues { parseZoneRef(it.value) }
        invalid = refs.values.any { it == null }
        if (invalid || busy) return
        busy = true
        scope.launchWrite(toast, onDone = { busy = false }) {
            shots.setZoneRefs(refs.mapValues { it.value!! })
            launch { toast.show(getString(Res.string.refs_saved)) }
        }
    }

    Panel {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(Res.string.refs_title).uppercase(), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(Res.string.refs_hint), color = c.muted, style = MaterialTheme.typography.bodyMedium)
        }
        Zone.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { zone ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Field(
                            stringResource(zoneName.getValue(zone)),
                            fields.getValue(zone),
                            { draft = fields + (zone to it) },
                            "ref_${zone.name.lowercase()}",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Text(
                            stringResource(Res.string.refs_default, "${DEFAULT_ZONE_REFS.getValue(zone)}%"),
                            color = c.muted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
        if (invalid) {
            Text(stringResource(Res.string.refs_error), color = c.accent, style = MaterialTheme.typography.bodyMedium)
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PrimaryButton(stringResource(Res.string.refs_save), ::save, enabled = !busy)
            GhostButton(stringResource(Res.string.refs_restore), {
                draft = DEFAULT_ZONE_REFS.mapValues { it.value.toString() }
                invalid = false
            })
        }
    }
}
