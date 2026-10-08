package it.manu.fuoriorario.feature.plays

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
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
import fuoriorario.composeapp.generated.resources.play_with_defense
import it.manu.fuoriorario.core.designsystem.Field
import it.manu.fuoriorario.core.designsystem.GhostButton
import it.manu.fuoriorario.core.designsystem.GhostStyle
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.designsystem.PrimaryButton
import it.manu.fuoriorario.core.designsystem.SegmentedControl
import it.manu.fuoriorario.core.designsystem.SelectField
import it.manu.fuoriorario.core.designsystem.TogglePill
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.domain.ATTACKERS
import it.manu.fuoriorario.domain.COURT_MARGIN
import it.manu.fuoriorario.domain.CourtSize
import it.manu.fuoriorario.domain.PlayCategory
import it.manu.fuoriorario.domain.Point
import it.manu.fuoriorario.domain.STEPS_MAX
import it.manu.fuoriorario.domain.frame
import it.manu.fuoriorario.domain.moves
import it.manu.fuoriorario.domain.nearest
import org.jetbrains.compose.resources.stringResource

/** How near a piece or handle a finger must land to drag it, in viewBox units. */
private const val GRAB_REACH = 60f

/** How far a finger must move, before the long-press time, to drag instead of holding. */
private val DRAG_START = 4.dp

/** A spot on a canvas [width] pixels wide, in the court's viewBox. */
private fun Offset.court(width: Int): Point {
    val scale = width / PLAY_COURT_WIDTH
    return Point(x / scale - COURT_MARGIN, y / scale - COURT_MARGIN)
}

/**
 * Staff's editor (ADR 0008): the play's fields, then one step at a time on the court. Drag a piece, or a move's handle to
 * bend it; hold an attacker to give him the ball. The court is chosen only for a new play. Leaving with changes, here
 * or with the system back, and deleting take a second tap.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlayEditor(draft: PlayDraft, busy: Boolean, actions: PlaysActions) {
    val c = FuoriOrarioTheme.colors
    val play = draft.play
    val step = draft.step

    // The gesture below lives across recompositions: it reads the draft on screen through these.
    val current by rememberUpdatedState(draft)
    val on by rememberUpdatedState(actions)

    /** The piece or handle under the finger. */
    var lifted by remember { mutableStateOf<String?>(null) }

    // ponytail: deprecated in Compose 1.10 for NavigationEventHandler, whose compose artifact isn't among our
    // dependencies yet; switch when it is.
    @Suppress("DEPRECATION")
    BackHandler(onBack = actions.onLeave)

    Panel {
        GhostButton(
            stringResource(if (draft.confirmingExit) Res.string.play_discard else Res.string.cancel),
            actions.onLeave,
            Modifier.testTag("editor_back"),
            style = if (draft.confirmingExit) GhostStyle.DANGER else GhostStyle.PLAIN
        )
        Text(
            stringResource(if (draft.isNew) Res.string.play_new else Res.string.play_edit_title).uppercase(),
            style = MaterialTheme.typography.titleLarge
        )
        Field(
            stringResource(Res.string.play_title),
            play.title,
            actions.onTitleChanged,
            "play_title"
        )
        val names = PlayCategory.entries.associateWith { stringResource(categoryName.getValue(it)) }
        SelectField(
            stringResource(Res.string.play_category),
            play.category,
            PlayCategory.entries,
            { names.getValue(it) },
            "play_category"
        ) { actions.onCategoryChanged(it) }
        Field(
            stringResource(Res.string.play_description),
            play.description.orEmpty(),
            actions.onDescriptionChanged,
            "play_description",
            singleLine = false
        )
        // On a phone the court control and "Con difesa" don't fit side by side: the pill wraps whole.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            if (draft.isNew) {
                Text(
                    stringResource(Res.string.play_court).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.muted
                )
                SegmentedControl(
                    mapOf(CourtSize.HALF to Res.string.play_half_court, CourtSize.FULL to Res.string.play_full_court),
                    play.court,
                    actions.onCourtChanged,
                    { "court_$it" }
                )
            }
            TogglePill(
                stringResource(Res.string.play_with_defense),
                play.defense,
                actions.onDefenseChanged,
                Modifier.testTag("play_defense")
            )
        }
    }

    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton(
                Res.drawable.ic_step_prev,
                stringResource(Res.string.play_prev),
                "step_prev",
                draft.index > 0,
                actions.onPrevStep
            )
            Text(
                stringResource(Res.string.play_step, draft.index + 1, play.steps.size),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            StepButton(
                Res.drawable.ic_step_next,
                stringResource(Res.string.play_next),
                "step_next",
                draft.index < play.steps.lastIndex,
                actions.onNextStep
            )
        }
        PlayCourt(
            play.court,
            moves(draft.prev, step),
            frame(draft.prev, step, 1f),
            Modifier
                .testTag("play_court")
                .pointerInput(Unit) {
                    // Drag and hold in one detector: a separate long-press detector would win over a slow drag that
                    // hasn't passed the platform's touch slop yet. Away from every piece and handle, the page scrolls.
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val at = down.position.court(size.width)
                        val target = nearest(current.targets, at, GRAB_REACH) ?: return@awaitEachGesture
                        val slop = DRAG_START.toPx()
                        var released = false
                        val moved = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            var change: PointerInputChange
                            do {
                                change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                                    ?.takeIf { it.pressed }
                                    ?: run {
                                        released = true
                                        return@withTimeoutOrNull null
                                    }
                            } while ((change.position - down.position).getDistance() <= slop)
                            change
                        }
                        when {
                            released -> Unit
                            // Held still: the ball to the attacker under the finger, if any.
                            moved == null -> {
                                on.onHold(at)
                                do {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { it.consume() }
                                } while (event.changes.any { it.pressed })
                            }
                            else -> {
                                moved.consume()
                                lifted = target
                                try {
                                    on.onPieceMoved(target, moved.position.court(size.width))
                                    drag(moved.id) { change ->
                                        change.consume()
                                        on.onPieceMoved(target, change.position.court(size.width))
                                    }
                                } finally {
                                    lifted = null
                                }
                            }
                        }
                    }
                },
            draft.targets.filterKeys { it.startsWith(HANDLE) }.values.toList(),
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
                    { actions.onScreenToggled(piece, it) },
                    Modifier.testTag("screen_$piece").semantics { contentDescription = label }
                )
            }
        }
        Field(
            stringResource(Res.string.play_note),
            step.note.orEmpty(),
            actions.onNoteChanged,
            "step_note",
            singleLine = false
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton(
                stringResource(Res.string.play_add_step),
                actions.onAddStep,
                Modifier.testTag("step_add"),
                enabled = play.steps.size < STEPS_MAX
            )
            GhostButton(
                stringResource(Res.string.play_remove_step),
                actions.onRemoveStep,
                Modifier.testTag("step_remove"),
                enabled = play.steps.size > 1
            )
        }
    }

    draft.error?.let {
        Text(stringResource(Res.string.play_no_title), color = c.accent, style = MaterialTheme.typography.bodyMedium)
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PrimaryButton(
            stringResource(Res.string.play_save),
            actions.onSave,
            Modifier.testTag("play_save"),
            enabled = !busy
        )
        if (!draft.isNew) {
            GhostButton(
                stringResource(if (draft.confirmingRemoval) Res.string.play_remove_confirm else Res.string.play_remove),
                actions.onRemove,
                Modifier.testTag("play_remove"),
                style = GhostStyle.DANGER,
                enabled = !busy
            )
        }
    }
}
