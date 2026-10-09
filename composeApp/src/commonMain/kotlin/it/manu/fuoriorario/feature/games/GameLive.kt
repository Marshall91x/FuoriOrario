package it.manu.fuoriorario.feature.games

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.game_all
import fuoriorario.composeapp.generated.resources.game_events
import fuoriorario.composeapp.generated.resources.game_finish
import fuoriorario.composeapp.generated.resources.game_free_throw
import fuoriorario.composeapp.generated.resources.game_made
import fuoriorario.composeapp.generated.resources.game_missed
import fuoriorario.composeapp.generated.resources.game_no_events
import fuoriorario.composeapp.generated.resources.game_opponent_points
import fuoriorario.composeapp.generated.resources.game_overtime
import fuoriorario.composeapp.generated.resources.game_pick
import fuoriorario.composeapp.generated.resources.game_q1
import fuoriorario.composeapp.generated.resources.game_q2
import fuoriorario.composeapp.generated.resources.game_q3
import fuoriorario.composeapp.generated.resources.game_q4
import fuoriorario.composeapp.generated.resources.game_remove_event
import fuoriorario.composeapp.generated.resources.game_undo
import fuoriorario.composeapp.generated.resources.game_us
import it.manu.fuoriorario.core.designsystem.GhostButton
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.designsystem.PrimaryButton
import it.manu.fuoriorario.core.designsystem.SegmentedControl
import it.manu.fuoriorario.core.designsystem.TogglePill
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.domain.CallUp
import it.manu.fuoriorario.domain.GameDraft
import it.manu.fuoriorario.domain.GameEvent
import it.manu.fuoriorario.domain.GameEventType
import it.manu.fuoriorario.domain.Quarter
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.domain.ZoneHeat
import it.manu.fuoriorario.domain.quarterScores
import it.manu.fuoriorario.domain.shotZones
import it.manu.fuoriorario.feature.shots.Court
import it.manu.fuoriorario.feature.shots.zoneName
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

private val quarterName = mapOf(
    Quarter.Q1 to Res.string.game_q1,
    Quarter.Q2 to Res.string.game_q2,
    Quarter.Q3 to Res.string.game_q3,
    Quarter.Q4 to Res.string.game_q4,
    Quarter.OT to Res.string.game_overtime
)

// Live colours nothing against the riferimenti: a few shots in one game say little.
private val noHeat = Zone.entries.associateWith { ZoneHeat.NONE }

