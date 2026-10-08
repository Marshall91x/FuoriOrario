package it.manu.fuoriorario.ui.roster

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.close
import fuoriorario.composeapp.generated.resources.member_add
import fuoriorario.composeapp.generated.resources.member_added
import fuoriorario.composeapp.generated.resources.member_edit
import fuoriorario.composeapp.generated.resources.member_email
import fuoriorario.composeapp.generated.resources.member_error_email
import fuoriorario.composeapp.generated.resources.member_error_email_taken
import fuoriorario.composeapp.generated.resources.member_error_name
import fuoriorario.composeapp.generated.resources.member_error_number
import fuoriorario.composeapp.generated.resources.member_name
import fuoriorario.composeapp.generated.resources.member_number
import fuoriorario.composeapp.generated.resources.member_position
import fuoriorario.composeapp.generated.resources.member_remove
import fuoriorario.composeapp.generated.resources.member_remove_confirm
import fuoriorario.composeapp.generated.resources.member_removed
import fuoriorario.composeapp.generated.resources.member_role
import fuoriorario.composeapp.generated.resources.member_save
import fuoriorario.composeapp.generated.resources.member_saved
import fuoriorario.composeapp.generated.resources.role_player
import fuoriorario.composeapp.generated.resources.role_staff
import fuoriorario.composeapp.generated.resources.roster_add_title
import fuoriorario.composeapp.generated.resources.roster_empty_hint
import fuoriorario.composeapp.generated.resources.roster_empty_title
import fuoriorario.composeapp.generated.resources.roster_title
import fuoriorario.composeapp.generated.resources.staff_title
import it.manu.fuoriorario.core.designsystem.Field
import it.manu.fuoriorario.core.designsystem.GhostButton
import it.manu.fuoriorario.core.designsystem.GhostStyle
import it.manu.fuoriorario.core.designsystem.LoadFailed
import it.manu.fuoriorario.core.designsystem.LocalToast
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.designsystem.PrimaryButton
import it.manu.fuoriorario.core.designsystem.SegmentedControl
import it.manu.fuoriorario.core.designsystem.SelectField
import it.manu.fuoriorario.core.designsystem.launchWrite
import it.manu.fuoriorario.core.designsystem.show
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.data.EmailTakenException
import it.manu.fuoriorario.data.RosterRepository
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.MemberError
import it.manu.fuoriorario.domain.POSITIONS
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.cleaned
import it.manu.fuoriorario.domain.memberError
import it.manu.fuoriorario.domain.newMemberError
import it.manu.fuoriorario.domain.rosterOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

private val errorText = mapOf(
    MemberError.EMAIL_INVALID to Res.string.member_error_email,
    MemberError.EMAIL_TAKEN to Res.string.member_error_email_taken,
    MemberError.NAME_LENGTH to Res.string.member_error_name,
    MemberError.NUMBER_RANGE to Res.string.member_error_number
)

/** The add form's empty state; survives configuration changes as JSON. */
private val blankMember = Member("", Role.PLAYER)
private val MemberSaver =
    Saver<Member, String>({
        Json.encodeToString(Member.serializer(), it)
    }, { Json.decodeFromString(Member.serializer(), it) })

/**
 * Staff roster (PRD F2): players by number then name, then staff, then the add form. Tap a row to edit; "Togli" twice to remove.
 * [onChanged] runs after a member is added, edited or removed, so the player menu and the signed-in member catch up.
 */
@Composable
fun RosterScreen(roster: RosterRepository, onChanged: (Member) -> Unit) {
    val c = FuoriOrarioTheme.colors
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    var members by remember { mutableStateOf<List<Member>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Member?>(null) }
    var confirmingRemoval by remember { mutableStateOf<Member?>(null) }

    /** An edit or removal is in flight. */
    var busy by remember { mutableStateOf(false) }
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
        LoadFailed { attempt++ }
    }
    if (team == null) return

    fun write(action: suspend CoroutineScope.() -> Unit) {
        busy = true
        scope.launchWrite(toast, onDone = { busy = false }, action)
    }

    fun remove(member: Member) {
        if (busy) return
        if (confirmingRemoval != member) {
            confirmingRemoval = member
            return
        }
        confirmingRemoval = null
        write {
            roster.remove(member)
            members = members.orEmpty().filter { it.id != member.id }
            launch { toast.show(getString(Res.string.member_removed, member.displayName)) }
            onChanged(member)
        }
    }

    fun save(edited: Member) {
        if (busy) return
        write {
            roster.update(edited)
            members = members.orEmpty().map { if (it.id == edited.id) edited else it }
            editing = null
            launch { toast.show(getString(Res.string.member_saved)) }
            onChanged(edited)
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
                        confirming = confirmingRemoval == member,
                        onEdit = {
                            confirmingRemoval = null
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
        AddMemberForm(team, roster) {
            members = team + it
            onChanged(it)
        }
    }
    editing?.let { EditSheet(it, busy, onDismiss = { editing = null }, onSave = ::save) }
}

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
            if (confirming) GhostStyle.DANGER else GhostStyle.PLAIN
        )
    }
}

