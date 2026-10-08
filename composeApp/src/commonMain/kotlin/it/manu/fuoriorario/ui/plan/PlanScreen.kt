package it.manu.fuoriorario.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.days_short
import fuoriorario.composeapp.generated.resources.exercise_added
import fuoriorario.composeapp.generated.resources.exercise_days
import fuoriorario.composeapp.generated.resources.exercise_edit
import fuoriorario.composeapp.generated.resources.exercise_library
import fuoriorario.composeapp.generated.resources.exercise_library_failed
import fuoriorario.composeapp.generated.resources.exercise_new
import fuoriorario.composeapp.generated.resources.exercise_remove
import fuoriorario.composeapp.generated.resources.exercise_remove_confirm
import fuoriorario.composeapp.generated.resources.exercise_removed
import fuoriorario.composeapp.generated.resources.exercise_save
import fuoriorario.composeapp.generated.resources.exercise_update
import fuoriorario.composeapp.generated.resources.exercise_updated
import fuoriorario.composeapp.generated.resources.ic_check
import fuoriorario.composeapp.generated.resources.note_edit
import fuoriorario.composeapp.generated.resources.note_label
import fuoriorario.composeapp.generated.resources.note_save
import fuoriorario.composeapp.generated.resources.note_saved
import fuoriorario.composeapp.generated.resources.note_text
import fuoriorario.composeapp.generated.resources.note_write
import fuoriorario.composeapp.generated.resources.plan_add
import fuoriorario.composeapp.generated.resources.plan_back
import fuoriorario.composeapp.generated.resources.plan_copied
import fuoriorario.composeapp.generated.resources.plan_copy
import fuoriorario.composeapp.generated.resources.plan_copy_confirm
import fuoriorario.composeapp.generated.resources.plan_copy_empty
import fuoriorario.composeapp.generated.resources.plan_day_done
import fuoriorario.composeapp.generated.resources.plan_day_rest
import fuoriorario.composeapp.generated.resources.plan_day_todo
import fuoriorario.composeapp.generated.resources.plan_done
import fuoriorario.composeapp.generated.resources.plan_edit
import fuoriorario.composeapp.generated.resources.plan_empty_player
import fuoriorario.composeapp.generated.resources.plan_empty_staff
import fuoriorario.composeapp.generated.resources.plan_items
import fuoriorario.composeapp.generated.resources.plan_label
import fuoriorario.composeapp.generated.resources.plan_next
import fuoriorario.composeapp.generated.resources.plan_prev
import fuoriorario.composeapp.generated.resources.plan_this_week
import fuoriorario.composeapp.generated.resources.plan_video
import it.manu.fuoriorario.core.designsystem.CategoryChip
import it.manu.fuoriorario.core.designsystem.ExerciseErrorText
import it.manu.fuoriorario.core.designsystem.ExerciseFields
import it.manu.fuoriorario.core.designsystem.Field
import it.manu.fuoriorario.core.designsystem.GhostButton
import it.manu.fuoriorario.core.designsystem.GhostStyle
import it.manu.fuoriorario.core.designsystem.LoadFailed
import it.manu.fuoriorario.core.designsystem.LocalToast
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.designsystem.PrimaryButton
import it.manu.fuoriorario.core.designsystem.Sheet
import it.manu.fuoriorario.core.designsystem.launchWrite
import it.manu.fuoriorario.core.designsystem.short
import it.manu.fuoriorario.core.designsystem.show
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.core.today
import it.manu.fuoriorario.data.PlanChangedException
import it.manu.fuoriorario.data.PlanRepository
import it.manu.fuoriorario.domain.Category
import it.manu.fuoriorario.domain.LibraryExercise
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanCheck
import it.manu.fuoriorario.domain.PlanItem
import it.manu.fuoriorario.domain.PlanItemError
import it.manu.fuoriorario.domain.Progress
import it.manu.fuoriorario.domain.WEEKLY_NOTE_MAX
import it.manu.fuoriorario.domain.checkOn
import it.manu.fuoriorario.domain.dayIndex
import it.manu.fuoriorario.domain.newPlanItem
import it.manu.fuoriorario.domain.planItemError
import it.manu.fuoriorario.domain.progress
import it.manu.fuoriorario.domain.weekOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

