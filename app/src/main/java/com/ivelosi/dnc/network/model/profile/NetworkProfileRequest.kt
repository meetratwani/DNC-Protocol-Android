package com.ivelosi.dnc.network.model.profile

import kotlinx.serialization.Serializable

@Serializable
data class NetworkProfileRequest(
    val senderId: Long,
    val receiverId: Long
)