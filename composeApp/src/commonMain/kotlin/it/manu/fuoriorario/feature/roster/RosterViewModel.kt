package it.manu.fuoriorario.feature.roster

import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.last_staff
import fuoriorario.composeapp.generated.resources.member_added
import fuoriorario.composeapp.generated.resources.member_removed
import fuoriorario.composeapp.generated.resources.member_saved
import it.manu.fuoriorario.core.error.ErrorManager
import it.manu.fuoriorario.core.viewmodel.CustomException
import it.manu.fuoriorario.core.viewmodel.DispatcherProvider
import it.manu.fuoriorario.core.viewmodel.ScreenModel
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.MemberError
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.cleaned
import it.manu.fuoriorario.domain.memberError
import it.manu.fuoriorario.domain.newMemberError
import it.manu.fuoriorario.feature.roster.data.EmailTakenException
import it.manu.fuoriorario.feature.roster.data.LastStaffException
import it.manu.fuoriorario.feature.roster.data.RosterRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.jetbrains.compose.resources.getString

/**
 * [members] is null while the team loads. [draft] is the add form as typed, [editing] the edit sheet's (null when
 * closed), each with its last failed check. [confirmingRemoval] took the first "Togli" tap; [busy]: a write is in flight.
 */
data class RosterScreenState(
    val members: List<Member>? = null,
    val draft: Member = Member("", Role.PLAYER),
    val addError: MemberError? = null,
    val editing: Member? = null,
    val editError: MemberError? = null,
    val confirmingRemoval: Member? = null,
    val busy: Boolean = false
)

/** Staff roster (PRD F2): add, edit and remove members of the team. */
class RosterViewModel(
    private val roster: RosterRepository,
    private val dispatchers: DispatcherProvider,
    errorManager: ErrorManager
) : ScreenModel<RosterScreenState>(RosterScreenState(), dispatchers, errorManager, busy = { copy(busy = it) }) {
    private val changeChannel = Channel<Member>(Channel.BUFFERED)

    /** A member added, edited or removed, so the player menu and the signed-in member catch up. */
    val changes = changeChannel.receiveAsFlow()

    init {
        defaultLaunch(dispatchers.main()) {
            val members = roster.members()
            set { copy(members = members) }
        }
    }

    fun onDraftChanged(draft: Member) = update { copy(draft = draft) }

    /** Errors stay under the form; a failed save toasts and keeps what was typed. */
    fun onAdd() {
        val team = state.members ?: return
        if (state.busy) return
        val draft = state.draft
        val member = draft.cleaned()
        val error = newMemberError(member.email, member.displayName, draft.jerseyNumber.orEmpty(), team)
        update { copy(addError = error) }
        if (error != null) return
        act {
            val added = try {
                roster.add(member)
            } catch (_: EmailTakenException) {
                return@act set { copy(addError = MemberError.EMAIL_TAKEN) }
            }
            set { copy(members = members.orEmpty() + added, draft = RosterScreenState().draft) }
            changeChannel.send(added)
            toast(getString(Res.string.member_added, member.displayName))
        }
    }

    /** Everything but the email: it's the login. */
    fun onEdit(member: Member) = update { copy(editing = member, editError = null, confirmingRemoval = null) }

    fun onEditChanged(member: Member) = update { copy(editing = member) }

    fun onDismissEdit() = update { copy(editing = null) }

    fun onSaveEdit() {
        val draft = state.editing ?: return
        if (state.busy) return
        val edited = draft.cleaned()
        val error = memberError(edited.displayName, draft.jerseyNumber.orEmpty())
        update { copy(editError = error) }
        if (error != null) return
        act {
            keepingStaff { roster.update(edited) }
            set { copy(members = members?.map { if (it.id == edited.id) edited else it }, editing = null) }
            changeChannel.send(edited)
            toast(Res.string.member_saved)
        }
    }

    /** The first tap asks to confirm, the second removes. */
    fun onRemove(member: Member) {
        if (state.busy) return
        if (state.confirmingRemoval != member) return update { copy(confirmingRemoval = member) }
        update { copy(confirmingRemoval = null) }
        act {
            keepingStaff { roster.remove(member) }
            set { copy(members = members?.filter { it.id != member.id }) }
            toast(getString(Res.string.member_removed, member.displayName))
            changeChannel.send(member)
        }
    }

    /** The team must keep a staff member: the toast says so rather than "Salvataggio non riuscito". */
    private suspend fun keepingStaff(write: suspend () -> Unit) = try {
        write()
    } catch (_: LastStaffException) {
        throw CustomException(0, getString(Res.string.last_staff), null)
    }
}
