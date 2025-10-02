package com.ivelosi.dnc.data.repository

import androidx.datastore.core.DataStore
import com.ivelosi.dnc.data.local.profile.ProfileEntity
import com.ivelosi.dnc.domain.model.device.Profile
import com.ivelosi.dnc.domain.model.device.toProfile
import com.ivelosi.dnc.domain.repository.OwnProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class OwnProfileLocalRepository(private val ownProfileDataStore: DataStore<ProfileEntity>) : OwnProfileRepository {
    override fun getProfileAsFlow(): Flow<Profile> {
        return ownProfileDataStore.data.map { it.toProfile() }
    }

    override suspend fun getProfile(): Profile {
        return ownProfileDataStore.data.first().toProfile()
    }

    override suspend fun setNid(Nid: Long) {
        ownProfileDataStore.updateData { it.copy(Nid = Nid) }
    }

    override suspend fun setUpdateTimestamp(updateTimestamp: Long) {
        ownProfileDataStore.updateData { it.copy(updateTimestamp = updateTimestamp) }
    }

    override suspend fun setUsername(username: String) {
        ownProfileDataStore.updateData { it.copy(username = username) }
    }

    override suspend fun setImageFileName(imageFileName: String) {
        ownProfileDataStore.updateData { it.copy(imageFileName = imageFileName) }
    }
}