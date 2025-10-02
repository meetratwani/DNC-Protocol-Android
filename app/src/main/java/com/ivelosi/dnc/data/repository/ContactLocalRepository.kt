package com.ivelosi.dnc.data.repository

import com.ivelosi.dnc.data.local.account.AccountDAO
import com.ivelosi.dnc.data.local.profile.ProfileDAO
import com.ivelosi.dnc.domain.model.device.Account
import com.ivelosi.dnc.domain.model.device.Contact
import com.ivelosi.dnc.domain.model.device.Profile
import com.ivelosi.dnc.domain.model.device.toAccount
import com.ivelosi.dnc.domain.model.device.toAccountEntity
import com.ivelosi.dnc.domain.model.device.toProfile
import com.ivelosi.dnc.domain.model.device.toProfileEntity
import com.ivelosi.dnc.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ContactLocalRepository(private val accountDAO: AccountDAO, private val profileDAO: ProfileDAO) : ContactRepository {
    override fun getAllContactsAsFlow(): Flow<List<Contact>> {
        val accountsFlow = accountDAO.getAllAccountsAsFlow().map { accountEntities ->
            accountEntities.map { it.toAccount() }
        }

        val profilesFlow = profileDAO.getAllProfilesAsFlow().map { profileEntities ->
            profileEntities.map { it.toProfile() }
        }

        return combine(accountsFlow, profilesFlow) { accounts, profiles ->
            accounts.map { account ->
                Contact(account, profiles.find { it.Nid == account.Nid })
            }
        }
    }

    override fun getContactByNidAsFlow(Nid: Long): Flow<Contact?> {
        val accountFlow = accountDAO.getAccountByNidAsFlow(Nid).map { accountEntity ->
            accountEntity?.toAccount()
        }

        val profileFlow = profileDAO.getProfileByNidAsFlow(Nid).map { profileEntity ->
            profileEntity?.toProfile()
        }

        return combine(accountFlow, profileFlow) { account, profile ->
            if(account != null && profile != null) {
                Contact(account, profile)
            } else {
                null
            }
        }
    }

    override fun getAllAccountsAsFlow(): Flow<List<Account>> {
        return accountDAO.getAllAccountsAsFlow().map { accountEntities ->
            accountEntities.map { it.toAccount() }
        }
    }

    override fun getAccountByNidAsFlow(Nid: Long): Flow<Account?> {
        return accountDAO.getAccountByNidAsFlow(Nid).map { it?.toAccount() }
    }

    override suspend fun getAllAccounts(): List<Account> {
        return accountDAO.getAllAccounts().map { it.toAccount() }
    }

    override suspend fun getAccountByNid(Nid: Long): Account? {
        return accountDAO.getAccountByNid(Nid)?.toAccount()
    }

    override suspend fun addAccount(account: Account): Long {
        return accountDAO.insertAccount(account.toAccountEntity())
    }

    override suspend fun addOrUpdateAccount(account: Account): Long {
        return accountDAO.insertOrUpdateAccount(account.toAccountEntity())
    }

    override suspend fun updateAccount(account: Account) {
        accountDAO.updateAccount(account.toAccountEntity())
    }

    override suspend fun deleteAccount(account: Account) {
        accountDAO.deleteAccount(account.toAccountEntity())
    }

    override fun getAllProfilesAsFlow(): Flow<List<Profile>> {
        return profileDAO.getAllProfilesAsFlow().map { profileEntities ->
            profileEntities.map { it.toProfile() }
        }
    }

    override fun getProfileByNidAsFlow(Nid: Long): Flow<Profile?> {
        return profileDAO.getProfileByNidAsFlow(Nid).map { it?.toProfile() }
    }

    override suspend fun getAllProfiles(): List<Profile> {
        return profileDAO.getAllProfiles().map { it.toProfile() }
    }

    override suspend fun getProfileByNid(Nid: Long): Profile? {
        return profileDAO.getProfileByNid(Nid)?.toProfile()
    }

    override suspend fun addProfile(profile: Profile): Long {
        return profileDAO.insertProfile(profile.toProfileEntity())
    }

    override suspend fun addOrUpdateProfile(profile: Profile): Long {
        return profileDAO.insertOrUpdateProfile(profile.toProfileEntity())
    }

    override suspend fun updateProfile(profile: Profile) {
        profileDAO.updateProfile(profile.toProfileEntity())
    }

    override suspend fun deleteProfile(profile: Profile) {
        profileDAO.deleteProfile(profile.toProfileEntity())
    }

}