/**
 * Live (glossary): the staff courtside. A convocato tapped stays selected; then a zone of the court and Segnato or
 * Sbagliato, or a free throw. The opponent's points, the quarter, "Annulla ultimo", the events, "Termina partita".
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun GameLive(draft: GameDraft, busy: Boolean, actions: GamesActions) {
    val c = FuoriOrarioTheme.colors
    // ponytail: deprecated, NavigationEventHandler when its compose artifact is among our dependencies (as in PlayEditor).
    @Suppress("DEPRECATION")
    BackHandler(onBack = actions.onLeaveLive)
    // The zone tapped, waiting for Segnato or Sbagliato.
    var pending by remember { mutableStateOf<Zone?>(null) }
    val picked = draft.selected != null

    GhostButton(stringResource(Res.string.game_all), actions.onLeaveLive, Modifier.testTag("game_back"))
    Scoreboard(draft)
    SegmentedControl(quarterName, draft.quarter, actions.onQuarter, tag = { "quarter_${it.name.lowercase()}" })
    Panel {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            draft.callUps.forEach { p ->
                TogglePill(p.label, p.id == draft.selected, {
                    actions.onSelect(p.id)
                }, Modifier.testTag("pick_${p.id}"))
            }
        }
        if (!picked) Text(stringResource(Res.string.game_pick), color = c.muted)
        Court(shotZones(draft.events), noHeat, pending) { zone -> if (picked) pending = zone }
        pending?.let { zone ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(zoneName.getValue(zone)), Modifier.weight(1f))
                PrimaryButton(stringResource(Res.string.game_made), {
                    actions.onShot(zone, true)
                    pending = null
                }, Modifier.testTag("shot_made"))
                GhostButton(stringResource(Res.string.game_missed), {
                    actions.onShot(zone, false)
                    pending = null
                }, Modifier.testTag("shot_missed"))
            }
        }
        val freeThrow = stringResource(Res.string.game_free_throw)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("$freeThrow ✓", { actions.onFreeThrow(true) }, Modifier.testTag("free_made"), enabled = picked)
            GhostButton("$freeThrow ✗", {
                actions.onFreeThrow(false)
            }, Modifier.testTag("free_missed"), enabled = picked)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..3).forEach { points ->
                GhostButton(
                    stringResource(Res.string.game_opponent_points, points),
                    { actions.onOpponentScored(points) },
                    Modifier.testTag("opponent_$points")
                )
            }
        }
        GhostButton(stringResource(Res.string.game_undo), actions.onUndo, enabled = draft.events.isNotEmpty())
    }
    Events(draft.events, draft.callUps, actions.onRemoveEvent)
    PrimaryButton(
        stringResource(Res.string.game_finish),
        actions.onFinish,
        Modifier.fillMaxWidth().testTag("game_finish"),
        enabled = !busy
    )
}

/** "NOI 12 – 8 VIRTUS" and the parziali. */
@Composable
private fun Scoreboard(draft: GameDraft) {
    val c = FuoriOrarioTheme.colors
    val score = draft.score
    Panel(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "${stringResource(
                Res.string.game_us
            ).uppercase()} ${score.us} – ${score.them} ${draft.opponent.uppercase()}",
            Modifier.testTag("game_score"),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            quarterScores(draft.events).map { (q, s) -> "${stringResource(quarterName.getValue(q))} ${s.us}–${s.them}" }
                .joinToString(" · "),
            color = c.muted,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** Newest first, each with its ✕. */
@Composable
private fun Events(events: List<GameEvent>, callUps: List<CallUp>, onRemove: (Int) -> Unit) {
    val c = FuoriOrarioTheme.colors
    val remove = stringResource(Res.string.game_remove_event)
    Panel {
        Text(stringResource(Res.string.game_events).uppercase(), style = MaterialTheme.typography.titleMedium)
        if (events.isEmpty()) Text(stringResource(Res.string.game_no_events), color = c.muted)
        Column {
            events.withIndex().reversed().forEach { (i, e) ->
                HorizontalDivider(color = c.line)
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val who = callUps.find { it.id == e.memberId }?.label
                    Text(
                        listOfNotNull(
                            stringResource(quarterName.getValue(e.quarter)),
                            who,
                            eventText(e)
                        ).joinToString(" · "),
                        Modifier.weight(1f)
                    )
                    Text(
                        "✕",
                        Modifier
                            .clickable(role = Role.Button) { onRemove(i) }
                            .semantics { contentDescription = remove }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        color = c.muted
                    )
                }
            }
        }
    }
}

@Composable
private fun eventText(e: GameEvent): String {
    val mark = if (e.made == true) "✓" else "✗"
    return when (e.type) {
        GameEventType.SHOT -> "${stringResource(zoneName.getValue(e.zone!!))} $mark"
        GameEventType.FREE_THROW -> "${stringResource(Res.string.game_free_throw)} $mark"
        GameEventType.OPPONENT -> stringResource(Res.string.game_opponent_points, e.value ?: 0)
        // ponytail: the other kinds have no button yet (#83).
        else -> e.type.name
    }
}

@Preview
@Composable
private fun GameLivePreview() = FuoriOrarioTheme {
    val draft = GameDraft(
        "d",
        LocalDate(2026, 10, 10),
        "Virtus",
        home = true,
        callUps = listOf(CallUp("luca", "Luca B.", "7"), CallUp("anna", "Anna", "4")),
        events = listOf(
            GameEvent(GameEventType.SHOT, Quarter.Q1, "luca", Zone.CEN, true),
            GameEvent(GameEventType.OPPONENT, Quarter.Q1, value = 2)
        ),
        selected = "luca"
    )
    Column { GameLive(draft, busy = false, GamesActions()) }
}
