package it.manu.fuoriorario.feature.plays

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.ic_step_next
import fuoriorario.composeapp.generated.resources.ic_step_prev
import fuoriorario.composeapp.generated.resources.play_all
import fuoriorario.composeapp.generated.resources.play_attack
import fuoriorario.composeapp.generated.resources.play_baseline_inbound
import fuoriorario.composeapp.generated.resources.play_defense
import fuoriorario.composeapp.generated.resources.play_edit
import fuoriorario.composeapp.generated.resources.play_end_of_game
import fuoriorario.composeapp.generated.resources.play_full_court
import fuoriorario.composeapp.generated.resources.play_half_court
import fuoriorario.composeapp.generated.resources.play_new
import fuoriorario.composeapp.generated.resources.play_next
import fuoriorario.composeapp.generated.resources.play_play
import fuoriorario.composeapp.generated.resources.play_prev
import fuoriorario.composeapp.generated.resources.play_sideline_inbound
import fuoriorario.composeapp.generated.resources.play_step
import fuoriorario.composeapp.generated.resources.play_transition
import fuoriorario.composeapp.generated.resources.play_with_defense
import fuoriorario.composeapp.generated.resources.play_zone_offense
import fuoriorario.composeapp.generated.resources.plays_empty
import it.manu.fuoriorario.core.designsystem.Chip
import it.manu.fuoriorario.core.designsystem.GhostButton
import it.manu.fuoriorario.core.designsystem.LocalToast
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.designsystem.PrimaryButton
import it.manu.fuoriorario.core.designsystem.show
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.CourtSize
import it.manu.fuoriorario.domain.Play
import it.manu.fuoriorario.domain.PlayCategory
import it.manu.fuoriorario.domain.Point
import it.manu.fuoriorario.domain.STEP_MILLIS
import it.manu.fuoriorario.domain.byCategory
import it.manu.fuoriorario.domain.frame
import it.manu.fuoriorario.domain.moves
import it.manu.fuoriorario.domain.startingStep
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

internal val categoryName = mapOf(
    PlayCategory.ATTACK to Res.string.play_attack,
    PlayCategory.ZONE_OFFENSE to Res.string.play_zone_offense,
    PlayCategory.SIDELINE_INBOUND to Res.string.play_sideline_inbound,
    PlayCategory.BASELINE_INBOUND to Res.string.play_baseline_inbound,
    PlayCategory.END_OF_GAME to Res.string.play_end_of_game,
    PlayCategory.TRANSITION to Res.string.play_transition,
    PlayCategory.DEFENSE to Res.string.play_defense
)

/**
 * Schemi: the team's plays by category; tapping one opens it step by step, "Tutti gli schemi" goes back. Staff also
 * draw new ones and edit or delete any of them; [onUnsaved] tells whether the editor holds changes to lose.
 */
@Composable
fun PlaysScreen(staff: Boolean, onUnsaved: (Boolean) -> Unit = {}, vm: PlaysViewModel = koinViewModel()) {
    val uiState = vm.uiState.collectAsStateWithLifecycle()
    val toast = LocalToast.current
    LaunchedEffect(vm) { vm.toasts.collect { launch { toast.show(it) } } }
    val unsaved = uiState.value.data?.draft?.unsaved == true
    LaunchedEffect(unsaved) { onUnsaved(unsaved) }
    DisposableEffect(Unit) { onDispose { onUnsaved(false) } }
    PlaysStateContent(
        uiState.value.state,
        staff,
        PlaysActions(
            onOpen = vm::onOpen,
            onBack = vm::onBack,
            onNew = vm::onNew,
            onEdit = vm::onEdit,
            onTitleChanged = vm::onTitleChanged,
            onCategoryChanged = vm::onCategoryChanged,
            onDescriptionChanged = vm::onDescriptionChanged,
            onCourtChanged = vm::onCourtChanged,
            onDefenseChanged = vm::onDefenseChanged,
            onPrevStep = vm::onPrevStep,
            onNextStep = vm::onNextStep,
            onPieceMoved = vm::onPieceMoved,
            onHold = vm::onHold,
            onScreenToggled = vm::onScreenToggled,
            onNoteChanged = vm::onNoteChanged,
            onAddStep = vm::onAddStep,
            onRemoveStep = vm::onRemoveStep,
            onLeave = vm::onLeave,
            onSave = vm::onSave,
            onRemove = vm::onRemove
        )
    )
}

