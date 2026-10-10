package it.manu.fuoriorario.feature.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.game_all
import fuoriorario.composeapp.generated.resources.game_box
import fuoriorario.composeapp.generated.resources.game_box_assists
import fuoriorario.composeapp.generated.resources.game_box_fouls
import fuoriorario.composeapp.generated.resources.game_box_free_throws
import fuoriorario.composeapp.generated.resources.game_box_player
import fuoriorario.composeapp.generated.resources.game_box_points
import fuoriorario.composeapp.generated.resources.game_box_rebounds
import fuoriorario.composeapp.generated.resources.game_box_steals
import fuoriorario.composeapp.generated.resources.game_box_threes
import fuoriorario.composeapp.generated.resources.game_box_turnovers
import fuoriorario.composeapp.generated.resources.game_box_twos
import fuoriorario.composeapp.generated.resources.game_delete
import fuoriorario.composeapp.generated.resources.game_delete_confirm
import fuoriorario.composeapp.generated.resources.game_map_team
import fuoriorario.composeapp.generated.resources.game_whole
import it.manu.fuoriorario.core.designsystem.GhostButton
import it.manu.fuoriorario.core.designsystem.GhostStyle
import it.manu.fuoriorario.core.designsystem.Loader
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.designsystem.SegmentedControl
import it.manu.fuoriorario.core.designsystem.short
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.domain.BoxLine
import it.manu.fuoriorario.domain.CallUp
import it.manu.fuoriorario.domain.Game
import it.manu.fuoriorario.domain.GameEvent
import it.manu.fuoriorario.domain.GameEventType
import it.manu.fuoriorario.domain.Quarter
import it.manu.fuoriorario.domain.Score
import it.manu.fuoriorario.domain.Shots
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.domain.quarterScores
import it.manu.fuoriorario.feature.shots.ShotMap
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

// Keyed by the cells' test tag suffix.
private val boxColumns = mapOf(
    "points" to Res.string.game_box_points,
    "twos" to Res.string.game_box_twos,
    "threes" to Res.string.game_box_threes,
    "free_throws" to Res.string.game_box_free_throws,
    "rebounds" to Res.string.game_box_rebounds,
    "assists" to Res.string.game_box_assists,
    "turnovers" to Res.string.game_box_turnovers,
    "steals" to Res.string.game_box_steals,
    "fouls" to Res.string.game_box_fouls
)

private val NAME_WIDTH = 110.dp
private val CELL_WIDTH = 44.dp

/**
 * Riepilogo (PRD F8): score, parziali, tabellino and shot map, filtered by quarter. Staff see every convocato and
 * delete the game with a second tap; a player sees the final score, their own row and their own map.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Summary(summary: GameSummary, staff: Boolean, busy: Boolean, actions: GamesActions) {
    // ponytail: deprecated, NavigationEventHandler when its compose artifact is among our dependencies (as in PlayEditor).
    @Suppress("DEPRECATION")
    BackHandler(onBack = actions.onCloseSummary)
    val game = summary.game
    GhostButton(stringResource(Res.string.game_all), actions.onCloseSummary, Modifier.testTag("game_back"))
    Text(
        "${game.date.short()} · ${homeLabel(game.home)}",
        color = FuoriOrarioTheme.colors.muted,
        style = MaterialTheme.typography.bodyMedium
    )
    if (!summary.loaded) return Loader()
    // A player reads only their own events: no parziali, and the final score whatever the quarter.
    if (staff) {
        Scoreboard(summary.score, game.opponent, quarterScores(summary.events))
    } else {
        Scoreboard(Score(game.ourScore, game.theirScore), game.opponent, null)
    }
    SegmentedControl(
        summary.quarters.associateWith { quarterName[it] ?: Res.string.game_whole },
        summary.quarter,
        actions.onSummaryQuarter,
        tag = { "summary_${it?.name?.lowercase() ?: "whole"}" }
    )
    BoxScore(summary.box, summary.member, actions.onBoxRow)
    Panel {
        val member = summary.callUps.find { it.id == summary.member }
        Text(
            member?.label ?: stringResource(Res.string.game_map_team),
            color = FuoriOrarioTheme.colors.muted,
            style = MaterialTheme.typography.bodyMedium
        )
        ShotMap(summary.zones, summary.refs)
    }
    if (staff) {
        GhostButton(
            stringResource(if (summary.confirmingRemoval) Res.string.game_delete_confirm else Res.string.game_delete),
            actions.onDelete,
            Modifier.testTag("game_delete"),
            style = if (summary.confirmingRemoval) GhostStyle.DANGER else GhostStyle.PLAIN,
            enabled = !busy
        )
    }
}

/** A row per convocato, scrolling sideways on a narrow screen; tapping one shows their map, [selected] stands out. */
@Composable
private fun BoxScore(lines: List<BoxLine>, selected: String?, onRow: (String) -> Unit) {
    val c = FuoriOrarioTheme.colors
    Panel {
        Text(stringResource(Res.string.game_box).uppercase(), style = MaterialTheme.typography.titleMedium)
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            BoxRow(stringResource(Res.string.game_box_player), boxColumns.values.map { stringResource(it) }, c.muted)
            lines.forEach { line ->
                HorizontalDivider(Modifier.width(NAME_WIDTH + CELL_WIDTH * boxColumns.size), color = c.line)
                BoxRow(
                    line.callUp.label,
                    line.cells(),
                    c.ink,
                    Modifier
                        .background(
                            if (line.callUp.id == selected) c.surface2 else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onRow(line.callUp.id) },
                    tag = "box_${line.callUp.id}"
                )
            }
        }
    }
}

/** PT, then made/attempted, then the counts. */
private fun BoxLine.cells() = listOf(points.toString(), twos.ratio, threes.ratio, freeThrows.ratio) +
    listOf(rebounds, assists, turnovers, steals, fouls).map { it.toString() }

private val Shots.ratio get() = "$made/$attempted"

@Composable
private fun BoxRow(
    name: String,
    cells: List<String>,
    color: Color,
    modifier: Modifier = Modifier,
    tag: String? = null
) {
    Row(
        modifier.then(tag?.let { Modifier.testTag(it) } ?: Modifier).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            Modifier.width(NAME_WIDTH),
            color = color,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        cells.zip(boxColumns.keys).forEach { (cell, key) ->
            val cellTag = tag?.let { "${it}_$key" }
            Text(
                cell,
                Modifier.width(CELL_WIDTH).then(cellTag?.let { Modifier.testTag(it) } ?: Modifier),
                color = color,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview
@Composable
private fun SummaryPreview() = FuoriOrarioTheme {
    val game = Game(LocalDate(2026, 10, 3), "Virtus", home = true, ourScore = 5, theirScore = 2, id = "g")
    val events = listOf(
        GameEvent(GameEventType.SHOT, Quarter.Q1, "luca", Zone.CEN, true),
        GameEvent(GameEventType.OPPONENT, Quarter.Q1, value = 2),
        GameEvent(GameEventType.SHOT, Quarter.Q2, "anna", Zone.PIT, true),
        GameEvent(GameEventType.REBOUND, Quarter.Q2, "anna")
    )
    val callUps = listOf(CallUp("anna", "Anna", "4"), CallUp("luca", "Luca B.", "7"))
    Column { Summary(GameSummary(game, callUps, events, loaded = true), staff = true, busy = false, GamesActions()) }
}