/** "Lun"… "Dom", 0 = Monday. On web the names load asynchronously: empty until then. */
@Composable
private fun dayNames() = stringArrayResource(Res.array.days_short).let { names ->
    List(7) { names.getOrElse(it) { "" } }
}

/**
 * [player]'s Piano (PRD F4), theirs or followed by staff: [week] with ‹ › navigation, then its exercises with the
 * assigned days and the week's completion. [week] (a Monday) is kept by the caller, across players. [staff] add
 * exercises, the player checks the days done.
 */
@Composable
fun PlanScreen(plans: PlanRepository, player: Member, staff: Boolean, week: LocalDate, onWeek: (LocalDate) -> Unit) {
    val c = FuoriOrarioTheme.colors
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<PlanItem>?>(null) }
    var checks by remember { mutableStateOf(emptySet<PlanCheck>()) }
    var loadFailed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PlanItem?>(null) }
    var writingNote by remember { mutableStateOf(false) }
    var confirmingCopy by remember(week) { mutableStateOf(false) }
    var library by remember { mutableStateOf(emptyList<LibraryExercise>()) }
    val thisWeek = remember { weekOf(today()) }
    val todayIndex = remember { dayIndex(today()) }

    /** A save or a check is in flight. */
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(week, attempt) {
        items = null
        loadFailed = false
        try {
            val loaded = plans.items(player, week)
            checks = plans.checks(loaded)
            note = plans.note(player, week)
            items = loaded
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            loadFailed = true
        }
    }

    // On the first "Aggiungi esercizio", again on the next if it failed: meanwhile the exercise is typed freely.
    LaunchedEffect(adding) {
        if (!adding || library.isNotEmpty()) return@LaunchedEffect
        try {
            library = plans.library()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            launch { toast.show(getString(Res.string.exercise_library_failed)) }
        }
    }

    /** A new exercise, or the edited one when it has an id. */
    fun save(item: PlanItem) {
        if (busy) return
        busy = true
        scope.launchWrite(toast, onDone = { busy = false }) {
            if (item.id == null) {
                items = items.orEmpty() + plans.add(item.copy(memberId = player.id))
                adding = false
                launch { toast.show(getString(Res.string.exercise_added)) }
            } else {
                plans.update(item)
                items = items?.map { if (it.id == item.id) item else it }
                editing = null
                launch { toast.show(getString(Res.string.exercise_updated)) }
            }
        }
    }

    fun remove(item: PlanItem) {
        if (busy) return
        busy = true
        scope.launchWrite(toast, onDone = { busy = false }) {
            plans.remove(item)
            items = items?.filter { it.id != item.id }
            checks = checks.filter { it.planItemId != item.id }.toSet()
            editing = null
            launch { toast.show(getString(Res.string.exercise_removed)) }
        }
    }

    /** Once the week is loaded; into one with exercises only on a second tap, and only if there is something to copy. */
    fun copyPreviousWeek() {
        val current = items
        if (busy || current == null) return
        busy = true
        scope.launchWrite(toast, onDone = { busy = false }) {
            if (current.isNotEmpty() && !confirmingCopy) {
                // Asks only when there is something to copy.
                if (plans.items(player, week.minus(DatePeriod(days = 7))).isEmpty()) {
                    launch { toast.show(getString(Res.string.plan_copy_empty)) }
                } else {
                    confirmingCopy = true
                }
                return@launchWrite
            }
            // Success or not, the next copy asks again.
            confirmingCopy = false
            val copies = try {
                plans.copyPreviousWeek(player, week, current.size)
            } catch (e: PlanChangedException) {
                attempt++
                throw e
            }
            items = current + copies
            launch {
                toast.show(getString(if (copies.isEmpty()) Res.string.plan_copy_empty else Res.string.plan_copied))
            }
        }
    }

    /** Trimmed; emptied, the note goes. */
    fun saveNote(text: String) {
        if (busy) return
        val new = text.trim().ifEmpty { null }
        if (new == note) {
            writingNote = false
            return
        }
        busy = true
        scope.launchWrite(toast, onDone = { busy = false }) {
            plans.saveNote(player, week, new)
            note = new
            writingNote = false
            launch { toast.show(getString(Res.string.note_saved)) }
        }
    }

    /** Only once saved: a failure leaves the dot as it was. */
    fun toggle(check: PlanCheck) {
        if (busy) return
        busy = true
        scope.launchWrite(toast, onDone = { busy = false }) {
            if (check in checks) {
                plans.uncheck(check)
                checks = checks - check
            } else {
                plans.check(check)
                checks = checks + check
            }
        }
    }

    Panel {
        Column {
            Text(
                stringResource(Res.string.plan_label).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = c.muted
            )
            Text(player.displayName.uppercase(), style = MaterialTheme.typography.titleLarge)
        }
        WeekNav(week, thisWeek, onWeek)
        items?.let { Completion(progress(it, checks)) }
        note?.let { StaffNote(it) }
        if (staff) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryButton(stringResource(Res.string.plan_add), { adding = true })
                GhostButton(
                    stringResource(if (note == null) Res.string.note_write else Res.string.note_edit),
                    { writingNote = true }
                )
                GhostButton(
                    stringResource(
                        if (confirmingCopy &&
                            !items.isNullOrEmpty()
                        ) {
                            Res.string.plan_copy_confirm
                        } else {
                            Res.string.plan_copy
                        }
                    ),
                    ::copyPreviousWeek
                )
            }
        }
    }

    if (loadFailed) LoadFailed { attempt++ }
    items?.let { list ->
        Panel {
            Text(stringResource(Res.string.plan_items), style = MaterialTheme.typography.titleMedium)
            if (list.isEmpty()) {
                val empty = if (staff) Res.string.plan_empty_staff else Res.string.plan_empty_player
                Text(stringResource(empty), color = c.muted)
            } else {
                Column {
                    list.forEachIndexed { i, item ->
                        if (i > 0) HorizontalDivider(color = c.line)
                        ItemRow(
                            item,
                            checks,
                            todayIndex.takeIf { week == thisWeek },
                            // Only the player checks.
                            onToggle = if (staff) null else ::toggle,
                            onEdit = if (staff) ({ editing = item }) else null,
                            Modifier.padding(top = if (i > 0) 14.dp else 4.dp, bottom = 14.dp)
                        )
                    }
                }
            }
        }
    }

    if (adding || editing != null) {
        ExerciseSheet(
            player.displayName,
            week,
            editing,
            library,
            busy,
            onDismiss = {
                adding = false
                editing = null
            },
            onSave = ::save,
            onRemove = ::remove
        )
    }
    if (writingNote) NoteSheet(player.displayName, note, busy, onDismiss = { writingNote = false }, onSave = ::saveNote)
}

