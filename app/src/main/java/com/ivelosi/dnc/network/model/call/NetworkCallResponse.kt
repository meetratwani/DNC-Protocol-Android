package com.ivelosi.dnc.network.model.call

import kotlinx.serialization.Serializable

@Serializable
data class NetworkCallResponse(
    val senderId: Long,
    val receiverId: Long,
    val accepted: Boolean
)