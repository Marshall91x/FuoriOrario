package it.manu.fuoriorario.ui.plays

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.cancel
import fuoriorario.composeapp.generated.resources.ic_step_next
import fuoriorario.composeapp.generated.resources.ic_step_prev
import fuoriorario.composeapp.generated.resources.play_add_step
import fuoriorario.composeapp.generated.resources.play_category
import fuoriorario.composeapp.generated.resources.play_court
import fuoriorario.composeapp.generated.resources.play_description
import fuoriorario.composeapp.generated.resources.play_discard
import fuoriorario.composeapp.generated.resources.play_edit_title
import fuoriorario.composeapp.generated.resources.play_editor_hint
import fuoriorario.composeapp.generated.resources.play_full_court
import fuoriorario.composeapp.generated.resources.play_half_court
import fuoriorario.composeapp.generated.resources.play_new
import fuoriorario.composeapp.generated.resources.play_next
import fuoriorario.composeapp.generated.resources.play_no_title
import fuoriorario.composeapp.generated.resources.play_note
import fuoriorario.composeapp.generated.resources.play_prev
import fuoriorario.composeapp.generated.resources.play_remove
import fuoriorario.composeapp.generated.resources.play_remove_confirm
import fuoriorario.composeapp.generated.resources.play_remove_step
import fuoriorario.composeapp.generated.resources.play_save
import fuoriorario.composeapp.generated.resources.play_screen
import fuoriorario.composeapp.generated.resources.play_screens
import fuoriorario.composeapp.generated.resources.play_step
import fuoriorario.composeapp.generated.resources.play_title
import fuoriorario.composeapp.generated.resources.play_unsaved
import fuoriorario.composeapp.generated.resources.play_with_defense
import it.manu.fuoriorario.domain.ATTACKERS
import it.manu.fuoriorario.domain.COURT_MARGIN
import it.manu.fuoriorario.domain.CourtSize
import it.manu.fuoriorario.domain.MoveKind
import it.manu.fuoriorario.domain.PLAY_DESCRIPTION_MAX
import it.manu.fuoriorario.domain.PLAY_TITLE_MAX
import it.manu.fuoriorario.domain.Play
import it.manu.fuoriorario.domain.PlayCategory
import it.manu.fuoriorario.domain.PlayError
import it.manu.fuoriorario.domain.Point
import it.manu.fuoriorario.domain.STEPS_MAX
import it.manu.fuoriorario.domain.STEP_NOTE_MAX
import it.manu.fuoriorario.domain.Step
import it.manu.fuoriorario.domain.bent
import it.manu.fuoriorario.domain.cleaned
import it.manu.fuoriorario.domain.frame
import it.manu.fuoriorario.domain.isDefender
import it.manu.fuoriorario.domain.moves
import it.manu.fuoriorario.domain.nearest
import it.manu.fuoriorario.domain.playError
import it.manu.fuoriorario.domain.with
import it.manu.fuoriorario.domain.withDefense
import it.manu.fuoriorario.domain.within
import it.manu.fuoriorario.domain.without
import it.manu.fuoriorario.ui.components.Field
import it.manu.fuoriorario.ui.components.GhostButton
import it.manu.fuoriorario.ui.components.GhostStyle
import it.manu.fuoriorario.ui.components.LocalToast
import it.manu.fuoriorario.ui.components.Panel
import it.manu.fuoriorario.ui.components.PrimaryButton
import it.manu.fuoriorario.ui.components.SegmentedControl
import it.manu.fuoriorario.ui.components.SelectField
import it.manu.fuoriorario.ui.components.TogglePill
import it.manu.fuoriorario.ui.components.show
import it.manu.fuoriorario.ui.theme.FuoriOrarioTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/** A handle's key among the drag targets: the piece whose move it bends, marked. */
private const val HANDLE = "~"

/** How near a piece or handle a finger must land to drag it, in viewBox units. */
private const val GRAB_REACH = 60f

/** How near an attacker a hold must be to give him the ball, in viewBox units: about two pieces. */
private const val HOLD_REACH = 40f