/** Prototype `.weeknav`: ‹ "5 ott – 11 ott" ›, with "Questa settimana" under the current one or a way back to it. */
@Composable
private fun WeekNav(week: LocalDate, thisWeek: LocalDate, onWeek: (LocalDate) -> Unit) {
    val c = FuoriOrarioTheme.colors

    @Composable
    fun Arrow(text: String, label: String, weeks: Int) {
        Box(
            Modifier
                .size(40.dp)
                .background(c.surface, RoundedCornerShape(10.dp))
                .border(1.dp, c.line, RoundedCornerShape(10.dp))
                .clickable(role = SemanticsRole.Button) { onWeek(week.plus(DatePeriod(days = 7 * weeks))) }
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center
        ) { Text(text, fontSize = 18.sp, color = c.ink) }
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Arrow("‹", stringResource(Res.string.plan_prev), -1)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${week.short()} – ${week.plus(DatePeriod(days = 6)).short()}",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            if (week == thisWeek) {
                Text(
                    stringResource(Res.string.plan_this_week).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.muted
                )
            } else {
                Text(
                    stringResource(Res.string.plan_back),
                    Modifier.clickable(role = SemanticsRole.Button) { onWeek(thisWeek) },
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.accent
                )
            }
        }
        Arrow("›", stringResource(Res.string.plan_next), 1)
    }
}

/** Prototype `.coach-note`: the staff note under its label, with an accent bar on the left. */
@Composable
private fun StaffNote(note: String) {
    val c = FuoriOrarioTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            stringResource(Res.string.note_label).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = c.muted
        )
        Text(
            note,
            Modifier
                .drawBehind { drawRect(c.accent, size = Size(3.dp.toPx(), size.height)) }
                .padding(start = 15.dp, top = 4.dp, bottom = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.5.sp
        )
    }
}

