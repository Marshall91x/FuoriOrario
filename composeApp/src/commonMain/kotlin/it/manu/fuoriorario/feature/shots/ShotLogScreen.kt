package it.manu.fuoriorario.feature.shots

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.cancel
import fuoriorario.composeapp.generated.resources.close
import fuoriorario.composeapp.generated.resources.ok
import fuoriorario.composeapp.generated.resources.period
import fuoriorario.composeapp.generated.resources.period_30
import fuoriorario.composeapp.generated.resources.period_7
import fuoriorario.composeapp.generated.resources.period_season
import fuoriorario.composeapp.generated.resources.role_player
import fuoriorario.composeapp.generated.resources.session_attempted
import fuoriorario.composeapp.generated.resources.session_date
import fuoriorario.composeapp.generated.resources.session_error_empty
import fuoriorario.composeapp.generated.resources.session_error_made
import fuoriorario.composeapp.generated.resources.session_hint
import fuoriorario.composeapp.generated.resources.session_less
import fuoriorario.composeapp.generated.resources.session_made
import fuoriorario.composeapp.generated.resources.session_more
import fuoriorario.composeapp.generated.resources.session_new
import fuoriorario.composeapp.generated.resources.session_note
import fuoriorario.composeapp.generated.resources.session_save
import fuoriorario.composeapp.generated.resources.shots_delete
import fuoriorario.composeapp.generated.resources.shots_delete_confirm
import fuoriorario.composeapp.generated.resources.shots_empty
import fuoriorario.composeapp.generated.resources.shots_log
import fuoriorario.composeapp.generated.resources.shots_title
import fuoriorario.composeapp.generated.resources.stat_attempted
import fuoriorario.composeapp.generated.resources.stat_field
import fuoriorario.composeapp.generated.resources.stat_free
import fuoriorario.composeapp.generated.resources.stat_three
import fuoriorario.composeapp.generated.resources.zone_acd
import fuoriorario.composeapp.generated.resources.zone_acs
import fuoriorario.composeapp.generated.resources.zone_ald
import fuoriorario.composeapp.generated.resources.zone_als
import fuoriorario.composeapp.generated.resources.zone_cen
import fuoriorario.composeapp.generated.resources.zone_mlc
import fuoriorario.composeapp.generated.resources.zone_mld
import fuoriorario.composeapp.generated.resources.zone_mls
import fuoriorario.composeapp.generated.resources.zone_pit
import fuoriorario.composeapp.generated.resources.zone_tl
import it.manu.fuoriorario.core.designsystem.Field
import it.manu.fuoriorario.core.designsystem.GhostButton
import it.manu.fuoriorario.core.designsystem.GhostStyle
import it.manu.fuoriorario.core.designsystem.LocalToast
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.designsystem.PrimaryButton
import it.manu.fuoriorario.core.designsystem.SegmentedControl
import it.manu.fuoriorario.core.designsystem.short
import it.manu.fuoriorario.core.designsystem.show
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.DEFAULT_ZONE_REFS
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Period
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.SessionError
import it.manu.fuoriorario.domain.ShotSession
import it.manu.fuoriorario.domain.Shots
import it.manu.fuoriorario.domain.Stats
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.domain.fieldGoal
import it.manu.fuoriorario.domain.freeThrows
import it.manu.fuoriorario.domain.stats
import it.manu.fuoriorario.domain.trend
import it.manu.fuoriorario.domain.zoneTotals
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

internal val zoneName = mapOf(
    Zone.PIT to Res.string.zone_pit,
    Zone.MLS to Res.string.zone_mls,
    Zone.MLC to Res.string.zone_mlc,
    Zone.MLD to Res.string.zone_mld,
    Zone.ACS to Res.string.zone_acs,
    Zone.ALS to Res.string.zone_als,
    Zone.CEN to Res.string.zone_cen,
    Zone.ALD to Res.string.zone_ald,
    Zone.ACD to Res.string.zone_acd,
    Zone.TL to Res.string.zone_tl
)

/** Zone ids as in the database, for test tags: `made_pit`, `attempted_tl_plus`. */
private val Zone.tag get() = name.lowercase()

