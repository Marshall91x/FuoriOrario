package it.manu.fuoriorario.feature.league

import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.fixture_deleted
import fuoriorario.composeapp.generated.resources.fixture_saved
import it.manu.fuoriorario.core.ROME
import it.manu.fuoriorario.core.error.ErrorManager
import it.manu.fuoriorario.core.today
import it.manu.fuoriorario.core.viewmodel.DispatcherProvider
import it.manu.fuoriorario.core.viewmodel.ScreenModel
import it.manu.fuoriorario.domain.Fixture
import it.manu.fuoriorario.domain.FixtureStatus
import it.manu.fuoriorario.domain.League
import it.manu.fuoriorario.domain.Leg
import it.manu.fuoriorario.domain.OPPONENT_MAX
import it.manu.fuoriorario.domain.Period
import it.manu.fuoriorario.domain.Round
import it.manu.fuoriorario.domain.nextRound
import it.manu.fuoriorario.domain.ours
import it.manu.fuoriorario.domain.rounds
import it.manu.fuoriorario.feature.games.GameForm
import it.manu.fuoriorario.feature.games.GamesStart
import it.manu.fuoriorario.feature.league.data.LeagueRepository
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** Leagues shown are the season's: the latest created from 1 September on. */
internal fun seasonStart() = Period.SEASON.start(today()).atStartOfDayIn(ROME)

/** Why the fixture sheet can't save. */
enum class FixtureError {
    /** Home or away missing, or the same team. */
    TEAMS,

    /** Scheduled or played without a valid date and time. */
    DATE,

    /** Played without both scores. */
    RESULT,

    /** Played with the same score: basketball has no draws, the standings couldn't count it. */
    DRAW
}

/**
 * The Modifica or "+ Gara" sheet as typed ([new]): [date] "9/11/2026" and [time] "18:30" in Rome, any separator.
 * A postponed fixture saves without date, a result only when played. [number] is made once for a new fixture, so a
 * save sent again adds nothing.
 */
data class FixtureDraft(
    val number: String,
    val new: Boolean,
    val round: Int,
    val leg: Leg,
    val homeId: String?,
    val awayId: String?,
    val status: FixtureStatus = FixtureStatus.SCHEDULED,
    val date: String = "",
    val time: String = "",
    val venue: String = "",
    val homeScore: String = "",
    val awayScore: String = ""
) {
    private val startsAt get() = runCatching {
        val (d, m, y) = numbers(date)
        val (h, min) = numbers(time)
        LocalDateTime(if (y < 100) 2000 + y else y, m, d, h, min).toInstant(ROME)
    }.getOrNull()

    val error get() = when {
        homeId == null || awayId == null || homeId == awayId -> FixtureError.TEAMS
        status != FixtureStatus.POSTPONED && startsAt == null -> FixtureError.DATE
        status == FixtureStatus.PLAYED && (homeScore.toIntOrNull() == null || awayScore.toIntOrNull() == null) ->
            FixtureError.RESULT
        status == FixtureStatus.PLAYED && homeScore.toInt() == awayScore.toInt() -> FixtureError.DRAW
        else -> null
    }

    /** Only once [error] is null. */
    val fixture get() = Fixture(
        number,
        round,
        leg,
        startsAt.takeIf { status != FixtureStatus.POSTPONED },
        homeId!!,
        awayId!!,
        venue.trim().ifEmpty { null },
        status,
        homeScore.toIntOrNull().takeIf { status == FixtureStatus.PLAYED },
        awayScore.toIntOrNull().takeIf { status == FixtureStatus.PLAYED }
    )

    private fun numbers(text: String) = text.split(Regex("\\D+")).filter { it.isNotEmpty() }.map(String::toInt)

    companion object {
        fun of(f: Fixture) = f.startsAt?.toLocalDateTime(ROME).let { at ->
            FixtureDraft(
                f.number,
                new = false,
                f.round,
                f.leg,
                f.homeId,
                f.awayId,
                f.status,
                date = at?.let { "${it.day}/${it.month.ordinal + 1}/${it.year}" }.orEmpty(),
                time = at?.let { "${it.hour}:${it.minute.toString().padStart(2, '0')}" }.orEmpty(),
                venue = f.venue.orEmpty(),
                homeScore = f.homeScore?.toString().orEmpty(),
                awayScore = f.awayScore?.toString().orEmpty()
            )
        }
    }
}

/**
 * [league] null: none imported this season. [ours] is "Nostre" (only our fixtures) against "Tutto il girone";
 * [rounds] are what shows, [open] the one with the next fixture. Staff only: [editing] is the fixture sheet (null when
 * closed) with its last failed check [error]; [confirmingDelete] took the first "Elimina" tap.
 */
