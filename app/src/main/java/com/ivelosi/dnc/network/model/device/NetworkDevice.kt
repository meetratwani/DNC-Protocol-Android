package com.ivelosi.dnc.network.model.device

import kotlinx.serialization.Serializable

@Serializable
data class NetworkDevice(
    val ipAddress: String?,
    val keepalive: Long,
    val account: NetworkAccount
)