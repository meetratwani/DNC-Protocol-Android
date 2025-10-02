package com.ivelosi.dnc.domain.repository

import com.ivelosi.dnc.domain.model.device.Account
import kotlinx.coroutines.flow.Flow

interface OwnAccountRepository {
    fun getAccountAsFlow(): Flow<Account>
    suspend fun getAccount(): Account
    suspend fun setNid(Nid: Long)
    suspend fun setProfileUpdateTimestamp(profileUpdateTimestamp: Long)
}