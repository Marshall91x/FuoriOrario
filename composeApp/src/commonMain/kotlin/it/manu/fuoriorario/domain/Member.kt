package it.manu.fuoriorario.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Member(@SerialName("display_name") val displayName: String, val role: Role)

@Serializable
enum class Role {
    @SerialName("staff")
    STAFF,

    @SerialName("player")
    PLAYER
}
