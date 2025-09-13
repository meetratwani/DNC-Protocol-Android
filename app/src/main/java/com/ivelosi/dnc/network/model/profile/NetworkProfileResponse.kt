package com.ivelosi.dnc.network.model.profile

import com.ivelosi.dnc.network.model.device.NetworkProfile
import kotlinx.serialization.Serializable

@Serializable
data class NetworkProfileResponse(
    val senderId: Long,
    val receiverId: Long,
    val profile: NetworkProfile
)