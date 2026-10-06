package it.manu.fuoriorario.ui.shots

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.cancel
import fuoriorario.composeapp.generated.resources.close
import fuoriorario.composeapp.generated.resources.load_failed
import fuoriorario.composeapp.generated.resources.months_short
import fuoriorario.composeapp.generated.resources.ok
import fuoriorario.composeapp.generated.resources.period
import fuoriorario.composeapp.generated.resources.period_30
import fuoriorario.composeapp.generated.resources.period_7
import fuoriorario.composeapp.generated.resources.period_season
import fuoriorario.composeapp.generated.resources.retry
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
import fuoriorario.composeapp.generated.resources.session_saved
import fuoriorario.composeapp.generated.resources.shots_delete
import fuoriorario.composeapp.generated.resources.shots_delete_confirm
import fuoriorario.composeapp.generated.resources.shots_deleted
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
import it.manu.fuoriorario.core.today
import it.manu.fuoriorario.data.ShotRepository
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.NOTE_MAX
import it.manu.fuoriorario.domain.Period
import it.manu.fuoriorario.domain.SessionError
import it.manu.fuoriorario.domain.ShotSession
import it.manu.fuoriorario.domain.Shots
import it.manu.fuoriorario.domain.Stats
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.domain.fieldGoal
import it.manu.fuoriorario.domain.freeThrows
import it.manu.fuoriorario.domain.newSession
import it.manu.fuoriorario.domain.sessionError
import it.manu.fuoriorario.domain.stats
import it.manu.fuoriorario.ui.components.Field
import it.manu.fuoriorario.ui.components.GhostButton
import it.manu.fuoriorario.ui.components.GhostStyle
import it.manu.fuoriorario.ui.components.LocalToast
import it.manu.fuoriorario.ui.components.Panel
import it.manu.fuoriorario.ui.components.PrimaryButton
import it.manu.fuoriorario.ui.components.SegmentedControl
import it.manu.fuoriorario.ui.components.launchWrite
import it.manu.fuoriorario.ui.components.show
import it.manu.fuoriorario.ui.theme.FuoriOrarioTheme
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

private val zoneName = mapOf(
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

/** Prototype `fmtShort`: "6 ott". On web the months load asynchronously: empty until then. */
@Composable
private fun LocalDate.short() = "$day ${stringArrayResource(Res.array.months_short).getOrElse(month.ordinal) { "" }}"

/**
 * The player's Diario di tiro (PRD F3): header with "Registra sessione", period and its stats,
 * then the period's sessions newest first. Each "Elimina" asks for a second tap.
 */
@Composable
fun ShotLogScreen(shots: ShotRepository, me: Member) {
    val c = FuoriOrarioTheme.colors
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    var sessions by remember { mutableStateOf<List<ShotSession>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var logging by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf<ShotSession?>(null) }
    var period by remember { mutableStateOf(Period.DAYS_30) }
    val today = remember { today() }
    val inPeriod = sessions?.let { period.filter(it, today) }

    /** A save or delete is in flight. */
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(attempt) {
        loadFailed = false
        try {
            sessions = shots.sessions()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            loadFailed = true
        }
    }

    fun save(session: ShotSession) {
        if (busy) return
        busy = true
        scope.launchWrite(toast, onDone = { busy = false }) {
            val saved = shots.add(session)
            // Stable sort: the new one goes first among sessions of the same day.
            sessions = (listOf(saved) + sessions.orEmpty()).sortedByDescending { it.date }
            logging = false
            launch { toast.show(getString(Res.string.session_saved)) }
        }
    }

    fun delete(session: ShotSession) {
        if (busy) return
        if (confirmingDelete != session) {
            confirmingDelete = session
            return
        }
        confirmingDelete = null
        busy = true
        scope.launchWrite(toast, onDone = { busy = false }) {
            shots.delete(session)
            sessions = sessions.orEmpty().filter { it.id != session.id }
            launch { toast.show(getString(Res.string.shots_deleted)) }
        }
    }

    Panel {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val role = me.position ?: stringResource(Res.string.role_player)
                Text(
                    (role + me.jerseyNumber?.let { " · #$it" }.orEmpty()).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.muted
                )
                Text(me.displayName.uppercase(), style = MaterialTheme.typography.titleLarge)
            }
            PrimaryButton(stringResource(Res.string.shots_log), {
                confirmingDelete = null
                logging = true
            })
        }
        val periodLabel = stringResource(Res.string.period)
        SegmentedControl(
            periodName,
            period,
            { period = it },
            tag = { "period_${it.name.lowercase()}" },
            Modifier.semantics { contentDescription = periodLabel }
        )
        inPeriod?.let { StatsGrid(stats(it)) }
    }

    if (loadFailed) {
        Panel(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(Res.string.load_failed), color = c.muted)
            GhostButton(stringResource(Res.string.retry), { attempt++ })
        }
    }
    inPeriod?.let { list ->
        Panel {
            Text(stringResource(Res.string.shots_title), style = MaterialTheme.typography.titleMedium)
            if (list.isEmpty()) {
                Text(stringResource(Res.string.shots_empty), color = c.muted)
            } else {
                Column {
                    list.forEachIndexed { i, session ->
                        if (i > 0) HorizontalDivider(color = c.line)
                        SessionRow(session, confirmingDelete == session) { delete(session) }
                    }
                }
            }
        }
    }

    if (logging) LogSheet(me.displayName, busy, onDismiss = { logging = false }, onSave = ::save)
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
 * The screen saves, so a failure leaves the sheet open as typed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogSheet(name: String, busy: Boolean, onDismiss: () -> Unit, onSave: (ShotSession) -> Unit) {
    val c = FuoriOrarioTheme.colors
    val today = remember { today() }
    var date by remember { mutableStateOf(today) }
    var zones by remember { mutableStateOf(Zone.entries.associateWith { Shots() }) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<SessionError?>(null) }

    fun save() {
        error = sessionError(zones)
        if (error == null) onSave(newSession(date, zones, note))
    }

    ModalBottomSheet(
        onDismiss,
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
                GhostButton(stringResource(Res.string.close), onDismiss)
            }
            DateField(date, today) { date = it }
            Text(stringResource(Res.string.session_hint), color = c.muted, style = MaterialTheme.typography.bodyMedium)
            Column {
                Zone.entries.forEach { zone ->
                    HorizontalDivider(color = c.line)
                    ZoneRow(zone, zones.getValue(zone)) { zones = zones + (zone to it) }
                }
            }
            Field(
                label = stringResource(Res.string.session_note),
                value = note,
                onValueChange = { note = it.take(NOTE_MAX) },
                tag = "session_note",
                singleLine = false
            )
            error?.let {
                val text = when (it) {
                    is SessionError.MadeOverAttempted ->
                        stringResource(Res.string.session_error_made, stringResource(zoneName.getValue(it.zone)))
                    SessionError.NoAttempts -> stringResource(Res.string.session_error_empty)
                }
                Text(text, color = c.accent, style = MaterialTheme.typography.bodyMedium)
            }
            PrimaryButton(stringResource(Res.string.session_save), ::save, Modifier.fillMaxWidth(), enabled = !busy)
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
