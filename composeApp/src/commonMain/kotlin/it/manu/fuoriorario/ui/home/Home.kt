package it.manu.fuoriorario.ui.home

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.ic_tab_plan
import fuoriorario.composeapp.generated.resources.ic_tab_plays
import fuoriorario.composeapp.generated.resources.ic_tab_shot_log
import fuoriorario.composeapp.generated.resources.ic_tab_team
import fuoriorario.composeapp.generated.resources.roster_empty_hint
import fuoriorario.composeapp.generated.resources.roster_empty_title
import fuoriorario.composeapp.generated.resources.roster_title
import fuoriorario.composeapp.generated.resources.tab_plan
import fuoriorario.composeapp.generated.resources.tab_plays
import fuoriorario.composeapp.generated.resources.tab_shot_log
import fuoriorario.composeapp.generated.resources.tab_team
import fuoriorario.composeapp.generated.resources.team_overview
import fuoriorario.composeapp.generated.resources.team_settings
import it.manu.fuoriorario.core.today
import it.manu.fuoriorario.data.PlanRepository
import it.manu.fuoriorario.data.PlayRepository
import it.manu.fuoriorario.data.RosterRepository
import it.manu.fuoriorario.data.ShotRepository
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Period
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.weekOf
import it.manu.fuoriorario.ui.components.LoadFailed
import it.manu.fuoriorario.ui.components.LocalToast
import it.manu.fuoriorario.ui.components.Page
import it.manu.fuoriorario.ui.components.Panel
import it.manu.fuoriorario.ui.components.SegmentedControl
import it.manu.fuoriorario.ui.components.ToastHost
import it.manu.fuoriorario.ui.plan.PlanScreen
import it.manu.fuoriorario.ui.plays.PlaysScreen
import it.manu.fuoriorario.ui.roster.RosterScreen
import it.manu.fuoriorario.ui.settings.LibraryScreen
import it.manu.fuoriorario.ui.settings.ZoneRefsScreen
import it.manu.fuoriorario.ui.shots.ShotLogScreen
import it.manu.fuoriorario.ui.team.TeamOverviewScreen
import it.manu.fuoriorario.ui.theme.FuoriOrarioTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Bottom bar sections; [name] is the route. */
private enum class Tab(val label: StringResource, val icon: DrawableResource, val staffOnly: Boolean = false) {
    SHOT_LOG(Res.string.tab_shot_log, Res.drawable.ic_tab_shot_log),
    PLAN(Res.string.tab_plan, Res.drawable.ic_tab_plan),
    PLAYS(Res.string.tab_plays, Res.drawable.ic_tab_plays),
    TEAM(Res.string.tab_team, Res.drawable.ic_tab_team, staffOnly = true)
}

/** What the Squadra tab shows; [tag] is the test tag of its switch. */
private enum class TeamSection(val label: StringResource, val tag: String) {
    OVERVIEW(Res.string.team_overview, "team_overview"),
    ROSTER(Res.string.roster_title, "team_roster"),
    SETTINGS(Res.string.team_settings, "team_settings")
}

/**
 * Signed-in content under the header: tabs for the member's role, with the toast host. Players have no Squadra route at all.
 * [followed] is whose Diario di tiro and Piano to show: the player themselves, or the one staff picked from [players] (null while loading).
 * [onPick] makes staff follow a player, as their header menu does.
 */
@Composable
fun Home(
    member: Member,
    roster: RosterRepository,
    shots: ShotRepository,
    plans: PlanRepository,
    plays: PlayRepository,
    followed: Member?,
    players: Result<List<Member>>?,
    onPick: (Member) -> Unit,
    onRetryPlayers: () -> Unit,
    onRosterChanged: (Member) -> Unit
) {
    val tabs = Tab.entries.filter { !it.staffOnly || member.role == Role.STAFF }
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val toast = remember { SnackbarHostState() }
    var period by remember { mutableStateOf(Period.DAYS_30) }
    var week by remember { mutableStateOf(weekOf(today())) }
    var section by remember { mutableStateOf(TeamSection.OVERVIEW) }

    fun open(tab: Tab) = nav.navigate(tab.name) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    CompositionLocalProvider(LocalToast provides toast) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                // Tabs switch at once, like the prototype: a crossfade would show both screens at the same time.
                NavHost(
                    nav,
                    startDestination = Tab.SHOT_LOG.name,
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None }
                ) {
                    tabs.forEach { tab ->
                        composable(tab.name) {
                            Page {
                                when {
                                    tab == Tab.TEAM -> {
                                        SegmentedControl(
                                            TeamSection.entries.associateWith { it.label },
                                            section,
                                            { section = it },
                                            tag = { it.tag }
                                        )
                                        when (section) {
                                            TeamSection.OVERVIEW -> when {
                                                players?.isFailure == true -> LoadFailed(onRetryPlayers)
                                                players?.getOrNull()?.isEmpty() == true -> NoPlayers()
                                                else -> players?.getOrNull()?.let { list ->
                                                    TeamOverviewScreen(list, shots, plans) {
                                                        onPick(it)
                                                        open(Tab.SHOT_LOG)
                                                    }
                                                }
                                            }
                                            TeamSection.ROSTER -> RosterScreen(roster, onRosterChanged)
                                            TeamSection.SETTINGS -> {
                                                LibraryScreen(plans)
                                                ZoneRefsScreen(shots)
                                            }
                                        }
                                    }
                                    tab == Tab.PLAYS -> PlaysScreen(plays)
                                    // A fresh screen per player: no data or pending writes carried over.
                                    followed != null -> key(followed.id) {
                                        if (tab == Tab.SHOT_LOG) {
                                            ShotLogScreen(shots, followed, period) { period = it }
                                        } else {
                                            PlanScreen(plans, followed, member.role == Role.STAFF, week) { week = it }
                                        }
                                    }
                                    players?.isFailure == true -> LoadFailed(onRetryPlayers)
                                    players?.getOrNull()?.isEmpty() == true -> NoPlayers()
                                }
                            }
                        }
                    }
                }
                ToastHost(toast, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
            }
            TabBar(tabs, entry?.destination?.route, ::open)
        }
    }
}

/** Prototype `emptyRoster`, without the form: that's in Squadra. */
@Composable
private fun NoPlayers() {
    Panel {
        Text(stringResource(Res.string.roster_empty_title).uppercase(), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(Res.string.roster_empty_hint), color = FuoriOrarioTheme.colors.muted)
    }
}

/** Prototype `.tabbar`: surface strip with top border, selected tab in accent. */
@Composable
private fun TabBar(tabs: List<Tab>, current: String?, onSelect: (Tab) -> Unit) {
    val c = FuoriOrarioTheme.colors
    Column(Modifier.fillMaxWidth().background(c.surface)) {
        HorizontalDivider(color = c.line)
        Row(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .windowInsetsPadding(WindowInsets.safeDrawing.exclude(WindowInsets.ime).only(WindowInsetsSides.Bottom))
                .selectableGroup()
        ) {
            tabs.forEach { tab ->
                val selected = tab.name == current
                val color = if (selected) c.accent else c.muted
                Column(
                    Modifier
                        .weight(1f)
                        .selectable(selected, role = SemanticsRole.Tab, onClick = { onSelect(tab) })
                        .padding(start = 4.dp, top = 10.dp, end = 4.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(painterResource(tab.icon), null, Modifier.size(24.dp), tint = color)
                    Text(
                        stringResource(tab.label),
                        color = color,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
