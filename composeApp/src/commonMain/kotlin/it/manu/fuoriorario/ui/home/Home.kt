package it.manu.fuoriorario.ui.home

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
import androidx.compose.runtime.remember
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
import fuoriorario.composeapp.generated.resources.ic_tab_shot_log
import fuoriorario.composeapp.generated.resources.ic_tab_team
import fuoriorario.composeapp.generated.resources.placeholder_plan
import fuoriorario.composeapp.generated.resources.placeholder_shot_log
import fuoriorario.composeapp.generated.resources.placeholder_title
import fuoriorario.composeapp.generated.resources.tab_plan
import fuoriorario.composeapp.generated.resources.tab_shot_log
import fuoriorario.composeapp.generated.resources.tab_team
import it.manu.fuoriorario.data.RosterRepository
import it.manu.fuoriorario.data.ShotRepository
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.ui.components.LocalToast
import it.manu.fuoriorario.ui.components.Page
import it.manu.fuoriorario.ui.components.Panel
import it.manu.fuoriorario.ui.components.ToastHost
import it.manu.fuoriorario.ui.roster.RosterScreen
import it.manu.fuoriorario.ui.shots.ShotLogScreen
import it.manu.fuoriorario.ui.theme.FuoriOrarioTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Bottom bar sections; [name] is the route. [placeholder] until the section is built. */
private enum class Tab(
    val label: StringResource,
    val icon: DrawableResource,
    val placeholder: StringResource?,
    val staffOnly: Boolean = false
) {
    SHOT_LOG(Res.string.tab_shot_log, Res.drawable.ic_tab_shot_log, Res.string.placeholder_shot_log),
    PLAN(Res.string.tab_plan, Res.drawable.ic_tab_plan, Res.string.placeholder_plan),
    TEAM(Res.string.tab_team, Res.drawable.ic_tab_team, null, staffOnly = true)
}

/** Signed-in content under the header: tabs for the member's role, with the toast host. Players have no Squadra route at all. */
@Composable
fun Home(member: Member, roster: RosterRepository, shots: ShotRepository, onSelfChanged: () -> Unit) {
    val tabs = Tab.entries.filter { !it.staffOnly || member.role == Role.STAFF }
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val toast = remember { SnackbarHostState() }

    CompositionLocalProvider(LocalToast provides toast) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                NavHost(nav, startDestination = Tab.SHOT_LOG.name) {
                    tabs.forEach { tab ->
                        composable(tab.name) {
                            Page {
                                when {
                                    tab == Tab.TEAM -> RosterScreen(roster, member, onSelfChanged)
                                    // Staff get a player picker first (#12).
                                    tab == Tab.SHOT_LOG && member.role == Role.PLAYER -> ShotLogScreen(shots, member)
                                    else -> Placeholder(tab)
                                }
                            }
                        }
                    }
                }
                ToastHost(toast, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
            }
            TabBar(tabs, entry?.destination?.route) { tab ->
                nav.navigate(tab.name) {
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }
}

@Composable
private fun Placeholder(tab: Tab) {
    Panel(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(Res.string.placeholder_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(tab.placeholder!!), color = FuoriOrarioTheme.colors.muted)
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
