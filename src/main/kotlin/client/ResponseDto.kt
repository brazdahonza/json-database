package client

import kotlinx.serialization.Serializable

@Serializable
data class ResponseDto(val response: String, val value: String = "", val reason: String = "") {
}