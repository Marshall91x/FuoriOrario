package it.manu.fuoriorario.domain

/** Ruolo in campo, stored as-is in `members.position`. */
val POSITIONS = listOf("Playmaker", "Guardia", "Ala", "Ala grande", "Centro")

private val EMAIL = Regex("""[^@\s]+@[^@\s]+\.[^@\s]+""")

/** By jersey number (unnumbered last), then name. */
fun List<Member>.rosterOrder(): List<Member> = sortedWith(
    compareBy<Member, Int?>(nullsLast()) { it.jerseyNumber?.toIntOrNull() }.thenBy { it.displayName.lowercase() }
)

enum class PlayerError { EMAIL_INVALID, EMAIL_TAKEN, NAME_LENGTH, NUMBER_RANGE }

/** First problem with a new member, or null. [email] and [name] already trimmed; [number] empty when not given. */
fun newPlayerError(email: String, name: String, number: String, team: List<Member>): PlayerError? = when {
    !EMAIL.matches(email) -> PlayerError.EMAIL_INVALID
    team.any { it.email.equals(email, ignoreCase = true) } -> PlayerError.EMAIL_TAKEN
    name.length !in 1..40 -> PlayerError.NAME_LENGTH
    number.isNotEmpty() && number.toIntOrNull() !in 0..99 -> PlayerError.NUMBER_RANGE
    else -> null
}
