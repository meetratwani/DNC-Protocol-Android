package com.ivelosi.dnc.network.model.device

import com.ivelosi.dnc.domain.model.device.Profile
import kotlinx.serialization.Serializable

@Serializable
data class NetworkProfile(
    val Nid: Long,
    val updateTimestamp: Long,
    val username: String,
    val imageBase64: String?
) {
    constructor(profile: Profile, imageBase64: String?)
            : this(profile.Nid, profile.updateTimestamp, profile.username, imageBase64)
}