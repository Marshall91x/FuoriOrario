package it.manu.fuoriorario.domain

import kotlin.jvm.JvmName

/** Ruolo in campo, stored as-is in `members.position`. */
val POSITIONS = listOf("Playmaker", "Guardia", "Ala", "Ala grande", "Centro")

private val EMAIL = Regex("""[^@\s]+@[^@\s]+\.[^@\s]+""")

/** By jersey number (unnumbered last), then name. */
fun List<Member>.rosterOrder(): List<Member> = sortedWith(rosterOrder(Member::jerseyNumber, Member::displayName))

/** Convocati like the roster they come from. */
@JvmName("callUpRosterOrder")
fun List<CallUp>.rosterOrder(): List<CallUp> = sortedWith(rosterOrder(CallUp::number, CallUp::name))

private fun <T> rosterOrder(number: (T) -> String?, name: (T) -> String) =
    compareBy<T, Int?>(nullsLast()) { number(it)?.toIntOrNull() }.thenBy { name(it).lowercase() }

/** [this] as typed in a form, ready to save: trimmed, email lowercase, number "07" → "7" and "" → null. */
fun Member.cleaned() = copy(
    displayName = displayName.trim(),
    email = email.trim().lowercase(),
    jerseyNumber = jerseyNumber?.toIntOrNull()?.toString()
)

enum class MemberError { EMAIL_INVALID, EMAIL_TAKEN, NAME_LENGTH, NUMBER_RANGE }

/** First problem with a new member, or null. [email] and [name] already trimmed; [number] empty when not given. */
fun newMemberError(email: String, name: String, number: String, team: List<Member>): MemberError? = when {
    !EMAIL.matches(email) -> MemberError.EMAIL_INVALID
    team.any { it.email.equals(email, ignoreCase = true) } -> MemberError.EMAIL_TAKEN
    else -> memberError(name, number)
}

/** First problem with a member's editable fields (email can't change), or null. */
fun memberError(name: String, number: String): MemberError? = when {
    name.length !in 1..40 -> MemberError.NAME_LENGTH
    number.isNotEmpty() && number.toIntOrNull() !in 0..99 -> MemberError.NUMBER_RANGE
    else -> null
}
