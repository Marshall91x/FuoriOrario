package it.manu.fuoriorario.ui.roster

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.load_failed
import fuoriorario.composeapp.generated.resources.player_add
import fuoriorario.composeapp.generated.resources.player_added
import fuoriorario.composeapp.generated.resources.player_email
import fuoriorario.composeapp.generated.resources.player_error_email
import fuoriorario.composeapp.generated.resources.player_error_email_taken
import fuoriorario.composeapp.generated.resources.player_error_name
import fuoriorario.composeapp.generated.resources.player_error_number
import fuoriorario.composeapp.generated.resources.player_name
import fuoriorario.composeapp.generated.resources.player_number
import fuoriorario.composeapp.generated.resources.player_position
import fuoriorario.composeapp.generated.resources.retry
import fuoriorario.composeapp.generated.resources.roster_add_title
import fuoriorario.composeapp.generated.resources.roster_empty_hint
import fuoriorario.composeapp.generated.resources.roster_empty_title
import fuoriorario.composeapp.generated.resources.roster_title
import fuoriorario.composeapp.generated.resources.save_failed
import it.manu.fuoriorario.data.EmailTakenException
import it.manu.fuoriorario.data.RosterRepository
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.POSITIONS
import it.manu.fuoriorario.domain.PlayerError
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.newPlayerError
import it.manu.fuoriorario.domain.rosterOrder
import it.manu.fuoriorario.ui.components.Field
import it.manu.fuoriorario.ui.components.GhostButton
import it.manu.fuoriorario.ui.components.LocalToast
import it.manu.fuoriorario.ui.components.Panel
import it.manu.fuoriorario.ui.components.PrimaryButton
import it.manu.fuoriorario.ui.components.show
import it.manu.fuoriorario.ui.theme.FuoriOrarioTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

private val errorText = mapOf(
    PlayerError.EMAIL_INVALID to Res.string.player_error_email,
    PlayerError.EMAIL_TAKEN to Res.string.player_error_email_taken,
    PlayerError.NAME_LENGTH to Res.string.player_error_name,
    PlayerError.NUMBER_RANGE to Res.string.player_error_number
)

/** Staff roster (PRD F2): players by number then name, and the add-player form. Empty roster → prototype `emptyRoster()`. */
@Composable
fun RosterScreen(roster: RosterRepository) {
    val c = FuoriOrarioTheme.colors
    var members by remember { mutableStateOf<List<Member>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(attempt) {
        loadFailed = false
        try {
            members = roster.members()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            loadFailed = true
        }
    }

    val team = members
    if (loadFailed) {
        Panel(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(Res.string.load_failed), color = c.muted)
            GhostButton(stringResource(Res.string.retry), { attempt++ })
        }
    }
    if (team == null) return

    val players = team.filter { it.role == Role.PLAYER }.rosterOrder()
    if (players.isNotEmpty()) {
        Panel {
            Text(stringResource(Res.string.roster_title).uppercase(), style = MaterialTheme.typography.titleLarge)
            Column {
                players.forEachIndexed { i, player ->
                    if (i > 0) HorizontalDivider(color = c.line)
                    PlayerRow(player)
                }
            }
        }
    }
    // Same call site either way, so the form (and its pending toast) survives the first player turning the roster non-empty.
    Panel {
        if (players.isEmpty()) {
            Text(stringResource(Res.string.roster_empty_title).uppercase(), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(Res.string.roster_empty_hint), color = c.muted)
        } else {
            Text(stringResource(Res.string.roster_add_title), style = MaterialTheme.typography.titleMedium)
        }
        PlayerForm(team, roster) { members = team + it }
    }
}

/** Prototype `.item`: `#num` then name over position. */
@Composable
private fun PlayerRow(player: Member) {
    val c = FuoriOrarioTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            player.jerseyNumber?.let { "#$it" }.orEmpty(),
            Modifier.width(40.dp),
            color = c.muted,
            style = MaterialTheme.typography.titleMedium
        )
        Column(Modifier.weight(1f)) {
            Text(player.displayName, fontWeight = FontWeight.SemiBold)
            player.position?.let {
                Text(it, color = c.muted, style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp)
            }
        }
    }
}

/** Errors stay inline; a failed save toasts and keeps what was typed. */
@Composable
private fun PlayerForm(team: List<Member>, roster: RosterRepository, onAdded: (Member) -> Unit) {
    val c = FuoriOrarioTheme.colors
    val toast = LocalToast.current
    var email by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var number by rememberSaveable { mutableStateOf("") }
    var position by rememberSaveable { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<PlayerError?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (busy) return
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim()
        error = newPlayerError(cleanEmail, cleanName, number, team)
        if (error != null) return
        // "07" → "7", so the number reads the same everywhere.
        val member = Member(
            cleanName,
            Role.PLAYER,
            email = cleanEmail,
            jerseyNumber = number.toIntOrNull()?.toString(),
            position = position
        )
        scope.launch {
            busy = true
            try {
                roster.add(member)
                onAdded(member)
                email = ""
                name = ""
                number = ""
                position = null
                launch { toast.show(getString(Res.string.player_added, cleanName)) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: EmailTakenException) {
                error = PlayerError.EMAIL_TAKEN
            } catch (_: Exception) {
                launch { toast.show(getString(Res.string.save_failed)) }
            } finally {
                busy = false
            }
        }
    }

    Field(
        label = stringResource(Res.string.player_email),
        value = email,
        onValueChange = { email = it },
        tag = "player_email",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Field(
            label = stringResource(Res.string.player_name),
            value = name,
            onValueChange = { name = it },
            tag = "player_name",
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        Field(
            label = stringResource(Res.string.player_number),
            value = number,
            onValueChange = { new -> number = new.filter(Char::isDigit).take(2) },
            tag = "player_number",
            modifier = Modifier.width(80.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
        )
    }
    PositionField(position) { position = it }
    error?.let {
        Text(stringResource(errorText.getValue(it)), color = c.accent, style = MaterialTheme.typography.bodyMedium)
    }
    PrimaryButton(stringResource(Res.string.player_add), ::submit, Modifier.fillMaxWidth(), enabled = !busy)
}

/** Prototype `.field select`: optional, "—" for none. */
@Composable
private fun PositionField(value: String?, onChange: (String?) -> Unit) {
    val c = FuoriOrarioTheme.colors
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            stringResource(Res.string.player_position).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = c.muted
        )
        Box {
            Text(
                value ?: "—",
                Modifier
                    .fillMaxWidth()
                    .testTag("player_position")
                    .background(c.surface2, RoundedCornerShape(9.dp))
                    .border(1.dp, c.line, RoundedCornerShape(9.dp))
                    .clickable(role = SemanticsRole.DropdownList) { open = true }
                    .padding(horizontal = 11.dp, vertical = 9.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = c.ink
            )
            DropdownMenu(open, { open = false }, containerColor = c.surface) {
                (listOf(null) + POSITIONS).forEach { p ->
                    DropdownMenuItem(
                        text = { Text(p ?: "—", color = c.ink) },
                        onClick = {
                            onChange(p)
                            open = false
                        }
                    )
                }
            }
        }
    }
}
