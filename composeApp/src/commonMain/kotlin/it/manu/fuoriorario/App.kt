package it.manu.fuoriorario

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.brand_first
import fuoriorario.composeapp.generated.resources.brand_second
import fuoriorario.composeapp.generated.resources.ic_chevron_down
import fuoriorario.composeapp.generated.resources.sign_out
import fuoriorario.composeapp.generated.resources.sign_out_confirm
import fuoriorario.composeapp.generated.resources.sign_out_draft
import fuoriorario.composeapp.generated.resources.subtitle_player
import fuoriorario.composeapp.generated.resources.subtitle_staff
import it.manu.fuoriorario.core.designsystem.GhostButton
import it.manu.fuoriorario.core.designsystem.GhostStyle
import it.manu.fuoriorario.core.designsystem.Loader
import it.manu.fuoriorario.core.designsystem.Page
import it.manu.fuoriorario.core.navigation.Home
import it.manu.fuoriorario.core.navigation.Route
import it.manu.fuoriorario.core.session.Session
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.di.appKoin
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.feature.auth.LoginScreen
import it.manu.fuoriorario.feature.auth.PrivacyScreen
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.KoinApplication

@Composable
fun App(koin: KoinApplication = appKoin) {
    KoinIsolatedContext(koin) {
        FuoriOrarioTheme { MainScreen() }
    }
}

@Composable
private fun MainScreen(vm: MainViewModel = koinViewModel()) {
    val uiState = vm.uiState.collectAsStateWithLifecycle()
    MainStateContent(uiState.value.state, vm::onPick, vm::onSignOut, vm::onRetryPlayers, vm::onRosterChanged)
}

@Composable
private fun MainStateContent(
    state: UseCaseMutableState<MainScreenState>?,
    onPick: (Member) -> Unit,
    onSignOut: () -> Unit,
    onRetryPlayers: () -> Unit,
    onRosterChanged: (Member) -> Unit
) {
    when (state) {
        is UseCaseMutableState.Error -> state.handler.ErrorScreenContent()
        is UseCaseMutableState.ShowData -> MainContent(state.items, onPick, onSignOut, onRetryPlayers, onRosterChanged)
        // Restoring the saved session: the header and the loader.
        UseCaseMutableState.Loading, null -> MainContent(
            MainScreenState(),
            onPick,
            onSignOut,
            onRetryPlayers,
            onRosterChanged
        )
    }
}

/** Header, then the destination the session picks: each one keeps its ViewModels until the session leaves it. */
@Composable
private fun MainContent(
    state: MainScreenState,
    onPick: (Member) -> Unit,
    onSignOut: () -> Unit,
    onRetryPlayers: () -> Unit,
    onRosterChanged: (Member) -> Unit
) {
    val c = FuoriOrarioTheme.colors
    val member = state.member
    val staff = member?.role == Role.STAFF
    val nav = rememberNavController()
    LaunchedEffect(state.route) {
        if (nav.currentDestination?.route != state.route.name) {
            nav.navigate(state.route.name) { popUpTo(nav.graph.id) { inclusive = true } }
        }
    }

    // Bottom inset is left to each screen: the home's tab bar runs to the bottom edge.
    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
    ) {
        // Stays put while the content scrolls, like the prototype's sticky `.top`.
        Header(
            member,
            state.players?.getOrNull().takeIf { staff }.orEmpty(),
            state.followed,
            state.confirmingSignOut,
            onPick = onPick,
            onSignOut = onSignOut,
            Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 16.dp).widthIn(max = 560.dp)
        )
        val bottomInset = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        NavHost(
            nav,
            startDestination = Route.LOADING.name,
            modifier = Modifier.weight(1f),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            composable(Route.LOADING.name) { Page(bottomInset) { Loader() } }
            composable(Route.LOGIN.name) {
                Page(bottomInset) { LoginScreen(expired = state.session == Session.Expired) }
            }
            composable(Route.PRIVACY.name) { Page(bottomInset) { PrivacyScreen() } }
            composable(Route.HOME.name) {
                member?.let { Home(it, state.followed, state.players, onPick, onRetryPlayers, onRosterChanged) }
            }
        }
    }
}

/**
 * Prototype `.top`: brand + subtitle; when signed in, the `.who` row with logout and the name, or for staff with
 * [players] the menu. [confirmingSignOut]: logging out would lose the game in progress, a second tap does it.
 */
@Composable
private fun Header(
    member: Member?,
    players: List<Member>,
    picked: Member?,
    confirmingSignOut: Boolean,
    onPick: (Member) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = FuoriOrarioTheme.colors
    Column(modifier.fillMaxWidth().padding(top = 14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val first = stringResource(Res.string.brand_first).uppercase()
            val second = stringResource(Res.string.brand_second).uppercase()
            Text(
                buildAnnotatedString {
                    append("$first ")
                    withStyle(SpanStyle(color = c.accent)) { append(second) }
                },
                modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.headlineMedium,
                color = c.ink
            )
            val subtitle = if (member?.role == Role.STAFF) Res.string.subtitle_staff else Res.string.subtitle_player
            Text(
                stringResource(subtitle).uppercase(),
                modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.labelSmall,
                color = c.muted
            )
        }
        if (member != null) {
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (players.isEmpty()) {
                    Text(member.displayName, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                } else {
                    PlayerPicker(players, picked, onPick, Modifier.weight(1f))
                }
                GhostButton(
                    stringResource(if (confirmingSignOut) Res.string.sign_out_confirm else Res.string.sign_out),
                    onSignOut,
                    Modifier.testTag("sign_out"),
                    style = if (confirmingSignOut) GhostStyle.DANGER else GhostStyle.PILL
                )
            }
            if (confirmingSignOut) {
                Text(
                    stringResource(Res.string.sign_out_draft),
                    Modifier.padding(top = 6.dp),
                    color = c.accent,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        HorizontalDivider(Modifier.padding(top = 10.dp), color = c.line)
    }
}

/** "#7 · Luca B.", or just the name without a number. */
private val Member.menuLabel get() = jerseyNumber?.let { "#$it · " }.orEmpty() + displayName

/** Prototype `.who select`: the player staff are following, picked from the roster. */
@Composable
private fun PlayerPicker(
    players: List<Member>,
    picked: Member?,
    onPick: (Member) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = FuoriOrarioTheme.colors
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .testTag("player_picker")
                .background(c.surface, RoundedCornerShape(10.dp))
                .border(1.dp, c.line, RoundedCornerShape(10.dp))
                .clickable(role = SemanticsRole.DropdownList) { open = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                picked?.menuLabel.orEmpty(),
                Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(painterResource(Res.drawable.ic_chevron_down), null, Modifier.size(18.dp), tint = c.muted)
        }
        DropdownMenu(open, { open = false }, containerColor = c.surface) {
            players.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.menuLabel, color = c.ink) },
                    onClick = {
                        onPick(p)
                        open = false
                    },
                    modifier = Modifier.testTag("player_option")
                )
            }
        }
    }
}
