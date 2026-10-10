package it.manu.fuoriorario.domain

import kotlin.math.roundToInt
import kotlinx.datetime.LocalDate
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** The 10 fixed shooting areas, in the prototype's order; serial names are the keys of `shot_sessions.zones`. */
@Serializable
enum class Zone {
    @SerialName("pit")
    PIT,

    @SerialName("mls")
    MLS,

    @SerialName("mlc")
    MLC,

    @SerialName("mld")
    MLD,

    @SerialName("acs")
    ACS,

    @SerialName("als")
    ALS,

    @SerialName("cen")
    CEN,

    @SerialName("ald")
    ALD,

    @SerialName("acd")
    ACD,

    @SerialName("tl")
    TL
}

/** Made / attempted in one zone; stored as `[made, attempted]`. */
@Serializable(ShotsSerializer::class)
data class Shots(val made: Int = 0, val attempted: Int = 0) {
    /** Null without attempts. */
    val percent: Int? get() = percentOf(made, attempted)

    operator fun plus(other: Shots) = Shots(made + other.made, attempted + other.attempted)

    /** Made more than attempted raises attempted to match. */
    fun withMade(made: Int) = Shots(made, maxOf(attempted, made))

    /** "+" on made adds 1. */
    fun stepMade(up: Boolean) = withMade((made + if (up) 1 else -1).coerceAtLeast(0))

    /** "+" on attempted adds [ATTEMPTS_STEP]. */
    fun stepAttempted(up: Boolean) =
        copy(attempted = (attempted + if (up) ATTEMPTS_STEP else -ATTEMPTS_STEP).coerceAtLeast(0))
}

/** [part] of [whole] in whole percent, rounded half up like the prototype; null when [whole] is 0. */
fun percentOf(part: Int, whole: Int): Int? = if (whole == 0) null else (part * 100.0 / whole).roundToInt()

const val ATTEMPTS_STEP = 5
const val NOTE_MAX = 300

internal object ShotsSerializer : KSerializer<Shots> {
    private val list = ListSerializer(Int.serializer())
    override val descriptor = list.descriptor

    override fun serialize(encoder: Encoder, value: Shots) =
        list.serialize(encoder, listOf(value.made, value.attempted))

    override fun deserialize(decoder: Decoder) = list.deserialize(decoder).let { (m, a) -> Shots(m, a) }
}

/** A `shot_sessions` row; team and author are filled by the database from the signed-in member. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ShotSession(
    val date: LocalDate,
    /** Only zones with attempts. */
    val zones: Map<Zone, Shots>,
    val note: String? = null,
    /** Null until saved; never sent, the database generates it. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val id: String? = null,
    /** Whose session it is; when null the database saves it under the signed-in member. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) @SerialName("member_id") val memberId: String? = null
)

/** Dal campo: every zone but free throws. */
val ShotSession.fieldGoal get() = zones.filterKeys { it != Zone.TL }.values.fold(Shots(), Shots::plus)

val ShotSession.freeThrows get() = zones[Zone.TL] ?: Shots()

internal val THREES = setOf(Zone.ACS, Zone.ALS, Zone.CEN, Zone.ALD, Zone.ACD)

/** Da tre: the five zones beyond the arc. */
val ShotSession.three get() = zones.filterKeys { it in THREES }.values.fold(Shots(), Shots::plus)

sealed interface SessionError {
    data class MadeOverAttempted(val zone: Zone) : SessionError

    data object NoAttempts : SessionError
}

/** First problem with the zones as typed, in [Zone] order, or null. */
fun sessionError(zones: Map<Zone, Shots>): SessionError? {
    Zone.entries.firstOrNull { z -> zones[z]?.let { it.made > it.attempted } == true }
        ?.let { return SessionError.MadeOverAttempted(it) }
    return if (zones.values.none { it.attempted > 0 }) SessionError.NoAttempts else null
}

/** The session to save from the form: zones without attempts dropped, note trimmed, blank note null. */
fun newSession(date: LocalDate, zones: Map<Zone, Shots>, note: String) =
    ShotSession(date, zones.filterValues { it.attempted > 0 }, note.trim().ifEmpty { null })
