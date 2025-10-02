package com.ivelosi.dnc.domain.model.device

import com.ivelosi.dnc.data.local.NodeIdentity
import com.ivelosi.dnc.network.model.device.NetworkDevice

data class Device(
    var ipAddress: String?,
    var keepalive: Long,
    val account: Account,
    val profile: Profile,
    val nid: Long = NodeIdentity.nid
) {
    constructor(networkDevice: NetworkDevice, profile: Profile)
            : this(networkDevice.ipAddress, networkDevice.keepalive, networkDevice.account.toAccount(), profile, NodeIdentity.nid)
}

fun Device.toNetworkDevice(): NetworkDevice {
    return NetworkDevice(
        ipAddress = ipAddress,
        keepalive = keepalive,
        account = account.toNetworkAccount()
    )
}