/**
 * [player]'s Diario di tiro (PRD F3), theirs or followed by staff: header with "Registra sessione", period and its stats,
 * the period's shot map, then its sessions newest first. [period] is kept by the caller, across players.
 */
@Composable
fun ShotLogScreen(
    player: Member,
    period: Period,
    onPeriod: (Period) -> Unit,
    vm: ShotLogViewModel = koinViewModel { parametersOf(player) }
) {
    val uiState = vm.uiState.collectAsStateWithLifecycle()
    val toast = LocalToast.current
    LaunchedEffect(vm) { vm.toasts.collect { launch { toast.show(it) } } }
    ShotLogStateContent(
        uiState.value.state,
        period,
        ShotLogActions(
            onPeriod = onPeriod,
            onLog = vm::onLog,
            onDismissLog = vm::onDismissLog,
            onDateChanged = vm::onDateChanged,
            onZoneChanged = vm::onZoneChanged,
            onNoteChanged = vm::onNoteChanged,
            onSave = vm::onSave,
            onDelete = vm::onDelete
        )
    )
}

/** What the Diario di tiro does, from the screen down to the sheet. */
class ShotLogActions(
    val onPeriod: (Period) -> Unit = {},
    val onLog: () -> Unit = {},
    val onDismissLog: () -> Unit = {},
    val onDateChanged: (LocalDate) -> Unit = {},
    val onZoneChanged: (Zone, Shots) -> Unit = { _, _ -> },
    val onNoteChanged: (String) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: (ShotSession) -> Unit = {}
)

@Composable
fun ShotLogStateContent(state: UseCaseMutableState<ShotLogScreenState>?, period: Period, actions: ShotLogActions) {
    when (state) {
        is UseCaseMutableState.Error -> state.handler.ErrorScreenContent()
        is UseCaseMutableState.ShowData -> ShotLogContent(state.items, period, actions)
        // Never: the header is there from the start, the sessions fill in.
        UseCaseMutableState.Loading, null -> Unit
    }
}

/** Each "Elimina" asks for a second tap. Nothing below the header until the sessions arrive. */
@Composable
fun ShotLogContent(state: ShotLogScreenState, period: Period, actions: ShotLogActions) {
    val c = FuoriOrarioTheme.colors
    val player = state.player
    val inPeriod = state.sessions?.let { period.filter(it, state.today) }

    Panel {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val role = player.position ?: stringResource(Res.string.role_player)
                Text(
                    (role + player.jerseyNumber?.let { " · #$it" }.orEmpty()).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.muted
                )
                Text(player.displayName.uppercase(), style = MaterialTheme.typography.titleLarge)
            }
            PrimaryButton(stringResource(Res.string.shots_log), actions.onLog)
        }
        val periodLabel = stringResource(Res.string.period)
        SegmentedControl(
            periodName,
            period,
            actions.onPeriod,
            tag = { "period_${it.name.lowercase()}" },
            Modifier.semantics { contentDescription = periodLabel }
        )
        inPeriod?.let { StatsGrid(stats(it)) }
    }

    inPeriod?.let { list -> state.refs?.let { Panel { ShotMap(zoneTotals(list), it) } } }
    inPeriod?.let { Panel { TrendChart(trend(it)) } }
    inPeriod?.let { list ->
        Panel {
            Text(stringResource(Res.string.shots_title), style = MaterialTheme.typography.titleMedium)
            if (list.isEmpty()) {
                Text(stringResource(Res.string.shots_empty), color = c.muted)
            } else {
                Column {
                    list.forEachIndexed { i, session ->
                        if (i > 0) HorizontalDivider(color = c.line)
                        SessionRow(session, state.confirmingDelete == session) { actions.onDelete(session) }
                    }
                }
            }
        }
    }

    state.draft?.let { LogSheet(player.displayName, it, state.today, state.busy, actions) }
}

@Preview
@Composable
private fun ShotLogContentPreview() = FuoriOrarioTheme {
    val today = LocalDate(2026, 10, 8)
    Column {
        ShotLogContent(
            ShotLogScreenState(
                Member("Luca B.", Role.PLAYER, jerseyNumber = "7", position = "Guardia"),
                today,
                sessions = listOf(
                    ShotSession(today, mapOf(Zone.PIT to Shots(6, 10), Zone.TL to Shots(7, 10)), "Gambe stanche"),
                    ShotSession(LocalDate(2026, 10, 5), mapOf(Zone.CEN to Shots(3, 8)))
                ),
                refs = DEFAULT_ZONE_REFS
            ),
            Period.DAYS_30,
            ShotLogActions()
        )
    }
}

