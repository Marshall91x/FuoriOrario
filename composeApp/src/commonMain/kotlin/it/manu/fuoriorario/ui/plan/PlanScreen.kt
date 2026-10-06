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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.category_athletics
import fuoriorario.composeapp.generated.resources.category_ball_handling
import fuoriorario.composeapp.generated.resources.category_defense
import fuoriorario.composeapp.generated.resources.category_footwork
import fuoriorario.composeapp.generated.resources.category_recovery
import fuoriorario.composeapp.generated.resources.category_shooting
import fuoriorario.composeapp.generated.resources.close
import fuoriorario.composeapp.generated.resources.days_short
import fuoriorario.composeapp.generated.resources.exercise_added
import fuoriorario.composeapp.generated.resources.exercise_category
import fuoriorario.composeapp.generated.resources.exercise_days
import fuoriorario.composeapp.generated.resources.exercise_description
import fuoriorario.composeapp.generated.resources.exercise_error_days
import fuoriorario.composeapp.generated.resources.exercise_error_title
import fuoriorario.composeapp.generated.resources.exercise_error_video
import fuoriorario.composeapp.generated.resources.exercise_new
import fuoriorario.composeapp.generated.resources.exercise_save
import fuoriorario.composeapp.generated.resources.exercise_title
import fuoriorario.composeapp.generated.resources.exercise_video
import fuoriorario.composeapp.generated.resources.exercise_volume
import fuoriorario.composeapp.generated.resources.plan_add
import fuoriorario.composeapp.generated.resources.plan_back
import fuoriorario.composeapp.generated.resources.plan_day_done
import fuoriorario.composeapp.generated.resources.plan_day_rest
import fuoriorario.composeapp.generated.resources.plan_day_todo
import fuoriorario.composeapp.generated.resources.plan_done
import fuoriorario.composeapp.generated.resources.plan_empty_player
import fuoriorario.composeapp.generated.resources.plan_empty_staff
import fuoriorario.composeapp.generated.resources.plan_items
import fuoriorario.composeapp.generated.resources.plan_label
import fuoriorario.composeapp.generated.resources.plan_next
import fuoriorario.composeapp.generated.resources.plan_prev
import fuoriorario.composeapp.generated.resources.plan_this_week
import fuoriorario.composeapp.generated.resources.plan_video
import it.manu.fuoriorario.core.today
import it.manu.fuoriorario.data.PlanRepository
import it.manu.fuoriorario.domain.Category
import it.manu.fuoriorario.domain.DESCRIPTION_MAX
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanCheck
import it.manu.fuoriorario.domain.PlanItem
import it.manu.fuoriorario.domain.PlanItemError
import it.manu.fuoriorario.domain.Progress
import it.manu.fuoriorario.domain.TITLE_MAX
import it.manu.fuoriorario.domain.VOLUME_MAX
import it.manu.fuoriorario.domain.newPlanItem
import it.manu.fuoriorario.domain.planItemError
import it.manu.fuoriorario.domain.progress
import it.manu.fuoriorario.domain.weekOf
import it.manu.fuoriorario.ui.components.Field
import it.manu.fuoriorario.ui.components.GhostButton
import it.manu.fuoriorario.ui.components.LoadFailed
import it.manu.fuoriorario.ui.components.LocalToast
import it.manu.fuoriorario.ui.components.Panel
import it.manu.fuoriorario.ui.components.PrimaryButton
import it.manu.fuoriorario.ui.components.SelectField
import it.manu.fuoriorario.ui.components.launchWrite
import it.manu.fuoriorario.ui.components.short
import it.manu.fuoriorario.ui.components.show
import it.manu.fuoriorario.ui.theme.FuoriOrarioTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