data class CalendarScreenState(
    val league: League? = null,
    val ours: Boolean = true,
    val rounds: List<Round> = emptyList(),
    val open: Int = 0,
    val editing: FixtureDraft? = null,
    val error: FixtureError? = null,
    val confirmingDelete: Boolean = false,
    val busy: Boolean = false
) {
    /**
     * "Inizia live" on our fixture still to play, without a live game yet, from the day before (Partite's limit):
     * Partite's form with its date, the opponent's FIP name and home or away. Null: no button.
     */
    /** The live game recorded for the fixture being edited, if any: its Riepilogo from the sheet. */
    val editingGameId get() = editing?.takeIf { !it.new }?.let { d ->
        league?.fixtures?.firstOrNull { it.number == d.number }?.gameId
    }

    fun liveStart(fixture: Fixture, today: LocalDate = today()): GamesStart? {
        val ours = league?.ours ?: return null
        val date = fixture.startsAt?.toLocalDateTime(ROME)?.date ?: return null
        if (!fixture.plays(ours) || fixture.status == FixtureStatus.PLAYED || fixture.gameId != null) return null
        if (date > today.plus(1, DateTimeUnit.DAY)) return null
        val home = fixture.homeId == ours.id
        val opponent = league.team(if (home) fixture.awayId else fixture.homeId).name
        return GamesStart(GameForm(date, opponent.take(OPPONENT_MAX), home, fixtureId = fixture.id))
    }
}

/** Partite → Calendario (ADR 0011): the league's fixtures by round, for the whole team; staff edit, add, delete them. */
class CalendarViewModel(
    private val repository: LeagueRepository,
    dispatchers: DispatcherProvider,
    errorManager: ErrorManager
) : ScreenModel<CalendarScreenState>(CalendarScreenState(), dispatchers, errorManager, busy = { copy(busy = it) }) {
    init {
        defaultLaunch(dispatchers.main()) {
            val league = repository.league(seasonStart())
            set { show(league, ours) }
        }
    }

    fun onOursChanged(ours: Boolean) = update { show(league, ours) }

    fun onEdit(fixture: Fixture) = open(FixtureDraft.of(fixture))

    /** "+ Gara": in the round on screen. */
    @OptIn(ExperimentalUuidApi::class)
    fun onAdd() {
        val round = state.rounds.getOrNull(state.open)
        // Not a FIP number (only digits): a reimport never finds it.
        val number = "M" + Uuid.random().toHexString().take(8)
        open(
            FixtureDraft(number, new = true, round?.number ?: 1, round?.leg ?: Leg.FIRST, homeId = null, awayId = null)
        )
    }

    /** A change asks "Elimina" again: the confirmation is for what was on screen. */
    fun onDraftChanged(draft: FixtureDraft) = update { copy(editing = draft, confirmingDelete = false) }

    fun onDismiss() = update { copy(editing = null, error = null, confirmingDelete = false) }

    fun onSave() {
        val draft = state.editing ?: return
        val league = state.league ?: return
        if (state.busy) return
        val error = draft.error
        update { copy(error = error) }
        if (error != null) return
        val fixture = draft.fixture
        act {
            val saved = repository.save(league, fixture)
            set { closed(league.fixtures.filter { it.number != fixture.number } + saved) }
            toast(Res.string.fixture_saved)
        }
    }

    /** "Elimina", then "Conferma". */
    fun onDelete() {
        val draft = state.editing?.takeIf { !it.new } ?: return
        val league = state.league ?: return
        if (!state.confirmingDelete) return update { copy(confirmingDelete = true) }
        if (state.busy) return
        val fixture = league.fixtures.firstOrNull { it.number == draft.number } ?: return
        act {
            repository.delete(league, fixture)
            set { closed(league.fixtures - fixture) }
            toast(Res.string.fixture_deleted)
        }
    }

    private fun open(draft: FixtureDraft) = update { copy(editing = draft, error = null, confirmingDelete = false) }

    private fun CalendarScreenState.closed(fixtures: List<Fixture>) =
        show(league!!.copy(fixtures = fixtures), ours).copy(editing = null, confirmingDelete = false)

    private fun CalendarScreenState.show(league: League?, ours: Boolean): CalendarScreenState {
        val all = league?.rounds().orEmpty()
        val rounds = if (ours) all.ours(league?.ours) else all
        return copy(league = league, ours = ours, rounds = rounds, open = rounds.nextRound(Clock.System.now()))
    }
}
