package com.ivelosi.dnc.network.model.call

import kotlinx.serialization.Serializable

@Serializable
data class NetworkCallRequest(
    val senderId: Long,
    val receiverId: Long
)