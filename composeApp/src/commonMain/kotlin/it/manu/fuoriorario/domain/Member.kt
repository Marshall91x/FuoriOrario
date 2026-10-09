package it.manu.fuoriorario.domain

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A `members` row; `team_id` is filled by the database from the staff inserting it. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Member(
    @SerialName("display_name") val displayName: String,
    val role: Role,
    /** ISO timestamp; null until the member accepts the privacy notice. */
    @SerialName("privacy_ack_at") val privacyAckAt: String? = null,
    val email: String = "",
    /** "0".."99". */
    @SerialName("jersey_number") val jerseyNumber: String? = null,
    /** One of [POSITIONS]. */
    val position: String? = null,
    /** Null until saved; never sent, the database generates it. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val id: String? = null
)

@Serializable
enum class Role {
    @SerialName("staff")
    STAFF,

    @SerialName("player")
    PLAYER
}
