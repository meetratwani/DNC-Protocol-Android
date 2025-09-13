package com.ivelosi.dnc.network.model.message

import kotlinx.serialization.Serializable

@Serializable
data class NetworkMessageAck(
    val messageId: Long,
    val senderId: Long,
    val receiverId: Long
)