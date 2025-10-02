package com.ivelosi.dnc.network.model.device

import kotlinx.serialization.Serializable

@Serializable
data class NetworkAccount(
    val Nid: Long,
    val profileUpdateTimestamp: Long
)