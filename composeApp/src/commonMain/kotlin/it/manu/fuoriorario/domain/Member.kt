package it.manu.fuoriorario.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Member(
    @SerialName("display_name") val displayName: String,
    val role: Role,
    /** ISO timestamp; null until the member accepts the privacy notice. */
    @SerialName("privacy_ack_at") val privacyAckAt: String? = null
)

@Serializable
enum class Role {
    @SerialName("staff")
    STAFF,

    @SerialName("player")
    PLAYER
}
