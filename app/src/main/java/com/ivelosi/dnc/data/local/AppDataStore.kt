package com.ivelosi.dnc.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.ivelosi.dnc.data.local.account.AccountEntity
import com.ivelosi.dnc.data.local.profile.ProfileEntity
import com.ivelosi.dnc.data.local.serializer.AccountSerializer
import com.ivelosi.dnc.data.local.serializer.ProfileSerializer

abstract class AppDataStore {
    companion object {
        private val Context.accountDataStore by dataStore(
            fileName = "account.json",
            serializer = AccountSerializer
        )

        private val Context.profileDataStore by dataStore(
            fileName = "profile.json",
            serializer = ProfileSerializer
        )

        fun getAccountRepository(context: Context): DataStore<AccountEntity> {
            return context.accountDataStore
        }

        fun getProfileRepository(context: Context): DataStore<ProfileEntity> {
            return context.profileDataStore
        }
    }
}