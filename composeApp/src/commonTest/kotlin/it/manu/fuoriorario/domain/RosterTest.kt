package it.manu.fuoriorario.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RosterTest {
    private fun player(name: String, number: String? = null, email: String = "$name@example.com") =
        Member(name, Role.PLAYER, email = email, jerseyNumber = number)

    @Test
    fun rosterOrder_byNumberThenName_unnumberedLast() {
        val members =
            listOf(player("Zeno"), player("Marco", "10"), player("anna"), player("Luca", "7"), player("Bea", "7"))

        assertEquals(listOf("Bea", "Luca", "Marco", "anna", "Zeno"), members.rosterOrder().map { it.displayName })
    }

    @Test
    fun newPlayerError_valid() {
        assertNull(newPlayerError("nuovo@example.com", "Luca B.", "", emptyList()))
        assertNull(newPlayerError("nuovo@example.com", "x".repeat(40), "0", emptyList()))
        assertNull(newPlayerError("nuovo@example.com", "Luca", "99", emptyList()))
    }

    @Test
    fun newPlayerError_invalid() {
        val team = listOf(player("Luca", email = "luca@example.com"))
        assertEquals(PlayerError.EMAIL_INVALID, newPlayerError("luca", "Luca", "", team))
        assertEquals(PlayerError.EMAIL_INVALID, newPlayerError("a b@example.com", "Luca", "", team))
        assertEquals(PlayerError.EMAIL_TAKEN, newPlayerError("luca@example.com", "Luca", "", team))
        assertEquals(PlayerError.NAME_LENGTH, newPlayerError("nuovo@example.com", "", "", team))
        assertEquals(PlayerError.NAME_LENGTH, newPlayerError("nuovo@example.com", "x".repeat(41), "", team))
        assertEquals(PlayerError.NUMBER_RANGE, newPlayerError("nuovo@example.com", "Luca", "100", team))
        assertEquals(PlayerError.NUMBER_RANGE, newPlayerError("nuovo@example.com", "Luca", "-1", team))
    }

    @Test
    fun memberError_onEdit_checksOnlyNameAndNumber() {
        assertNull(memberError("Luca", ""))
        assertNull(memberError("Luca", "99"))
        assertEquals(PlayerError.NAME_LENGTH, memberError("", "7"))
        assertEquals(PlayerError.NUMBER_RANGE, memberError("Luca", "100"))
    }
}
