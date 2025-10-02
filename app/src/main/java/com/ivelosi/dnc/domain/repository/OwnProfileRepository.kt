package com.ivelosi.dnc.domain.repository

import com.ivelosi.dnc.domain.model.device.Profile
import kotlinx.coroutines.flow.Flow

interface OwnProfileRepository {
    fun getProfileAsFlow(): Flow<Profile>
    suspend fun getProfile(): Profile

    suspend fun setNid(Nid: Long)

    suspend fun setUpdateTimestamp(updateTimestamp: Long)

    suspend fun setUsername(username: String)

    suspend fun setImageFileName(imageFileName: String)
}