/** Errors stay inline; a failed save toasts and keeps what was typed. */
@Composable
private fun AddMemberForm(team: List<Member>, roster: RosterRepository, onAdded: (Member) -> Unit) {
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    var draft by rememberSaveable(stateSaver = MemberSaver) { mutableStateOf(blankMember) }
    var error by remember { mutableStateOf<MemberError?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun submit() {
        if (busy) return
        val member = draft.cleaned()
        error = newMemberError(member.email, member.displayName, draft.jerseyNumber.orEmpty(), team)
        if (error != null) return
        busy = true
        scope.launchWrite(toast, onDone = { busy = false }) {
            try {
                onAdded(roster.add(member))
            } catch (_: EmailTakenException) {
                error = MemberError.EMAIL_TAKEN
                return@launchWrite
            }
            draft = blankMember
            launch { toast.show(getString(Res.string.member_added, member.displayName)) }
        }
    }

    Field(
        label = stringResource(Res.string.member_email),
        value = draft.email,
        onValueChange = { draft = draft.copy(email = it) },
        tag = "add_email",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
    )
    MemberFields("add", draft) { draft = it }
    ErrorText(error)
    PrimaryButton(stringResource(Res.string.member_add), ::submit, Modifier.fillMaxWidth(), enabled = !busy)
}

/** Prototype `.sheet` for editing everything but the email (it's the login). The screen saves, so the toast outlives the sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSheet(member: Member, busy: Boolean, onDismiss: () -> Unit, onSave: (Member) -> Unit) {
    val c = FuoriOrarioTheme.colors
    var draft by remember { mutableStateOf(member) }
    var error by remember { mutableStateOf<MemberError?>(null) }

    fun save() {
        val edited = draft.cleaned()
        error = memberError(edited.displayName, draft.jerseyNumber.orEmpty())
        if (error == null) onSave(edited)
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
            MemberFields("edit", draft) { draft = it }
            ErrorText(error)
            PrimaryButton(stringResource(Res.string.member_save), ::save, Modifier.fillMaxWidth(), enabled = !busy)
        }
    }
}

@Composable
private fun ErrorText(error: MemberError?) {
    error?.let {
        Text(
            stringResource(errorText.getValue(it)),
            color = FuoriOrarioTheme.colors.accent,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** Name, number, position and role of [draft] as typed, shared by the add form and the edit sheet. Test tags start with [tag]. */
@Composable
private fun MemberFields(tag: String, draft: Member, onChange: (Member) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Field(
            label = stringResource(Res.string.member_name),
            value = draft.displayName,
            onValueChange = { onChange(draft.copy(displayName = it)) },
            tag = "${tag}_name",
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        Field(
            label = stringResource(Res.string.member_number),
            value = draft.jerseyNumber.orEmpty(),
            onValueChange = { onChange(draft.copy(jerseyNumber = it.filter(Char::isDigit).take(2))) },
            tag = "${tag}_number",
            modifier = Modifier.width(80.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
        )
    }
    // Optional: "—" for none.
    SelectField(
        stringResource(Res.string.member_position),
        draft.position,
        listOf(null) + POSITIONS,
        { it ?: "—" },
        "${tag}_position"
    ) { onChange(draft.copy(position = it)) }
    RoleField(tag, draft.role) { onChange(draft.copy(role = it)) }
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
        SegmentedControl(
            mapOf(Role.PLAYER to Res.string.role_player, Role.STAFF to Res.string.role_staff),
            value,
            onChange,
            tag = { "${tag}_role_${it.name.lowercase()}" }
        )
    }
}
