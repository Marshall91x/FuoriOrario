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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.close
import fuoriorario.composeapp.generated.resources.last_staff
import fuoriorario.composeapp.generated.resources.load_failed
import fuoriorario.composeapp.generated.resources.member_edit
import fuoriorario.composeapp.generated.resources.member_remove
import fuoriorario.composeapp.generated.resources.member_remove_confirm
import fuoriorario.composeapp.generated.resources.member_removed
import fuoriorario.composeapp.generated.resources.member_role
import fuoriorario.composeapp.generated.resources.member_save
import fuoriorario.composeapp.generated.resources.member_saved
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
import fuoriorario.composeapp.generated.resources.role_player
import fuoriorario.composeapp.generated.resources.role_staff
import fuoriorario.composeapp.generated.resources.roster_add_title
import fuoriorario.composeapp.generated.resources.roster_empty_hint
import fuoriorario.composeapp.generated.resources.roster_empty_title
import fuoriorario.composeapp.generated.resources.roster_title
import fuoriorario.composeapp.generated.resources.save_denied
import fuoriorario.composeapp.generated.resources.save_failed
import fuoriorario.composeapp.generated.resources.staff_title
import it.manu.fuoriorario.data.EmailTakenException
import it.manu.fuoriorario.data.LastStaffException
import it.manu.fuoriorario.data.PermissionDeniedException
import it.manu.fuoriorario.data.RosterRepository
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.POSITIONS
import it.manu.fuoriorario.domain.PlayerError
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.memberError
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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

private val errorText = mapOf(
    PlayerError.EMAIL_INVALID to Res.string.player_error_email,
    PlayerError.EMAIL_TAKEN to Res.string.player_error_email_taken,
    PlayerError.NAME_LENGTH to Res.string.player_error_name,
    PlayerError.NUMBER_RANGE to Res.string.player_error_number
)

