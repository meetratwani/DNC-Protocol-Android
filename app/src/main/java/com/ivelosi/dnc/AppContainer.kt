package com.ivelosi.dnc

import android.content.Context
import com.ivelosi.dnc.HandlerFactory
import com.ivelosi.dnc.MainActivity
import com.ivelosi.dnc.data.local.AppDataStore
import com.ivelosi.dnc.data.local.AppDatabase
import com.ivelosi.dnc.data.local.FileManager
import com.ivelosi.dnc.data.repository.ChatLocalRepository
import com.ivelosi.dnc.data.repository.ContactLocalRepository
import com.ivelosi.dnc.data.repository.OwnAccountLocalRepository
import com.ivelosi.dnc.data.repository.OwnProfileLocalRepository
import com.ivelosi.dnc.domain.repository.ChatRepository
import com.ivelosi.dnc.domain.repository.ContactRepository
import com.ivelosi.dnc.domain.repository.OwnAccountRepository
import com.ivelosi.dnc.domain.repository.OwnProfileRepository
import com.ivelosi.dnc.network.CallManager
import com.ivelosi.dnc.network.NetworkManager
import com.ivelosi.dnc.network.wifidirect.WiFiDirectBroadcastReceiver
import com.ivelosi.dnc.security.E2EEManager

interface AppContainer {
    val context: Context
    val handlerFactory: HandlerFactory
    val wiFiDirectBroadcastReceiver: WiFiDirectBroadcastReceiver
    val chatRepository: ChatRepository
    val contactRepository: ContactRepository
    val ownAccountRepository: OwnAccountRepository
    val ownProfileRepository: OwnProfileRepository
    val fileManager: FileManager
    val networkManager: NetworkManager
    val callManager: CallManager
    var e2eeManager: E2EEManager
}

class AppDataContainer(activity: MainActivity) : AppContainer {
    override val context: Context = activity

    override val handlerFactory = HandlerFactory(context)

    override val wiFiDirectBroadcastReceiver = WiFiDirectBroadcastReceiver(context)

    override val chatRepository: ChatRepository by lazy {
        ChatLocalRepository(AppDatabase.getDatabase(context).contactDao(), AppDatabase.getDatabase(context).messageDao(), AppDatabase.getDatabase(context).profileDao())
    }

    override val contactRepository: ContactRepository by lazy {
        ContactLocalRepository(AppDatabase.getDatabase(context).contactDao(), AppDatabase.getDatabase(context).profileDao())
    }

    override val ownAccountRepository: OwnAccountRepository by lazy {
        OwnAccountLocalRepository(AppDataStore.getAccountRepository(context))
    }

    override val ownProfileRepository: OwnProfileRepository by lazy {
        OwnProfileLocalRepository(AppDataStore.getProfileRepository(context))
    }

    override val fileManager: FileManager by lazy {
        FileManager(context)
    }

    override val networkManager: NetworkManager by lazy {
        NetworkManager(ownAccountRepository, ownProfileRepository, handlerFactory, wiFiDirectBroadcastReceiver, chatRepository, contactRepository, fileManager)
    }

    override val callManager: CallManager by lazy {
        CallManager(context)
    }

    override lateinit var e2eeManager: E2EEManager
}