/** What Schemi does, from the list down to the editor. */
class PlaysActions(
    val onOpen: (Play) -> Unit = {},
    val onBack: () -> Unit = {},
    val onNew: () -> Unit = {},
    val onEdit: () -> Unit = {},
    val onTitleChanged: (String) -> Unit = {},
    val onCategoryChanged: (PlayCategory) -> Unit = {},
    val onDescriptionChanged: (String) -> Unit = {},
    val onCourtChanged: (CourtSize) -> Unit = {},
    val onDefenseChanged: (Boolean) -> Unit = {},
    val onPrevStep: () -> Unit = {},
    val onNextStep: () -> Unit = {},
    val onPieceMoved: (String, Point) -> Unit = { _, _ -> },
    val onHold: (Point) -> Unit = {},
    val onScreenToggled: (String, Boolean) -> Unit = { _, _ -> },
    val onNoteChanged: (String) -> Unit = {},
    val onAddStep: () -> Unit = {},
    val onRemoveStep: () -> Unit = {},
    val onLeave: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onRemove: () -> Unit = {}
)

@Composable
fun PlaysStateContent(state: UseCaseMutableState<PlaysScreenState>?, staff: Boolean, actions: PlaysActions) {
    when (state) {
        is UseCaseMutableState.Error -> state.handler.ErrorScreenContent()
        is UseCaseMutableState.ShowData -> PlaysContent(state.items, staff, actions)
        // Never: the plays fill in a screen that is there from the start.
        UseCaseMutableState.Loading, null -> Unit
    }
}

/** The editor if open, else the play in the viewer, else the list. Nothing until the plays arrive. */
@Composable
fun PlaysContent(state: PlaysScreenState, staff: Boolean, actions: PlaysActions) {
    val open = state.open
    val draft = state.draft
    when {
        draft != null -> key(draft.original) { PlayEditor(draft, state.busy, actions) }
        open != null -> key(open) { PlayViewer(open, onEdit = actions.onEdit.takeIf { staff }, actions.onBack) }
        else -> state.plays?.let { list ->
            if (staff) PrimaryButton(stringResource(Res.string.play_new), actions.onNew, Modifier.testTag("play_new"))
            if (list.isEmpty()) {
                Panel { Text(stringResource(Res.string.plays_empty), color = FuoriOrarioTheme.colors.muted) }
            }
            byCategory(list).forEach { (category, group) -> CategoryPanel(category, group, actions.onOpen) }
        }
    }
}

@Preview
@Composable
private fun PlaysContentPreview() = FuoriOrarioTheme {
    val play = Play(
        "Pick and roll centrale",
        PlayCategory.ATTACK,
        "Blocco del centro per il playmaker in punta.",
        CourtSize.HALF,
        defense = false,
        steps = listOf(startingStep(false)),
        id = "seed"
    )
    Column { PlaysContent(PlaysScreenState(listOf(play)), staff = true, PlaysActions()) }
}

/** A category's plays, like the prototype's `.list` of `.item`s. */
@Composable
private fun CategoryPanel(category: PlayCategory, plays: List<Play>, onOpen: (Play) -> Unit) {
    val c = FuoriOrarioTheme.colors
    Panel {
        Text(stringResource(categoryName.getValue(category)).uppercase(), style = MaterialTheme.typography.titleMedium)
        Column {
            plays.forEach { play ->
                HorizontalDivider(color = c.line)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .testTag("play_${play.id}")
                        .clickable { onOpen(play) }
                        .padding(vertical = 11.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(play.title, fontWeight = FontWeight.SemiBold)
                    play.description?.let { Text(it, color = c.muted, style = MaterialTheme.typography.bodyMedium) }
                    Chips(play)
                }
            }
        }
    }
}

