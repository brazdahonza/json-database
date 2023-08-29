package client

import kotlinx.serialization.Serializable

@Serializable
data class RequestDto(val type: String, val key: String, val value: String = " ") {
}