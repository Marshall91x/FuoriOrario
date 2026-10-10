package it.manu.fuoriorario.domain

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 1–4, and OT for every overtime together; serial names are `game_events.quarter`. */
@Serializable
enum class Quarter {
    @SerialName("1")
    Q1,

    @SerialName("2")
    Q2,

    @SerialName("3")
    Q3,

    @SerialName("4")
    Q4,

    @SerialName("5")
    OT
}

/** What happened (glossary *Evento*): TIRO, LIBERO, RIM, AST, PP, REC, FAL and AVV, the opponent's points. */
@Serializable
enum class GameEventType { SHOT, FREE_THROW, REBOUND, ASSIST, TURNOVER, STEAL, FOUL, OPPONENT }

/**
 * A `game_events` row without its game and order, which is its place in the list. [memberId] is null only for
 * [GameEventType.OPPONENT], which has [value] 1–3; [zone] (never [Zone.TL]) only for a shot; [made] for shots and free
 * throws.
 */
@Serializable
data class GameEvent(
    val type: GameEventType,
    val quarter: Quarter,
    @SerialName("member_id") val memberId: String? = null,
    val zone: Zone? = null,
    val made: Boolean? = null,
    val value: Int? = null
)

/** Our points from this event: 0 for anything but a made shot or free throw. */
val GameEvent.points: Int
    get() = when {
        made != true -> 0
        type == GameEventType.FREE_THROW -> 1
        type == GameEventType.SHOT -> if (zone in THREES) 3 else 2
        else -> 0
    }

data class Score(val us: Int = 0, val them: Int = 0) {
    operator fun plus(other: Score) = Score(us + other.us, them + other.them)
}

fun score(events: List<GameEvent>): Score = events.fold(Score()) { sum, e ->
    sum + if (e.type == GameEventType.OPPONENT) Score(them = e.value ?: 0) else Score(us = e.points)
}

/** Parziali: Q1–Q4 always, OT only when something happened in it. */
fun quarterScores(events: List<GameEvent>): Map<Quarter, Score> = Quarter.entries
    .filter { q -> q != Quarter.OT || events.any { it.quarter == q } }
    .associateWith { q -> score(events.filter { it.quarter == q }) }

/** Made / attempted over shots or free throws. */
private fun List<GameEvent>.shots() = fold(Shots()) { sum, e -> sum + Shots(if (e.made == true) 1 else 0, 1) }

/** Made / attempted per zone, free throws under [Zone.TL], like [zoneTotals]: every zone present. */
fun shotZones(events: List<GameEvent>): Map<Zone, Shots> = Zone.entries.associateWith { z ->
    events.filter { e ->
        e.type == GameEventType.SHOT && e.zone == z || e.type == GameEventType.FREE_THROW && z == Zone.TL
    }.shots()
}

/** The events of [quarter], or the whole game when null ("Tutta"). */
fun List<GameEvent>.inQuarter(quarter: Quarter?) = if (quarter == null) this else filter { it.quarter == quarter }

/** A row of the tabellino: PT, T2, T3 and TL made / attempted, RIM, AST, PP, REC, FAL. */
data class BoxLine(
    val callUp: CallUp,
    val points: Int,
    val twos: Shots,
    val threes: Shots,
    val freeThrows: Shots,
    val rebounds: Int,
    val assists: Int,
    val turnovers: Int,
    val steals: Int,
    val fouls: Int
)

/** The tabellino: a row per convocato, in [callUps] order, even without events. */
fun boxScore(callUps: List<CallUp>, events: List<GameEvent>): List<BoxLine> = callUps.map { callUp ->
    val own = events.filter { it.memberId == callUp.id }
    fun count(type: GameEventType) = own.count { it.type == type }
    fun shots(match: (GameEvent) -> Boolean) = own.filter(match).shots()
    BoxLine(
        callUp,
        own.sumOf { it.points },
        shots { it.type == GameEventType.SHOT && it.zone !in THREES },
        shots { it.type == GameEventType.SHOT && it.zone in THREES },
        shots { it.type == GameEventType.FREE_THROW },
        count(GameEventType.REBOUND),
        count(GameEventType.ASSIST),
        count(GameEventType.TURNOVER),
        count(GameEventType.STEAL),
        count(GameEventType.FOUL)
    )
}

/** A `games` row; [id] is the draft's, made on the device. */
@Serializable
data class Game(
    val date: LocalDate,
    val opponent: String,
    val home: Boolean,
    val note: String? = null,
    @SerialName("our_score") val ourScore: Int = 0,
    @SerialName("their_score") val theirScore: Int = 0,
    val id: String? = null
)

/** A convocato as the draft keeps them: what the chip shows, even with the roster out of reach. */
@Serializable
data class CallUp(val id: String, val name: String, val number: String? = null)

/**
 * Partita in corso (ADR 0010): the game not saved yet, on the device. [id] is made here so a save retried after a
 * lost answer finds the game already there. [selected] is the convocato the next shot goes to.
 */
@Serializable
data class GameDraft(
    val id: String,
    val date: LocalDate,
    val opponent: String,
    val home: Boolean,
    val note: String? = null,
    val callUps: List<CallUp> = emptyList(),
    val events: List<GameEvent> = emptyList(),
    val quarter: Quarter = Quarter.Q1,
    val selected: String? = null
) {
    val score get() = score(events)

    fun toGame() = score.let { Game(date, opponent, home, note, it.us, it.them, id) }
}

const val OPPONENT_MAX = 60
const val GAME_NOTE_MAX = 300

enum class GameError { NO_OPPONENT }

/** First problem with a new game as typed, or null; lengths are capped by the fields. */
fun gameError(opponent: String): GameError? = if (opponent.isBlank()) GameError.NO_OPPONENT else null