/** A spot on a canvas [width] pixels wide, in the court's viewBox. */
private fun Offset.court(width: Int): Point {
    val scale = width / PLAY_COURT_WIDTH
    return Point(x / scale - COURT_MARGIN, y / scale - COURT_MARGIN)
}

/**
 * Staff's editor (ADR 0008): the play's fields, then one step at a time on the court. Drag a piece, or a move's handle to
 * bend it; hold an attacker to give him the ball. The court is chosen only for a new play. Leaving with changes, here
 * or with the system back, and deleting take a second tap. [onUnsaved] tells whether there are changes to lose.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlayEditor(
    play: Play,
    busy: Boolean,
    onSave: (Play) -> Unit,
    onRemove: (Play) -> Unit,
    onUnsaved: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val c = FuoriOrarioTheme.colors
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf(play) }
    var index by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<PlayError?>(null) }
    var confirmingExit by remember { mutableStateOf(false) }
    var confirmingRemoval by remember { mutableStateOf(false) }

    /** The piece or handle under the finger. */
    var lifted by remember { mutableStateOf<String?>(null) }
    val isNew = play.id == null
    val prev = draft.steps.getOrNull(index - 1)
    val step = draft.steps[index]
    val moves = moves(prev, step)

    fun change(to: Play) {
        draft = to
        confirmingExit = false
        confirmingRemoval = false
    }

    // The gestures below live across recompositions: they read the step on screen through these, never through [step].
    fun current() = draft.steps[index]

    fun edit(to: (Step) -> Step) = change(draft.copy(steps = draft.steps.with(index, to(current()))))

    /** The pieces, and a handle halfway along each move to bend it. */
    fun targets(): Map<String, Point> = current().pos + moves(draft.steps.getOrNull(index - 1), current())
        .filter { it.kind != MoveKind.PASS && it.from != it.to }
        .associate { "$HANDLE${it.piece}" to it.at(0.5f) }

    fun moveTo(target: String, at: Point) {
        val spot = at.within(draft.court)
        val piece = target.removePrefix(HANDLE)
        val from = draft.steps.getOrNull(index - 1)?.pos?.get(piece)
        when {
            !target.startsWith(HANDLE) -> edit { it.copy(pos = it.pos + (piece to spot)) }
            from != null -> edit { it.bent(piece, from, spot) }
        }
    }

    val unsaved = draft != play
    LaunchedEffect(unsaved) { onUnsaved(unsaved) }
    DisposableEffect(Unit) { onDispose { onUnsaved(false) } }

    fun leave() {
        if (!unsaved || confirmingExit) return onBack()
        confirmingExit = true
        scope.launch { toast.show(getString(Res.string.play_unsaved)) }
    }
    // ponytail: deprecated in Compose 1.10 for NavigationEventHandler, whose compose artifact isn't among our
    // dependencies yet; switch when it is.
    @Suppress("DEPRECATION")
    BackHandler(onBack = ::leave)

    Panel {
        GhostButton(
            stringResource(if (confirmingExit) Res.string.play_discard else Res.string.cancel),
            ::leave,
            Modifier.testTag("editor_back"),
            style = if (confirmingExit) GhostStyle.DANGER else GhostStyle.PLAIN
        )
        Text(
            stringResource(if (isNew) Res.string.play_new else Res.string.play_edit_title).uppercase(),
            style = MaterialTheme.typography.titleLarge
        )
        Field(
            stringResource(Res.string.play_title),
            draft.title,
            { change(draft.copy(title = it.take(PLAY_TITLE_MAX))) },
            "play_title"
        )
        val names = PlayCategory.entries.associateWith { stringResource(categoryName.getValue(it)) }
        SelectField(
            stringResource(Res.string.play_category),
            draft.category,
            PlayCategory.entries,
            { names.getValue(it) },
            "play_category"
        ) { change(draft.copy(category = it)) }
        Field(
            stringResource(Res.string.play_description),
            draft.description.orEmpty(),
            { change(draft.copy(description = it.take(PLAY_DESCRIPTION_MAX))) },
            "play_description",
            singleLine = false
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isNew) {
                Text(
                    stringResource(Res.string.play_court).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.muted
                )
                SegmentedControl(
                    mapOf(CourtSize.HALF to Res.string.play_half_court, CourtSize.FULL to Res.string.play_full_court),
                    draft.court,
                    { change(draft.copy(court = it)) },
                    { "court_$it" }
                )
            }
            TogglePill(
                stringResource(Res.string.play_with_defense),
                draft.defense,
                { on -> change(draft.copy(defense = on, steps = draft.steps.map { it.withDefense(on) })) },
                Modifier.testTag("play_defense")
            )
        }
    }

    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton(Res.drawable.ic_step_prev, stringResource(Res.string.play_prev), "step_prev", index > 0) {
                index--
            }
            Text(
                stringResource(Res.string.play_step, index + 1, draft.steps.size),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            StepButton(
                Res.drawable.ic_step_next,
                stringResource(Res.string.play_next),
                "step_next",
                index < draft.steps.lastIndex
            ) { index++ }
        }
        PlayCourt(
            draft.court,
            moves,
            frame(prev, step, 1f),
            Modifier
                .testTag("play_court")
                .pointerInput(Unit) {
                    // Only a finger near a piece or handle drags; elsewhere the page scrolls.
                    awaitEachGesture {
                        // The hold detector below has already consumed the down.
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val target = nearest(targets(), down.position.court(size.width), GRAB_REACH)
                            ?: return@awaitEachGesture
                        val start = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                            ?: return@awaitEachGesture
                        lifted = target
                        try {
                            moveTo(target, start.position.court(size.width))
                            drag(start.id) { change ->
                                change.consume()
                                moveTo(target, change.position.court(size.width))
                            }
                        } finally {
                            lifted = null
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { at ->
                        val attacker =
                            nearest(current().pos.filterKeys { !isDefender(it) }, at.court(size.width), HOLD_REACH)
                        if (attacker != null) edit { it.copy(ball = attacker) }
                    })
                },
            targets().filterKeys { it.startsWith(HANDLE) }.values.toList(),
            lifted
        )
        Text(stringResource(Res.string.play_editor_hint), color = c.muted, style = MaterialTheme.typography.bodyMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(Res.string.play_screens).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = c.muted
            )
            ATTACKERS.forEach { piece ->
                val label = stringResource(Res.string.play_screen, piece)
                TogglePill(
                    piece,
                    piece in step.screens,
                    { on -> edit { it.copy(screens = if (on) it.screens + piece else it.screens - piece) } },
                    Modifier.testTag("screen_$piece").semantics { contentDescription = label }
                )
            }
        }
        Field(
            stringResource(Res.string.play_note),
            step.note.orEmpty(),
            { note -> edit { it.copy(note = note.take(STEP_NOTE_MAX)) } },
            "step_note",
            singleLine = false
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton(
                stringResource(Res.string.play_add_step),
                {
                    // A copy of where everyone stands, ready to move.
                    change(
                        draft.copy(
                            steps = draft.steps.toMutableList().also {
                                it.add(index + 1, Step(step.pos, step.ball))
                            }
                        )
                    )
                    index++
                },
                Modifier.testTag("step_add"),
                enabled = draft.steps.size < STEPS_MAX
            )
            GhostButton(
                stringResource(Res.string.play_remove_step),
                {
                    change(draft.copy(steps = draft.steps.without(index)))
                    index = index.coerceAtMost(draft.steps.lastIndex)
                },
                Modifier.testTag("step_remove"),
                enabled = draft.steps.size > 1
            )
        }
    }

    error?.let {
        Text(stringResource(Res.string.play_no_title), color = c.accent, style = MaterialTheme.typography.bodyMedium)
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PrimaryButton(
            stringResource(Res.string.play_save),
            {
                error = playError(draft)
                if (error == null) onSave(draft.cleaned())
            },
            Modifier.testTag("play_save"),
            enabled = !busy
        )
        if (!isNew) {
            GhostButton(
                stringResource(if (confirmingRemoval) Res.string.play_remove_confirm else Res.string.play_remove),
                { if (confirmingRemoval) onRemove(play) else confirmingRemoval = true },
                Modifier.testTag("play_remove"),
                style = GhostStyle.DANGER,
                enabled = !busy
            )
        }
    }
}
