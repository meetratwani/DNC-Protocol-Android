package com.ivelosi.dnc.domain.repository

import com.ivelosi.dnc.domain.model.device.Account
import com.ivelosi.dnc.domain.model.device.Contact
import com.ivelosi.dnc.domain.model.device.Profile
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun getAllContactsAsFlow(): Flow<List<Contact>>
    fun getContactByNidAsFlow(Nid: Long): Flow<Contact?>

    fun getAllAccountsAsFlow(): Flow<List<Account>>
    fun getAccountByNidAsFlow(Nid: Long): Flow<Account?>
    suspend fun getAllAccounts(): List<Account>
    suspend fun getAccountByNid(Nid: Long): Account?
    suspend fun addAccount(account: Account): Long
    suspend fun addOrUpdateAccount(account: Account): Long
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(account: Account)

    fun getAllProfilesAsFlow(): Flow<List<Profile>>
    fun getProfileByNidAsFlow(Nid: Long): Flow<Profile?>
    suspend fun getAllProfiles(): List<Profile>
    suspend fun getProfileByNid(Nid: Long): Profile?
    suspend fun addProfile(profile: Profile): Long
    suspend fun addOrUpdateProfile(profile: Profile): Long
    suspend fun updateProfile(profile: Profile)
    suspend fun deleteProfile(profile: Profile)
}