@Composable
private fun Chips(play: Play) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Chip(
            stringResource(
                if (play.court ==
                    CourtSize.HALF
                ) {
                    Res.string.play_half_court
                } else {
                    Res.string.play_full_court
                }
            )
        )
        if (play.defense) Chip(stringResource(Res.string.play_with_defense))
    }
}

/**
 * One play: the court at a step with the moves that led there, its note, ◀ / ▶ and "Riproduci". ▶ and "Riproduci"
 * animate into the next step, "Riproduci" on to the last (from the first, if already there); ◀ jumps back, and
 * ◀ / ▶ stop it. Staff get [onEdit].
 */
@Composable
private fun PlayViewer(play: Play, onEdit: (() -> Unit)?, onBack: () -> Unit) {
    val c = FuoriOrarioTheme.colors
    val last = play.steps.lastIndex
    var index by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(1f) }
    var playing by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun forward() {
        progress.snapTo(0f)
        index++
        progress.animateTo(1f, tween(STEP_MILLIS, easing = LinearEasing))
    }

    /** Stops playing and runs [action] from the end of the step on screen. */
    fun go(action: suspend () -> Unit) {
        playing?.cancel()
        playing = null
        scope.launch {
            progress.snapTo(1f)
            action()
        }
    }

    /** Plays to the last step, starting over from the first if already there. */
    fun play() {
        go {}
        val job = scope.launch(start = CoroutineStart.LAZY) {
            if (index == last) index = 0
            while (index < last) forward()
        }
        playing = job
        job.invokeOnCompletion { if (playing === job) playing = null }
        job.start()
    }

    val prev = play.steps.getOrNull(index - 1)
    val step = play.steps[index]

    Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            GhostButton(stringResource(Res.string.play_all), onBack, Modifier.testTag("plays_back"))
            onEdit?.let { GhostButton(stringResource(Res.string.play_edit), it, Modifier.testTag("play_edit")) }
        }
        Column {
            Text(
                stringResource(categoryName.getValue(play.category)).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = c.muted
            )
            Text(play.title.uppercase(), style = MaterialTheme.typography.titleLarge)
        }
        play.description?.let { Text(it, color = c.muted, style = MaterialTheme.typography.bodyMedium) }
        Chips(play)
        PlayCourt(play.court, moves(prev, step), frame(prev, step, progress.value))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton(Res.drawable.ic_step_prev, stringResource(Res.string.play_prev), "step_prev", index > 0) {
                go { index-- }
            }
            Text(
                stringResource(Res.string.play_step, index + 1, play.steps.size),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            StepButton(Res.drawable.ic_step_next, stringResource(Res.string.play_next), "step_next", index < last) {
                go { forward() }
            }
        }
        step.note?.let {
            Text(
                it,
                Modifier
                    .fillMaxWidth()
                    .background(c.surface2, RoundedCornerShape(10.dp))
                    .padding(12.dp, 10.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium
            )
        }
        PrimaryButton(
            stringResource(Res.string.play_play),
            ::play,
            Modifier.fillMaxWidth().testTag("play_toggle"),
            enabled = playing == null && last > 0
        )
    }
}

/** Prototype `.btn.ghost` holding a drawn arrow. */
@Composable
internal fun StepButton(
    icon: DrawableResource,
    description: String,
    tag: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val c = FuoriOrarioTheme.colors
    OutlinedButton(
        onClick,
        Modifier.testTag(tag),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, c.line),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = c.ink,
            disabledContentColor = c.muted.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(14.dp, 8.dp)
    ) { Icon(painterResource(icon), description, Modifier.size(18.dp)) }
}
