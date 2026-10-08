package it.manu.fuoriorario.feature.plan

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.days_short
import fuoriorario.composeapp.generated.resources.exercise_days
import fuoriorario.composeapp.generated.resources.exercise_edit
import fuoriorario.composeapp.generated.resources.exercise_library
import fuoriorario.composeapp.generated.resources.exercise_new
import fuoriorario.composeapp.generated.resources.exercise_remove
import fuoriorario.composeapp.generated.resources.exercise_remove_confirm
import fuoriorario.composeapp.generated.resources.exercise_save
import fuoriorario.composeapp.generated.resources.exercise_update
import fuoriorario.composeapp.generated.resources.ic_check
import fuoriorario.composeapp.generated.resources.note_edit
import fuoriorario.composeapp.generated.resources.note_label
import fuoriorario.composeapp.generated.resources.note_save
import fuoriorario.composeapp.generated.resources.note_text
import fuoriorario.composeapp.generated.resources.note_write
import fuoriorario.composeapp.generated.resources.plan_add
import fuoriorario.composeapp.generated.resources.plan_back
import fuoriorario.composeapp.generated.resources.plan_copy
import fuoriorario.composeapp.generated.resources.plan_copy_confirm
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
import it.manu.fuoriorario.core.designsystem.Loader
import it.manu.fuoriorario.core.designsystem.LocalToast
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.designsystem.PrimaryButton
import it.manu.fuoriorario.core.designsystem.Sheet
import it.manu.fuoriorario.core.designsystem.short
import it.manu.fuoriorario.core.designsystem.show
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.Category
import it.manu.fuoriorario.domain.LibraryExercise
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanCheck
import it.manu.fuoriorario.domain.PlanItem
import it.manu.fuoriorario.domain.Progress
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.WEEKLY_NOTE_MAX
import it.manu.fuoriorario.domain.checkOn
import it.manu.fuoriorario.domain.dayIndex
import it.manu.fuoriorario.domain.progress
import it.manu.fuoriorario.domain.weekOf
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

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
fun PlanScreen(
    player: Member,
    staff: Boolean,
    week: LocalDate,
    onWeek: (LocalDate) -> Unit,
    vm: PlanViewModel = koinViewModel { parametersOf(player, week) }
) {
    val uiState = vm.uiState.collectAsStateWithLifecycle()
    val toast = LocalToast.current
    LaunchedEffect(vm) { vm.toasts.collect { launch { toast.show(it) } } }
    PlanStateContent(
        uiState.value.state,
        staff,
        PlanActions(
            onWeek = {
                vm.onWeek(it)
                onWeek(it)
            },
            onAdd = vm::onAdd,
            onEdit = vm::onEdit,
            onDismissExercise = vm::onDismissExercise,
            onExerciseChanged = vm::onExerciseChanged,
            onDaysChanged = vm::onDaysChanged,
            onSaveExercise = vm::onSaveExercise,
            onRemove = vm::onRemove,
            onCopy = vm::onCopy,
            onWriteNote = vm::onWriteNote,
            onDismissNote = vm::onDismissNote,
            onNoteChanged = vm::onNoteChanged,
            onSaveNote = vm::onSaveNote,
            onToggle = vm::onToggle
        )
    )
}

/** What the Piano does, from the screen down to its sheets. */
class PlanActions(
    val onWeek: (LocalDate) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onEdit: (PlanItem) -> Unit = {},
    val onDismissExercise: () -> Unit = {},
    val onExerciseChanged: (LibraryExercise) -> Unit = {},
    val onDaysChanged: (Set<Int>) -> Unit = {},
    val onSaveExercise: () -> Unit = {},
    val onRemove: () -> Unit = {},
    val onCopy: () -> Unit = {},
    val onWriteNote: () -> Unit = {},
    val onDismissNote: () -> Unit = {},
    val onNoteChanged: (String) -> Unit = {},
    val onSaveNote: () -> Unit = {},
    val onToggle: (PlanCheck) -> Unit = {}
)

@Composable
fun PlanStateContent(state: UseCaseMutableState<PlanScreenState>?, staff: Boolean, actions: PlanActions) {
    when (state) {
        is UseCaseMutableState.Error -> state.handler.ErrorScreenContent()
        is UseCaseMutableState.ShowData -> PlanContent(state.items, staff, actions)
        UseCaseMutableState.Loading, null -> Loader()
    }
}

/**
 * While another week loads, the previous one stays dimmed under the loader, and can't be checked or edited.
 * Only the player checks, only staff edit.
 */