/** Prototype `.ex`: area chip, title (with "Modifica" for staff), volume, description, video link and day dots. */
@Composable
private fun ItemRow(
    item: PlanItem,
    checks: Set<PlanCheck>,
    today: Int?,
    onToggle: ((PlanCheck) -> Unit)?,
    onEdit: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val c = FuoriOrarioTheme.colors
    val uris = LocalUriHandler.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CategoryChip(item.category)
                Text(item.title, style = MaterialTheme.typography.titleMedium)
            }
            onEdit?.let {
                GhostButton(stringResource(Res.string.plan_edit), it, Modifier.testTag("plan_edit_${item.id}"))
            }
        }
        item.volume?.let {
            Text(
                it,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.accent
            )
        }
        item.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = c.muted) }
        item.videoUrl?.let { url ->
            Text(
                stringResource(Res.string.plan_video),
                Modifier.clickable(role = SemanticsRole.Button) {
                    // No app for the link: nothing to open, but no crash.
                    runCatching { uris.openUri(url) }
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = c.accent
            )
        }
        Days(item, checks, today, onToggle)
    }
}

/** Prototype `.bar` under "Completati x/y". */
@Composable
private fun Completion(progress: Progress) {
    val c = FuoriOrarioTheme.colors
    val fraction = if (progress.total == 0) 0f else progress.done.toFloat() / progress.total
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(Res.string.plan_done).uppercase(),
            Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = c.muted
        )
        Text(
            "${progress.done}/${progress.total}",
            style = MaterialTheme.typography.titleMedium,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .progressSemantics(fraction)
            .background(c.surface2, RoundedCornerShape(50))
            .border(1.dp, c.line, RoundedCornerShape(50))
    ) {
        Box(Modifier.fillMaxWidth(fraction).height(10.dp).background(c.accent, RoundedCornerShape(50)))
    }
}

/**
 * Prototype `.days`: a column per weekday, the dot ringed in accent when assigned, filled with ✓ when done, dashed
 * and faded on rest days. [today] (0 = Monday) is highlighted. Assigned days toggle with [onToggle], if given.
 */
