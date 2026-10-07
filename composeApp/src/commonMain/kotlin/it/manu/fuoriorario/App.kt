package it.manu.fuoriorario

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.russhwolf.settings.Settings
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.brand_first
import fuoriorario.composeapp.generated.resources.brand_second
import fuoriorario.composeapp.generated.resources.ic_chevron_down
import fuoriorario.composeapp.generated.resources.sign_out
import fuoriorario.composeapp.generated.resources.subtitle_player
import fuoriorario.composeapp.generated.resources.subtitle_staff
import it.manu.fuoriorario.data.AuthRepository
import it.manu.fuoriorario.data.PlanRepository
import it.manu.fuoriorario.data.RosterRepository
import it.manu.fuoriorario.data.Session
import it.manu.fuoriorario.data.ShotRepository
import it.manu.fuoriorario.data.SupabaseAuthRepository
import it.manu.fuoriorario.data.SupabasePlanRepository
import it.manu.fuoriorario.data.SupabaseRosterRepository
import it.manu.fuoriorario.data.SupabaseShotRepository
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.rosterOrder
import it.manu.fuoriorario.ui.auth.LoginScreen
import it.manu.fuoriorario.ui.auth.PrivacyScreen
import it.manu.fuoriorario.ui.components.GhostButton
import it.manu.fuoriorario.ui.components.GhostStyle
import it.manu.fuoriorario.ui.components.Page
import it.manu.fuoriorario.ui.home.Home
import it.manu.fuoriorario.ui.theme.FuoriOrarioTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun App(
    auth: AuthRepository = remember { SupabaseAuthRepository() },
    roster: RosterRepository = remember { SupabaseRosterRepository() },
    shots: ShotRepository = remember { SupabaseShotRepository() },
    /** The device's own storage: only the staff's picked player. */
    prefs: Settings = remember { Settings() },
    plans: PlanRepository = remember { SupabasePlanRepository() }
) {
    FuoriOrarioTheme {
        val c = FuoriOrarioTheme.colors
        val session by auth.session.collectAsState(Session.Loading)
        val member = (session as? Session.SignedIn)?.member
        val scope = rememberCoroutineScope()
        val staff = member?.role == Role.STAFF

        // Staff's player menu (#12): the team's players, null while loading. Kept here so the pick survives tab changes,
        // and in [prefs] so it survives restarts.
        var players by remember(member?.id) { mutableStateOf<Result<List<Member>>?>(null) }
        var playersLoad by remember { mutableIntStateOf(0) }
        var pickedId by remember(member?.id) { mutableStateOf(prefs.getStringOrNull(PICKED_PLAYER)) }
        LaunchedEffect(member?.id, staff, playersLoad) {
            if (!staff) return@LaunchedEffect
            players = try {
                Result.success(roster.members().filter { it.role == Role.PLAYER }.rosterOrder())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        fun pick(player: Member) {
            pickedId = player.id
            prefs.putString(PICKED_PLAYER, player.id!!)
        }
        val followed = if (staff) {
            players?.getOrNull()?.let { list -> list.find { it.id == pickedId } ?: list.firstOrNull() }
        } else {
            member
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
                players?.getOrNull().takeIf { staff }.orEmpty(),
                followed,
                onPick = ::pick,
                onSignOut = { scope.launch { auth.signOut() } },
                Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 16.dp).widthIn(max = 560.dp)
            )
            if (member?.privacyAckAt != null) {
                Home(
                    member,
                    roster,
                    shots,
                    plans,
                    followed,
                    players,
                    onPick = ::pick,
                    onRetryPlayers = { playersLoad++ },
                    onRosterChanged = {
                        if (it.id == member.id) auth.refresh()
                        playersLoad++
                    }
                )
            } else {
                Page(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))) {
                    when (session) {
                        Session.Loading -> Unit
                        Session.SignedOut -> LoginScreen(auth)
                        is Session.SignedIn -> PrivacyScreen(auth)
                    }
                }
            }
        }
    }
}

private const val PICKED_PLAYER = "picked_player"

/** Prototype `.top`: brand + subtitle; when signed in, the `.who` row with logout and the name, or for staff with [players] the menu. */
@Composable
private fun Header(
    member: Member?,
    players: List<Member>,
    picked: Member?,
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
                GhostButton(stringResource(Res.string.sign_out), onSignOut, style = GhostStyle.PILL)
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
