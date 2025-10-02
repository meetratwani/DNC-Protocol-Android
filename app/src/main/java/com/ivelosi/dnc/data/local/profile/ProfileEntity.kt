package com.ivelosi.dnc.data.local.profile

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class ProfileEntity(
    @PrimaryKey
    val Nid: Long,
    val updateTimestamp: Long,
    val username: String,
    val imageFileName: String?
)