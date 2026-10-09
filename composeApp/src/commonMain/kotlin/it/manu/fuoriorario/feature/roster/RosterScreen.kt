package it.manu.fuoriorario.feature.roster

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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.close
import fuoriorario.composeapp.generated.resources.member_add
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
import fuoriorario.composeapp.generated.resources.member_role
import fuoriorario.composeapp.generated.resources.member_save
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
import it.manu.fuoriorario.core.designsystem.Loader
import it.manu.fuoriorario.core.designsystem.LocalToast
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.designsystem.PrimaryButton
import it.manu.fuoriorario.core.designsystem.SegmentedControl
import it.manu.fuoriorario.core.designsystem.SelectField
import it.manu.fuoriorario.core.designsystem.show
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.MemberError
import it.manu.fuoriorario.domain.POSITIONS
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.rosterOrder
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val errorText = mapOf(
    MemberError.EMAIL_INVALID to Res.string.member_error_email,
    MemberError.EMAIL_TAKEN to Res.string.member_error_email_taken,
    MemberError.NAME_LENGTH to Res.string.member_error_name,
    MemberError.NUMBER_RANGE to Res.string.member_error_number
)

/**
 * Staff roster (PRD F2): players by number then name, then staff, then the add form. Tap a row to edit; "Togli" twice to remove.
 * [onChanged] runs after a member is added, edited or removed, so the player menu and the signed-in member catch up.
 */
@Composable
fun RosterScreen(onChanged: (Member) -> Unit, vm: RosterViewModel = koinViewModel()) {
    val uiState = vm.uiState.collectAsStateWithLifecycle()
    val toast = LocalToast.current
    LaunchedEffect(vm) { vm.toasts.collect { launch { toast.show(it) } } }
    val changed by rememberUpdatedState(onChanged)
    LaunchedEffect(vm) { vm.changes.collect { changed(it) } }
    RosterStateContent(
        uiState.value.state,
        RosterActions(
            onDraftChanged = vm::onDraftChanged,
            onAdd = vm::onAdd,
            onEdit = vm::onEdit,
            onEditChanged = vm::onEditChanged,
            onDismissEdit = vm::onDismissEdit,
            onSaveEdit = vm::onSaveEdit,
            onRemove = vm::onRemove
        )
    )
}

/** What the roster does, from the screen down to its sheet. */
class RosterActions(
    val onDraftChanged: (Member) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onEdit: (Member) -> Unit = {},
    val onEditChanged: (Member) -> Unit = {},
    val onDismissEdit: () -> Unit = {},
    val onSaveEdit: () -> Unit = {},
    val onRemove: (Member) -> Unit = {}
)

@Composable
fun RosterStateContent(state: UseCaseMutableState<RosterScreenState>?, actions: RosterActions) {
    when (state) {
        is UseCaseMutableState.Error -> state.handler.ErrorScreenContent()
        is UseCaseMutableState.ShowData -> RosterContent(state.items, actions)
        UseCaseMutableState.Loading, null -> Loader()
    }
}

/** Nothing until the team arrives. */
@Composable
fun RosterContent(state: RosterScreenState, actions: RosterActions) {
    val c = FuoriOrarioTheme.colors
    val team = state.members ?: return

    @Composable
    fun MemberList(title: StringResource, list: List<Member>) {
        Panel {
            Text(stringResource(title).uppercase(), style = MaterialTheme.typography.titleLarge)
            Column {
                list.forEachIndexed { i, member ->
                    if (i > 0) HorizontalDivider(color = c.line)
                    MemberRow(
                        member,
                        confirming = state.confirmingRemoval == member,
                        onEdit = { actions.onEdit(member) },
                        onRemove = { actions.onRemove(member) }
                    )
                }
            }
        }
    }

    val players = team.filter { it.role == Role.PLAYER }.rosterOrder()
    if (players.isNotEmpty()) MemberList(Res.string.roster_title, players)
    MemberList(Res.string.staff_title, team.filter { it.role == Role.STAFF }.rosterOrder())
    Panel {
        if (players.isEmpty()) {
            Text(stringResource(Res.string.roster_empty_title).uppercase(), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(Res.string.roster_empty_hint), color = c.muted)
        } else {
            Text(stringResource(Res.string.roster_add_title), style = MaterialTheme.typography.titleMedium)
        }
        AddMemberForm(state.draft, state.addError, state.busy, actions)
    }
    state.editing?.let { EditSheet(it, state.editError, state.busy, actions) }
}

@Preview
@Composable
private fun RosterContentPreview() = FuoriOrarioTheme {
    Column {
        RosterContent(
            RosterScreenState(
                listOf(
                    Member("Coach", Role.STAFF, email = "staff@example.com", id = "s"),
                    Member(
                        "Luca B.",
                        Role.PLAYER,
                        email = "luca@example.com",
                        jerseyNumber = "7",
                        position = "Guardia",
                        id = "l"
                    ),
                    Member("Marco R.", Role.PLAYER, email = "marco@example.com", jerseyNumber = "12", id = "m")
                )
            ),
            RosterActions()
        )
    }
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

/** The add form as typed, with its error under it. */
@Composable
private fun AddMemberForm(draft: Member, error: MemberError?, busy: Boolean, actions: RosterActions) {
    Field(
        label = stringResource(Res.string.member_email),
        value = draft.email,
        onValueChange = { actions.onDraftChanged(draft.copy(email = it)) },
        tag = "add_email",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
    )
    MemberFields("add", draft, actions.onDraftChanged)
    ErrorText(error)
    PrimaryButton(stringResource(Res.string.member_add), actions.onAdd, Modifier.fillMaxWidth(), enabled = !busy)
}

/** Prototype `.sheet` for editing everything but the email (it's the login): [draft] as typed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSheet(draft: Member, error: MemberError?, busy: Boolean, actions: RosterActions) {
    val c = FuoriOrarioTheme.colors
    ModalBottomSheet(
        actions.onDismissEdit,
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
                    Text(draft.email, style = MaterialTheme.typography.titleMedium)
                }
                GhostButton(stringResource(Res.string.close), actions.onDismissEdit)
            }
            MemberFields("edit", draft, actions.onEditChanged)
            ErrorText(error)
            PrimaryButton(
                stringResource(Res.string.member_save),
                actions.onSaveEdit,
                Modifier.fillMaxWidth(),
                enabled = !busy
            )
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