@Composable
private fun Days(item: PlanItem, checks: Set<PlanCheck>, today: Int?, onToggle: ((PlanCheck) -> Unit)?) {
    val c = FuoriOrarioTheme.colors
    val names = dayNames()
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        names.forEachIndexed { i, name ->
            val on = i in item.days
            val check = item.checkOn(i)
            val done = on && check in checks
            val isToday = i == today
            val label = stringResource(
                when {
                    done -> Res.string.plan_day_done
                    on -> Res.string.plan_day_todo
                    else -> Res.string.plan_day_rest
                },
                name
            )
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    name.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = if (isToday) c.ink else c.muted
                )
                Box(
                    Modifier
                        .widthIn(max = 40.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .then(
                            if (on && onToggle != null) {
                                Modifier.toggleable(done, role = SemanticsRole.Checkbox) { onToggle(check) }
                            } else {
                                Modifier
                            }
                        )
                        .semantics { contentDescription = label }
                        .drawBehind {
                            val stroke = 2.dp.toPx()
                            val center = Offset(size.width / 2, size.height / 2)
                            val radius = (size.minDimension - stroke) / 2
                            // A 3dp halo just outside the ring.
                            if (isToday && on && !done) {
                                val halo = 3.dp.toPx()
                                drawCircle(c.accentSoft, radius + (stroke + halo) / 2, center, style = Stroke(halo))
                            }
                            if (done) drawCircle(c.accent, radius + stroke / 2, center)
                            drawCircle(
                                if (on) c.accent else c.line.copy(alpha = 0.35f),
                                radius = radius,
                                center = center,
                                style = Stroke(
                                    stroke,
                                    pathEffect = if (on) {
                                        null
                                    } else {
                                        PathEffect.dashPathEffect(
                                            floatArrayOf(4.dp.toPx(), 3.dp.toPx())
                                        )
                                    }
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (done) {
                        Icon(
                            painterResource(Res.drawable.ic_check),
                            null,
                            Modifier.size(16.dp),
                            tint = c.accentInk
                        )
                    }
                }
            }
        }
    }
}

/**
 * Prototype `openEx`: title, area, volume, description, video and days, empty for a new exercise (Mon, Wed, Fri by
 * default) or filled from [item] to edit it, which can also be removed with a second tap. A new one can start from an
 * exercise of the [library], copied into the form. The screen saves, so a failure leaves the sheet open as typed.
 */
@Composable
private fun ExerciseSheet(
    name: String,
    week: LocalDate,
    item: PlanItem?,
    library: List<LibraryExercise>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (PlanItem) -> Unit,
    onRemove: (PlanItem) -> Unit
) {
    var draft by remember {
        mutableStateOf(
            item?.run { LibraryExercise(title, category, volume, description, videoUrl) }
                ?: LibraryExercise("", Category.entries.first())
        )
    }
    var days by remember { mutableStateOf(item?.days?.toSet() ?: setOf(0, 2, 4)) }
    var error by remember { mutableStateOf<PlanItemError?>(null) }
    var confirmingRemoval by remember { mutableStateOf(false) }

    fun save() {
        error = planItemError(draft.title, days, draft.videoUrl.orEmpty())
        if (error != null) return
        val new = draft.run {
            newPlanItem(week, title, category, volume.orEmpty(), description.orEmpty(), videoUrl.orEmpty(), days)
        }
        onSave(item?.let { new.copy(id = it.id, memberId = it.memberId) } ?: new)
    }

    Sheet(stringResource(if (item == null) Res.string.exercise_new else Res.string.exercise_edit), name, onDismiss) {
        if (item == null && library.isNotEmpty()) LibraryPicker(library) { draft = it }
        ExerciseFields(draft) { draft = it }
        DayPicker(days) { days = it }
        ExerciseErrorText(error)
        if (item == null) {
            PrimaryButton(stringResource(Res.string.exercise_save), ::save, Modifier.fillMaxWidth(), enabled = !busy)
        } else {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                PrimaryButton(stringResource(Res.string.exercise_update), ::save, enabled = !busy)
                GhostButton(
                    stringResource(
                        if (confirmingRemoval) Res.string.exercise_remove_confirm else Res.string.exercise_remove
                    ),
                    { if (confirmingRemoval) onRemove(item) else confirmingRemoval = true },
                    style = GhostStyle.DANGER
                )
            }
        }
    }
}

/** Prototype `.lib`: a pill per library exercise. */
@Composable
private fun LibraryPicker(library: List<LibraryExercise>, onPick: (LibraryExercise) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            stringResource(Res.string.exercise_library).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = FuoriOrarioTheme.colors.muted
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            library.forEach { GhostButton(it.title, { onPick(it) }, style = GhostStyle.PILL) }
        }
    }
}

/** Prototype `openNote`: the staff note for the week, up to [WEEKLY_NOTE_MAX] characters; emptied, the screen removes it. */
@Composable
private fun NoteSheet(name: String, note: String?, busy: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(note.orEmpty()) }
    Sheet(stringResource(Res.string.note_label), name, onDismiss) {
        Field(stringResource(Res.string.note_text), text, {
            text = it.take(WEEKLY_NOTE_MAX)
        }, "note", singleLine = false)
        PrimaryButton(stringResource(Res.string.note_save), { onSave(text) }, Modifier.fillMaxWidth(), enabled = !busy)
    }
}

/** Prototype `.daypick`: a checkbox per weekday. */
@Composable
private fun DayPicker(days: Set<Int>, onChange: (Set<Int>) -> Unit) {
    val c = FuoriOrarioTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            stringResource(Res.string.exercise_days).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = c.muted
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            dayNames().forEachIndexed { i, name ->
                val on = i in days
                Row(
                    Modifier
                        .testTag("exercise_day_$i")
                        .border(1.dp, c.line, RoundedCornerShape(8.dp))
                        .toggleable(on, role = SemanticsRole.Checkbox) { onChange(if (it) days + i else days - i) }
                        .padding(start = 2.dp, end = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        on,
                        null,
                        Modifier.size(32.dp),
                        colors = CheckboxDefaults.colors(
                            checkedColor = c.accent,
                            checkmarkColor = c.accentInk,
                            uncheckedColor = c.muted
                        )
                    )
                    Text(name, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp)
                }
            }
        }
    }
}
