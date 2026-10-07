package it.manu.fuoriorario.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Area of an exercise, in the prototype's order; serial names are the values of `plan_items.category`. */
@Serializable
enum class Category {
    @SerialName("Ball handling")
    BALL_HANDLING,

    @SerialName("Tiro")
    SHOOTING,

    @SerialName("Footwork")
    FOOTWORK,

    @SerialName("Atletica")
    ATHLETICS,

    @SerialName("Difesa")
    DEFENSE,

    @SerialName("Recupero")
    RECOVERY
}

const val TITLE_MAX = 60
const val VOLUME_MAX = 40
const val DESCRIPTION_MAX = 400
const val WEEKLY_NOTE_MAX = 500

/** [date]'s day in its week: 0 = Monday … 6 = Sunday. */
fun dayIndex(date: LocalDate) = date.dayOfWeek.isoDayNumber - 1

/** The week [date] falls in, as its Monday (CONTEXT "Settimana"). */
fun weekOf(date: LocalDate): LocalDate = date.minus(DatePeriod(days = dayIndex(date)))

/** A `plan_items` row: an exercise for one player in one [week]; team is filled by the database from the staff. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PlanItem(
    /** The week's Monday. */
    val week: LocalDate,
    val title: String,
    val category: Category,
    // Always sent, null too: an edit that empties them must clear them.
    @EncodeDefault val volume: String? = null,
    @EncodeDefault val description: String? = null,
    @EncodeDefault @SerialName("video_url") val videoUrl: String? = null,
    /** 0 = Monday … 6 = Sunday, ascending. */
    val days: List<Int>,
    /** Null until saved; never sent, the database generates it. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val id: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) @SerialName("member_id") val memberId: String? = null
)

/**
 * An `exercise_library` row: a model exercise of the team (Libreria); plans copy it, never refer to it. Team is filled
 * by the database from the staff.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class LibraryExercise(
    val title: String,
    val category: Category,
    // Always sent, null too: an edit that empties them must clear them.
    @EncodeDefault val volume: String? = null,
    @EncodeDefault val description: String? = null,
    @EncodeDefault @SerialName("video_url") val videoUrl: String? = null,
    /** Position in the library, ascending. */
    @EncodeDefault val sort: Int = 0,
    /** Null until saved; never sent, the database generates it. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val id: String? = null
)

/** The library exercise to save from the form: trimmed, blanks null. */
fun LibraryExercise.cleaned() = copy(
    title = title.trim(),
    volume = volume?.trim()?.ifEmpty { null },
    description = description?.trim()?.ifEmpty { null },
    videoUrl = videoUrl?.trim()?.ifEmpty { null }
)

enum class PlanItemError { TITLE, NO_DAYS, VIDEO }

/** First problem with the exercise form as typed, or null; [days] is null for a library exercise, which has none. */
fun planItemError(title: String, days: Set<Int>?, video: String): PlanItemError? = when {
    title.isBlank() -> PlanItemError.TITLE
    days?.isEmpty() == true -> PlanItemError.NO_DAYS
    video.isNotBlank() && !video.trim().startsWith("https://") -> PlanItemError.VIDEO
    else -> null
}

/** The exercise to save from the form: trimmed, blanks null, days ascending. */
fun newPlanItem(
    week: LocalDate,
    title: String,
    category: Category,
    volume: String,
    description: String,
    video: String,
    days: Set<Int>
) = PlanItem(
    week,
    title.trim(),
    category,
    volume.trim().ifEmpty { null },
    description.trim().ifEmpty { null },
    video.trim().ifEmpty { null },
    days.sorted()
)

/** A `plan_checks` row: the player did exercise [planItemId] on [day] (0 = Monday) of its week. */
@Serializable
data class PlanCheck(@SerialName("plan_item_id") val planItemId: String, val day: Int)

/** The check for [day] of this saved exercise. */
fun PlanItem.checkOn(day: Int) = PlanCheck(id!!, day)

/** Completamento: [done] checks out of [total] assigned days. */
data class Progress(val done: Int, val total: Int)

/** The week's [Progress] over [items]; checks on days no longer assigned don't count. */
fun progress(items: List<PlanItem>, checks: Set<PlanCheck>) = Progress(
    items.sumOf { item -> item.days.count { item.checkOn(it) in checks } },
    items.sumOf { it.days.size }
)