private val periodName = mapOf(
    Period.DAYS_7 to Res.string.period_7,
    Period.DAYS_30 to Res.string.period_30,
    Period.SEASON to Res.string.period_season
)

/** Prototype `.stats`: 4 columns, 2 when narrow. Percentages show "—" without attempts. */
@Composable
private fun StatsGrid(stats: Stats) {
    val c = FuoriOrarioTheme.colors
    val small = SpanStyle(fontSize = 15.sp, color = c.muted, fontWeight = FontWeight.SemiBold)
    fun percent(shots: Shots) = buildAnnotatedString {
        val p = shots.percent
        if (p == null) {
            withStyle(small) { append("—") }
        } else {
            append("$p")
            withStyle(small) { append("%") }
        }
    }
    val items = listOf(
        Triple("attempted", Res.string.stat_attempted, AnnotatedString("${stats.attempted}")),
        Triple("field", Res.string.stat_field, percent(stats.fieldGoal)),
        Triple("three", Res.string.stat_three, percent(stats.three)),
        Triple("free", Res.string.stat_free, percent(stats.freeThrows))
    )
    // The prototype goes to 2 columns under a 400px viewport: about 340dp inside the panel.
    BoxWithConstraints {
        val columns = if (maxWidth < 340.dp) 2 else 4
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (tag, label, value) ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                stringResource(label).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = c.muted
                            )
                            Text(
                                value,
                                Modifier.testTag("stat_$tag"),
                                style = MaterialTheme.typography.headlineMedium,
                                fontSize = 30.sp,
                                lineHeight = 30.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Prototype `sessionRow`: date, dal campo m/a · %, TL m/a, note, and "Elimina" → "Conferma". */
@Composable
private fun SessionRow(session: ShotSession, confirming: Boolean, onDelete: () -> Unit) {
    val c = FuoriOrarioTheme.colors
    val number = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(session.date.short(), Modifier.width(58.dp), style = number, fontSize = 16.sp)
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val field = session.fieldGoal
                Text(
                    field.percent?.let { "${field.made}/${field.attempted} · $it%" } ?: "—",
                    Modifier.alignByBaseline(),
                    style = number
                )
                val ft = session.freeThrows
                if (ft.attempted > 0) {
                    Text("· TL ${ft.made}/${ft.attempted}", Modifier.alignByBaseline(), color = c.muted)
                }
            }
            session.note?.let {
                Text(it, color = c.muted, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp)
            }
        }
        GhostButton(
            stringResource(if (confirming) Res.string.shots_delete_confirm else Res.string.shots_delete),
            onDelete,
            Modifier.testTag("delete_${session.id}"),
            if (confirming) GhostStyle.DANGER else GhostStyle.PLAIN
        )
    }
}

