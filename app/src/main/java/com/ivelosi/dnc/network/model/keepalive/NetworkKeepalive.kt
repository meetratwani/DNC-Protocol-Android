package com.ivelosi.dnc.network.model.keepalive

import com.ivelosi.dnc.network.model.device.NetworkDevice
import kotlinx.serialization.Serializable

@Serializable
data class NetworkKeepalive(
    val networkDevices: List<NetworkDevice>
)