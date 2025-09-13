package com.ivelosi.dnc.domain.repository

import com.ivelosi.dnc.domain.model.device.Account
import com.ivelosi.dnc.domain.model.device.Contact
import com.ivelosi.dnc.domain.model.device.Profile
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun getAllContactsAsFlow(): Flow<List<Contact>>
    fun getContactByAccountIdAsFlow(accountId: Long): Flow<Contact?>

    fun getAllAccountsAsFlow(): Flow<List<Account>>
    fun getAccountByAccountIdAsFlow(accountId: Long): Flow<Account?>
    suspend fun getAllAccounts(): List<Account>
    suspend fun getAccountByAccountId(accountId: Long): Account?
    suspend fun addAccount(account: Account): Long
    suspend fun addOrUpdateAccount(account: Account): Long
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(account: Account)

    fun getAllProfilesAsFlow(): Flow<List<Profile>>
    fun getProfileByAccountIdAsFlow(accountId: Long): Flow<Profile?>
    suspend fun getAllProfiles(): List<Profile>
    suspend fun getProfileByAccountId(accountId: Long): Profile?
    suspend fun addProfile(profile: Profile): Long
    suspend fun addOrUpdateProfile(profile: Profile): Long
    suspend fun updateProfile(profile: Profile)
    suspend fun deleteProfile(profile: Profile)
}