/**
 * Prototype `openLog`: date (today by default, never in the future), a stepper pair per zone, optional note.
 * The ViewModel saves, so a failure leaves the sheet open as typed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogSheet(name: String, draft: SessionDraft, today: LocalDate, busy: Boolean, actions: ShotLogActions) {
    val c = FuoriOrarioTheme.colors
    ModalBottomSheet(
        actions.onDismissLog,
        containerColor = c.surface,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        dragHandle = null
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp, 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.session_new).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.muted
                    )
                    Text(name.uppercase(), style = MaterialTheme.typography.titleLarge)
                }
                GhostButton(stringResource(Res.string.close), actions.onDismissLog)
            }
            DateField(draft.date, today, actions.onDateChanged)
            Text(stringResource(Res.string.session_hint), color = c.muted, style = MaterialTheme.typography.bodyMedium)
            Column {
                Zone.entries.forEach { zone ->
                    HorizontalDivider(color = c.line)
                    ZoneRow(zone, draft.zones.getValue(zone)) { actions.onZoneChanged(zone, it) }
                }
            }
            Field(
                label = stringResource(Res.string.session_note),
                value = draft.note,
                onValueChange = actions.onNoteChanged,
                tag = "session_note",
                singleLine = false
            )
            draft.error?.let {
                val text = when (it) {
                    is SessionError.MadeOverAttempted ->
                        stringResource(Res.string.session_error_made, stringResource(zoneName.getValue(it.zone)))
                    SessionError.NoAttempts -> stringResource(Res.string.session_error_empty)
                }
                Text(text, color = c.accent, style = MaterialTheme.typography.bodyMedium)
            }
            PrimaryButton(
                stringResource(Res.string.session_save),
                actions.onSave,
                Modifier.fillMaxWidth(),
                enabled = !busy
            )
        }
    }
}

/** Prototype `input type=date` with `max` = today: a field opening the date picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(value: LocalDate, today: LocalDate, onChange: (LocalDate) -> Unit) {
    val c = FuoriOrarioTheme.colors
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            stringResource(Res.string.session_date).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = c.muted
        )
        Text(
            "${value.short()} ${value.year}",
            Modifier
                .fillMaxWidth()
                .background(c.surface2, RoundedCornerShape(9.dp))
                .border(1.dp, c.line, RoundedCornerShape(9.dp))
                .clickable(role = SemanticsRole.Button) { open = true }
                .padding(horizontal = 11.dp, vertical = 9.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = c.ink
        )
    }
    if (!open) return
    // The picker works in UTC midnights.
    val todayMillis = today.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = value.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis

            override fun isSelectableYear(year: Int) = year <= today.year
        }
    )
    DatePickerDialog(
        onDismissRequest = { open = false },
        confirmButton = {
            TextButton({
                state.selectedDateMillis?.let {
                    onChange(Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date)
                }
                open = false
            }) { Text(stringResource(Res.string.ok), color = c.accent) }
        },
        dismissButton = { TextButton({ open = false }) { Text(stringResource(Res.string.cancel), color = c.muted) } }
    ) { DatePicker(state) }
}

/** Prototype `.zrow`: zone name, then made and attempted steppers. */
@Composable
private fun ZoneRow(zone: Zone, shots: Shots, onChange: (Shots) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val name = stringResource(zoneName.getValue(zone))
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Stepper(
            "made_${zone.tag}",
            stringResource(Res.string.session_made),
            shots.made,
            onType = { onChange(shots.withMade(it)) },
            onStep = { onChange(shots.stepMade(it)) }
        )
        Stepper(
            "attempted_${zone.tag}",
            stringResource(Res.string.session_attempted),
            shots.attempted,
            onType = { onChange(shots.copy(attempted = it)) },
            onStep = { onChange(shots.stepAttempted(it)) }
        )
    }
}

/** Prototype `.step`: − [number over caption] +. [onStep] gets true for +. */
@Composable
private fun Stepper(tag: String, caption: String, value: Int, onType: (Int) -> Unit, onStep: (Boolean) -> Unit) {
    val c = FuoriOrarioTheme.colors
    val what = caption.lowercase()

    @Composable
    fun StepButton(up: Boolean) {
        val label = stringResource(if (up) Res.string.session_more else Res.string.session_less, what)
        Box(
            Modifier
                .size(34.dp)
                .testTag("${tag}_${if (up) "plus" else "minus"}")
                .background(c.surface2, RoundedCornerShape(8.dp))
                .border(1.dp, c.line, RoundedCornerShape(8.dp))
                .clickable(role = SemanticsRole.Button) { onStep(up) }
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center
        ) { Text(if (up) "+" else "−", fontSize = 18.sp, color = c.ink) }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        StepButton(up = false)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicTextField(
                value = value.toString(),
                onValueChange = { onType(it.filter(Char::isDigit).take(4).toIntOrNull() ?: 0) },
                modifier = Modifier
                    .width(46.dp)
                    .testTag(tag)
                    .background(c.surface, RoundedCornerShape(8.dp))
                    .border(1.dp, c.line, RoundedCornerShape(8.dp))
                    .padding(horizontal = 2.dp, vertical = 6.dp),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = c.ink,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(c.accent),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Text(caption.uppercase(), style = MaterialTheme.typography.labelSmall, fontSize = 10.5.sp, color = c.muted)
        }
        StepButton(up = true)
    }
}
