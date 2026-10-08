package it.manu.fuoriorario

import it.manu.fuoriorario.core.error.ErrorManager
import it.manu.fuoriorario.core.navigation.Route
import it.manu.fuoriorario.core.session.SelectedPlayer
import it.manu.fuoriorario.core.session.Session
import it.manu.fuoriorario.core.session.member
import it.manu.fuoriorario.core.viewmodel.ComposeViewModel
import it.manu.fuoriorario.core.viewmodel.DispatcherProvider
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.rosterOrder
import it.manu.fuoriorario.feature.auth.data.AuthRepository
import it.manu.fuoriorario.feature.roster.data.RosterRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update

/**
 * [players]: staff only, the team's players for the header menu; null while loading.
 * [followed]: whose Diario di tiro and Piano to show, the player themselves or the one staff picked.
 */
data class MainScreenState(
    val session: Session = Session.Loading,
    val players: Result<List<Member>>? = null,
    val followed: Member? = null
) {
    val member get() = session.member

    val route
        get() = when (session) {
            Session.Loading -> Route.LOADING
            Session.SignedOut, Session.Expired -> Route.LOGIN
            is Session.SignedIn -> if (session.member.privacyAckAt == null) Route.PRIVACY else Route.HOME
        }
}

/** The root: follows the session, so login, logout and expiry move the app without anyone navigating. */
class MainViewModel(
    private val auth: AuthRepository,
    private val roster: RosterRepository,
    private val selectedPlayer: SelectedPlayer,
    dispatchers: DispatcherProvider,
    errorManager: ErrorManager
) : ComposeViewModel<MainScreenState>(dispatcherProvider = dispatchers, errorManager = errorManager) {
    private val playersReload = MutableStateFlow(0)

    init {
        defaultLaunch {
            // One subscription: each one would load the member again.
            val session = auth.session.shareIn(this, SharingStarted.Eagerly, replay = 1)
            combine(session, session.players(), selectedPlayer.id) { s, players, picked ->
                MainScreenState(s, players, followed(s, players, picked))
            }.collect { emitSuccess(it) }
        }
    }

    /** Players in the menu come from the roster: they all have an id. */
    fun onPick(player: Member) {
        player.id?.let(selectedPlayer::select)
    }

    fun onRetryPlayers() = playersReload.update { it + 1 }

    /** Staff changed [member] in the roster: if it's their own row, their role or name may have changed too. */
    fun onRosterChanged(member: Member) {
        if (member.id == uiState.value.data?.member?.id) auth.refresh()
        onRetryPlayers()
    }

    fun onSignOut() {
        // Nothing to say if it fails: the session is forgotten on this device anyway.
        defaultLaunchForChannels(errorFunction = {}) { auth.signOut() }
    }

    /** A fresh list per staff member (null until it arrives); a retry keeps the old one on screen meanwhile. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Flow<Session>.players(): Flow<Result<List<Member>>?> =
        map { s -> s.member.let { (it?.role == Role.STAFF) to it?.id } }
            .distinctUntilChanged()
            .flatMapLatest { (staff, _) ->
                if (!staff) {
                    flowOf(null)
                } else {
                    playersReload.mapLatest<Int, Result<List<Member>>?> { loadPlayers() }.onStart { emit(null) }
                }
            }

    private suspend fun loadPlayers(): Result<List<Member>> = try {
        Result.success(roster.members().filter { it.role == Role.PLAYER }.rosterOrder())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun followed(session: Session, players: Result<List<Member>>?, picked: String?): Member? {
        val me = session.member ?: return null
        if (me.role != Role.STAFF) return me
        return players?.getOrNull()?.let { list -> list.find { it.id == picked } ?: list.firstOrNull() }
    }
}