/** Staff roster (PRD F2): players by number then name, then staff, then the add form. Tap a row to edit; "Togli" twice to remove. */
@Composable
fun RosterScreen(roster: RosterRepository) {
    val c = FuoriOrarioTheme.colors
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    var members by remember { mutableStateOf<List<Member>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Member?>(null) }

    /** Id of the member whose "Togli" was tapped once. */
    var confirming by remember { mutableStateOf<String?>(null) }
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

    fun remove(member: Member) {
        if (confirming != member.id) {
            confirming = member.id
            return
        }
        confirming = null
        scope.launch {
            try {
                roster.remove(member)
                members = members.orEmpty().filter { it.id != member.id }
                launch { toast.show(getString(Res.string.member_removed, member.displayName)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                launch { toast.showSaveError(e) }
            }
        }
    }

    @Composable
    fun MemberList(title: StringResource, list: List<Member>) {
        Panel {
            Text(stringResource(title).uppercase(), style = MaterialTheme.typography.titleLarge)
            Column {
                list.forEachIndexed { i, member ->
                    if (i > 0) HorizontalDivider(color = c.line)
                    MemberRow(
                        member,
                        confirming = confirming == member.id,
                        onEdit = {
                            confirming = null
                            editing = member
                        },
                        onRemove = { remove(member) }
                    )
                }
            }
        }
    }

    val players = team.filter { it.role == Role.PLAYER }.rosterOrder()
    if (players.isNotEmpty()) MemberList(Res.string.roster_title, players)
    MemberList(Res.string.staff_title, team.filter { it.role == Role.STAFF }.rosterOrder())
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
    editing?.let { member ->
        EditSheet(member, roster, onDismiss = { editing = null }) { saved ->
            members = members.orEmpty().map { if (it.id == saved.id) saved else it }
            editing = null
            // Here, not in the sheet: closing it cancels its scope.
            scope.launch { toast.show(getString(Res.string.member_saved)) }
        }
    }
}

/** Toast for a failed write, by cause. */
private suspend fun SnackbarHostState.showSaveError(e: Exception) = show(
    getString(
        when (e) {
            is LastStaffException -> Res.string.last_staff
            is PermissionDeniedException -> Res.string.save_denied
            else -> Res.string.save_failed
        }
    )
)

/** Prototype `.item`: `#num` then name over position, and the "Togli" → "Conferma" button. */
@Composable
private fun MemberRow(member: Member, confirming: Boolean, onEdit: () -> Unit, onRemove: () -> Unit) {
    val c = FuoriOrarioTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            member.jerseyNumber?.let { "#$it" }.orEmpty(),
            Modifier.width(40.dp),
            color = c.muted,
            style = MaterialTheme.typography.titleMedium
        )
        Column(Modifier.weight(1f)) {
            Text(member.displayName, fontWeight = FontWeight.SemiBold)
            member.position?.let {
                Text(it, color = c.muted, style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp)
            }
        }
        GhostButton(
            stringResource(if (confirming) Res.string.member_remove_confirm else Res.string.member_remove),
            onRemove,
            Modifier.testTag("remove_${member.id}"),
            danger = confirming
        )
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
    var role by rememberSaveable { mutableStateOf(Role.PLAYER) }
    var error by remember { mutableStateOf<PlayerError?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (busy) return
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim()
        error = newPlayerError(cleanEmail, cleanName, number, team)
        if (error != null) return
        val member = Member(
            cleanName,
            role,
            email = cleanEmail,
            jerseyNumber = cleanNumber(number),
            position = position
        )
        scope.launch {
            busy = true
            try {
                onAdded(roster.add(member))
                email = ""
                name = ""
                number = ""
                position = null
                role = Role.PLAYER
                launch { toast.show(getString(Res.string.player_added, cleanName)) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: EmailTakenException) {
                error = PlayerError.EMAIL_TAKEN
            } catch (e: Exception) {
                launch { toast.showSaveError(e) }
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
    MemberFields("player", name, {
        name = it
    }, number, { number = it }, position, { position = it }, role, { role = it })
    error?.let {
        Text(stringResource(errorText.getValue(it)), color = c.accent, style = MaterialTheme.typography.bodyMedium)
    }
    PrimaryButton(stringResource(Res.string.player_add), ::submit, Modifier.fillMaxWidth(), enabled = !busy)
}

/** "07" → "7", so the number reads the same everywhere; null when empty. */
private fun cleanNumber(number: String) = number.toIntOrNull()?.toString()

/** Prototype `.sheet` for editing everything but the email (it's the login). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSheet(member: Member, roster: RosterRepository, onDismiss: () -> Unit, onSaved: (Member) -> Unit) {
    val c = FuoriOrarioTheme.colors
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(member.displayName) }
    var number by rememberSaveable { mutableStateOf(member.jerseyNumber.orEmpty()) }
    var position by rememberSaveable { mutableStateOf(member.position) }
    var role by rememberSaveable { mutableStateOf(member.role) }
    var error by remember { mutableStateOf<PlayerError?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun save() {
        if (busy) return
        val cleanName = name.trim()
        error = memberError(cleanName, number)
        if (error != null) return
        val saved = member.copy(
            displayName = cleanName,
            jerseyNumber = cleanNumber(number),
            position = position,
            role = role
        )
        scope.launch {
            busy = true
            try {
                roster.update(saved)
                onSaved(saved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                launch { toast.showSaveError(e) }
            } finally {
                busy = false
            }
        }
    }

    ModalBottomSheet(
        onDismiss,
        containerColor = c.surface,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        dragHandle = null
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp, 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.member_edit).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.muted
                    )
                    Text(member.email, style = MaterialTheme.typography.titleMedium)
                }
                GhostButton(stringResource(Res.string.close), onDismiss)
            }
            MemberFields("edit", name, { name = it }, number, { number = it }, position, { position = it }, role, {
                role =
                    it
            })
            error?.let {
                Text(
                    stringResource(errorText.getValue(it)),
                    color = c.accent,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            PrimaryButton(stringResource(Res.string.member_save), ::save, Modifier.fillMaxWidth(), enabled = !busy)
        }
    }
}

/** Name, number, position and role, shared by the add form and the edit sheet. Test tags start with [tag]. */
@Composable
private fun MemberFields(
    tag: String,
    name: String,
    onName: (String) -> Unit,
    number: String,
    onNumber: (String) -> Unit,
    position: String?,
    onPosition: (String?) -> Unit,
    role: Role,
    onRole: (Role) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Field(
            label = stringResource(Res.string.player_name),
            value = name,
            onValueChange = onName,
            tag = "${tag}_name",
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        Field(
            label = stringResource(Res.string.player_number),
            value = number,
            onValueChange = { onNumber(it.filter(Char::isDigit).take(2)) },
            tag = "${tag}_number",
            modifier = Modifier.width(80.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
        )
    }
    PositionField(tag, position, onPosition)
    RoleField(tag, role, onRole)
}

/** Prototype `.seg`: Giocatore | Staff. */
@Composable
private fun RoleField(tag: String, value: Role, onChange: (Role) -> Unit) {
    val c = FuoriOrarioTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            stringResource(Res.string.member_role).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = c.muted
        )
        Row(
            Modifier
                .background(c.surface2, RoundedCornerShape(50))
                .border(1.dp, c.line, RoundedCornerShape(50))
                .padding(3.dp)
                .selectableGroup()
        ) {
            listOf(
                Role.PLAYER to Res.string.role_player,
                Role.STAFF to Res.string.role_staff
            ).forEach { (role, label) ->
                val selected = role == value
                Text(
                    stringResource(label),
                    Modifier
                        .testTag("${tag}_role_${role.name.lowercase()}")
                        .background(if (selected) c.ink else Color.Transparent, RoundedCornerShape(50))
                        .selectable(selected, role = SemanticsRole.RadioButton) { onChange(role) }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    color = if (selected) c.bg else c.muted,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** Prototype `.field select`: optional, "—" for none. */
@Composable
private fun PositionField(tag: String, value: String?, onChange: (String?) -> Unit) {
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
                    .testTag("${tag}_position")
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