@Composable
fun PlanContent(state: PlanScreenState, staff: Boolean, actions: PlanActions) {
    val c = FuoriOrarioTheme.colors
    val thisWeek = weekOf(state.today)
    val items = state.items
    val dim = if (state.reloading) 0.4f else 1f

    Panel {
        Column {
            Text(
                stringResource(Res.string.plan_label).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = c.muted
            )
            Text(state.player.displayName.uppercase(), style = MaterialTheme.typography.titleLarge)
        }
        WeekNav(state.week, thisWeek, actions.onWeek)
        Column(Modifier.alpha(dim), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items?.let { Completion(progress(it, state.checks)) }
            state.note?.let { StaffNote(it) }
        }
        if (staff) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryButton(stringResource(Res.string.plan_add), actions.onAdd)
                GhostButton(
                    stringResource(if (state.note == null) Res.string.note_write else Res.string.note_edit),
                    actions.onWriteNote
                )
                GhostButton(
                    stringResource(
                        if (state.confirmingCopy && !items.isNullOrEmpty()) {
                            Res.string.plan_copy_confirm
                        } else {
                            Res.string.plan_copy
                        }
                    ),
                    actions.onCopy
                )
            }
        }
    }

    items?.let { list ->
        Box {
            Panel(Modifier.alpha(dim)) {
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
                                state.checks,
                                dayIndex(state.today).takeIf { state.week == thisWeek },
                                onToggle = if (staff || state.reloading) null else actions.onToggle,
                                onEdit = if (staff && !state.reloading) ({ actions.onEdit(item) }) else null,
                                Modifier.padding(top = if (i > 0) 14.dp else 4.dp, bottom = 14.dp)
                            )
                        }
                    }
                }
            }
            if (state.reloading) Loader(Modifier.matchParentSize())
        }
    }

    state.exercise?.let { ExerciseSheet(state.player.displayName, it, state.library, state.busy, actions) }
    state.noteDraft?.let { NoteSheet(state.player.displayName, it, state.busy, actions) }
}

@Preview
@Composable
private fun PlanContentPreview() = FuoriOrarioTheme {
    val today = LocalDate(2026, 10, 8)
    val week = weekOf(today)
    Column {
        PlanContent(
            PlanScreenState(
                Member("Luca B.", Role.PLAYER, jerseyNumber = "7"),
                week,
                today,
                items = listOf(
                    PlanItem(week, "Mikan drill", Category.FOOTWORK, "3 × 20", days = listOf(0, 2, 4), id = "p1"),
                    PlanItem(week, "Tiri liberi", Category.SHOOTING, "10 × 10", days = listOf(1, 3), id = "p2")
                ),
                checks = setOf(PlanCheck("p1", 0), PlanCheck("p2", 1)),
                note = "Settimana di carico: cura i piedi."
            ),
            staff = false,
            PlanActions()
        )
    }
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
 * Prototype `openEx`: title, area, volume, description, video and days, empty for a new exercise or filled from the
 * one being edited, which can also be removed with a second tap. A new one can start from an exercise of the
 * [library], copied into the form. The ViewModel saves, so a failure leaves the sheet open as typed.
 */
@Composable
private fun ExerciseSheet(
    name: String,
    draft: ExerciseDraft,
    library: List<LibraryExercise>,
    busy: Boolean,
    actions: PlanActions
) {
    val new = draft.item == null
    Sheet(
        stringResource(if (new) Res.string.exercise_new else Res.string.exercise_edit),
        name,
        actions.onDismissExercise
    ) {
        if (new && library.isNotEmpty()) LibraryPicker(library, actions.onExerciseChanged)
        ExerciseFields(draft.exercise, actions.onExerciseChanged)
        DayPicker(draft.days, actions.onDaysChanged)
        ExerciseErrorText(draft.error)
        if (new) {
            PrimaryButton(
                stringResource(Res.string.exercise_save),
                actions.onSaveExercise,
                Modifier.fillMaxWidth(),
                enabled = !busy
            )
        } else {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                PrimaryButton(stringResource(Res.string.exercise_update), actions.onSaveExercise, enabled = !busy)
                GhostButton(
                    stringResource(
                        if (draft.confirmingRemoval) Res.string.exercise_remove_confirm else Res.string.exercise_remove
                    ),
                    actions.onRemove,
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

/** Prototype `openNote`: the staff note for the week, up to [WEEKLY_NOTE_MAX] characters; emptied, it goes. */
@Composable
private fun NoteSheet(name: String, text: String, busy: Boolean, actions: PlanActions) {
    Sheet(stringResource(Res.string.note_label), name, actions.onDismissNote) {
        Field(stringResource(Res.string.note_text), text, actions.onNoteChanged, "note", singleLine = false)
        PrimaryButton(
            stringResource(Res.string.note_save),
            actions.onSaveNote,
            Modifier.fillMaxWidth(),
            enabled = !busy
        )
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
