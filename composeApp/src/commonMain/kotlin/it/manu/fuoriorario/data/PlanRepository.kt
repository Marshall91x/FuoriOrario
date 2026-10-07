package it.manu.fuoriorario.data

import it.manu.fuoriorario.domain.LibraryExercise
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanCheck
import it.manu.fuoriorario.domain.PlanItem
import kotlinx.datetime.LocalDate

/** The week changed since it was loaded: reload before writing again. */
class PlanChangedException : Exception()

/**
 * Weekly plans and their notes: RLS lets a player read only their own, staff read and write those of their team's players.
 * Checks are the other way round: only the player checks, staff read them.
 */
interface PlanRepository {
    /** [member]'s exercises for [week] (its Monday), in the order they were added. */
    suspend fun items(member: Member, week: LocalDate): List<PlanItem>

    /** Saves [item] under its [PlanItem.memberId], returning it with its id. Throws [PermissionDeniedException]. */
    suspend fun add(item: PlanItem): PlanItem

    /** Saves the edited [item] in place. Throws [PermissionDeniedException]. */
    suspend fun update(item: PlanItem)

    /** Removes [item] with its checks. Throws [PermissionDeniedException]. */
    suspend fun remove(item: PlanItem)

    /**
     * Copies [member]'s exercises from the week before [week] into it, without checks; returns the copies, none when
     * that week was empty. [seen] is how many exercises [week] had when loaded: if that changed, nothing is copied.
     * Throws [PlanChangedException], [PermissionDeniedException].
     */
    suspend fun copyPreviousWeek(member: Member, week: LocalDate, seen: Int): List<PlanItem>

    /** The team's library, in its order. */
    suspend fun library(): List<LibraryExercise>

    /** Saves [exercise] in the staff's team library, returning it with its id. Throws [PermissionDeniedException]. */
    suspend fun addToLibrary(exercise: LibraryExercise): LibraryExercise

    /** Saves the edited library [exercise] in place; plans keep their copies. Throws [PermissionDeniedException]. */
    suspend fun updateInLibrary(exercise: LibraryExercise)

    /** Deletes [exercise] from the library; plans keep their copies. Throws [PermissionDeniedException]. */
    suspend fun removeFromLibrary(exercise: LibraryExercise)

    /** Puts the library in the order of [library], all at once. Throws [PermissionDeniedException]. */
    suspend fun reorderLibrary(library: List<LibraryExercise>)

    /** [member]'s staff note for [week], if any. */
    suspend fun note(member: Member, week: LocalDate): String?

    /** Sets [member]'s staff note for [week]; null removes it. Throws [PermissionDeniedException]. */
    suspend fun saveNote(member: Member, week: LocalDate, note: String?)

    /** The checks on [items]. */
    suspend fun checks(items: List<PlanItem>): Set<PlanCheck>

    /** Throws [PermissionDeniedException]. */
    suspend fun check(check: PlanCheck)

    /** Throws [PermissionDeniedException]. */
    suspend fun uncheck(check: PlanCheck)
}