/** Italian name of each area. On web the names load asynchronously: empty until then. */
private val categoryNames
    @Composable get() = mapOf(
        Category.BALL_HANDLING to stringResource(Res.string.category_ball_handling),
        Category.SHOOTING to stringResource(Res.string.category_shooting),
        Category.FOOTWORK to stringResource(Res.string.category_footwork),
        Category.ATHLETICS to stringResource(Res.string.category_athletics),
        Category.DEFENSE to stringResource(Res.string.category_defense),
        Category.RECOVERY to stringResource(Res.string.category_recovery)
    )

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
    var adding by remember { mutableStateOf(false) }
    val thisWeek = remember { weekOf(today()) }
    val todayIndex = remember { today().dayOfWeek.isoDayNumber - 1 }

    /** A save or a check is in flight. */
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(week, attempt) {
        items = null
        loadFailed = false
        try {
            val loaded = plans.items(player, week)
            checks = plans.checks(loaded)
            items = loaded
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            loadFailed = true
        }
    }

    fun save(item: PlanItem) {
        if (busy) return
        busy = true
        scope.launchWrite(toast, onDone = { busy = false }) {
            val saved = plans.add(item.copy(memberId = player.id))
            items = items.orEmpty() + saved
            adding = false
            launch { toast.show(getString(Res.string.exercise_added)) }
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
        if (staff) PrimaryButton(stringResource(Res.string.plan_add), { adding = true })
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
                            Modifier.padding(top = if (i > 0) 14.dp else 4.dp, bottom = 14.dp)
                        )
                    }
                }
            }
        }
    }

    if (adding) ExerciseSheet(player.displayName, week, busy, onDismiss = { adding = false }, onSave = ::save)
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

/** Prototype `.ex`: area chip, title, volume, description, video link and the week's day dots. */
@Composable
private fun ItemRow(
    item: PlanItem,
    checks: Set<PlanCheck>,
    today: Int?,
    onToggle: ((PlanCheck) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val c = FuoriOrarioTheme.colors
    val uris = LocalUriHandler.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                categoryNames.getValue(item.category).uppercase(),
                Modifier
                    .background(c.surface2, RoundedCornerShape(50))
                    .border(1.dp, c.line, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = c.muted
            )
            Text(item.title, style = MaterialTheme.typography.titleMedium)
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
            val check = PlanCheck(item.id.orEmpty(), i)
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
                    if (done) Text("✓", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.accentInk)
                }
            }
        }
    }
}

/**
 * Prototype `openEx`, new exercise only: title, area, volume, description, video and days (Mon, Wed, Fri by default).
 * The screen saves, so a failure leaves the sheet open as typed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseSheet(
    name: String,
    week: LocalDate,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (PlanItem) -> Unit
) {
    val c = FuoriOrarioTheme.colors
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Category.entries.first()) }
    var volume by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var video by remember { mutableStateOf("") }
    var days by remember { mutableStateOf(setOf(0, 2, 4)) }
    var error by remember { mutableStateOf<PlanItemError?>(null) }

    fun save() {
        error = planItemError(title, days, video)
        if (error == null) onSave(newPlanItem(week, title, category, volume, description, video, days))
    }

    ModalBottomSheet(
        onDismiss,
        // Fully open: half way the save button can sit below the screen.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                        stringResource(Res.string.exercise_new).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.muted
                    )
                    Text(name.uppercase(), style = MaterialTheme.typography.titleLarge)
                }
                GhostButton(stringResource(Res.string.close), onDismiss)
            }
            Field(stringResource(Res.string.exercise_title), title, { title = it.take(TITLE_MAX) }, "exercise_title")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val names = categoryNames
                SelectField(
                    stringResource(Res.string.exercise_category),
                    category,
                    Category.entries,
                    { names.getValue(it) },
                    "exercise_category",
                    Modifier.weight(1f)
                ) { category = it }
                Field(
                    stringResource(Res.string.exercise_volume),
                    volume,
                    { volume = it.take(VOLUME_MAX) },
                    "exercise_volume",
                    Modifier.weight(1f)
                )
            }
            Field(
                stringResource(Res.string.exercise_description),
                description,
                { description = it.take(DESCRIPTION_MAX) },
                "exercise_description",
                singleLine = false
            )
            Field(
                stringResource(Res.string.exercise_video),
                video,
                { video = it },
                "exercise_video",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
            DayPicker(days) { days = it }
            error?.let {
                val text = when (it) {
                    PlanItemError.TITLE -> Res.string.exercise_error_title
                    PlanItemError.NO_DAYS -> Res.string.exercise_error_days
                    PlanItemError.VIDEO -> Res.string.exercise_error_video
                }
                Text(stringResource(text), color = c.accent, style = MaterialTheme.typography.bodyMedium)
            }
            PrimaryButton(stringResource(Res.string.exercise_save), ::save, Modifier.fillMaxWidth(), enabled = !busy)
        }
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
