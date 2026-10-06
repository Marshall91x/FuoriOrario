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
    fun newMemberError_valid() {
        assertNull(newMemberError("nuovo@example.com", "Luca B.", "", emptyList()))
        assertNull(newMemberError("nuovo@example.com", "x".repeat(40), "0", emptyList()))
        assertNull(newMemberError("nuovo@example.com", "Luca", "99", emptyList()))
    }

    @Test
    fun newMemberError_invalid() {
        val team = listOf(player("Luca", email = "luca@example.com"))
        assertEquals(MemberError.EMAIL_INVALID, newMemberError("luca", "Luca", "", team))
        assertEquals(MemberError.EMAIL_INVALID, newMemberError("a b@example.com", "Luca", "", team))
        assertEquals(MemberError.EMAIL_TAKEN, newMemberError("luca@example.com", "Luca", "", team))
        assertEquals(MemberError.NAME_LENGTH, newMemberError("nuovo@example.com", "", "", team))
        assertEquals(MemberError.NAME_LENGTH, newMemberError("nuovo@example.com", "x".repeat(41), "", team))
        assertEquals(MemberError.NUMBER_RANGE, newMemberError("nuovo@example.com", "Luca", "100", team))
        assertEquals(MemberError.NUMBER_RANGE, newMemberError("nuovo@example.com", "Luca", "-1", team))
    }

    @Test
    fun memberError_onEdit_checksOnlyNameAndNumber() {
        assertNull(memberError("Luca", ""))
        assertNull(memberError("Luca", "99"))
        assertEquals(MemberError.NAME_LENGTH, memberError("", "7"))
        assertEquals(MemberError.NUMBER_RANGE, memberError("Luca", "100"))
    }

    @Test
    fun cleaned_trimsAndNormalizes() {
        val typed = Member(" Luca ", Role.PLAYER, email = " Luca@Example.com ", jerseyNumber = "07")
        assertEquals(Member("Luca", Role.PLAYER, email = "luca@example.com", jerseyNumber = "7"), typed.cleaned())
        assertNull(typed.copy(jerseyNumber = "").cleaned().jerseyNumber)
    }
}
