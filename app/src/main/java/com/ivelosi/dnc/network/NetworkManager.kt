package com.ivelosi.dnc.network

import android.net.Uri
import com.ivelosi.dnc.HandlerFactory
import com.ivelosi.dnc.data.local.FileManager
import com.ivelosi.dnc.domain.model.device.Account
import com.ivelosi.dnc.domain.model.device.Device
import com.ivelosi.dnc.domain.model.device.Profile
import com.ivelosi.dnc.domain.model.device.toAccount
import com.ivelosi.dnc.domain.model.device.toNetworkDevice
import com.ivelosi.dnc.domain.model.message.AudioMessage
import com.ivelosi.dnc.domain.model.message.FileMessage
import com.ivelosi.dnc.domain.model.message.MessageState
import com.ivelosi.dnc.domain.model.message.TextMessage
import com.ivelosi.dnc.domain.model.message.toNetworkTextMessage
import com.ivelosi.dnc.domain.repository.ChatRepository
import com.ivelosi.dnc.domain.repository.ContactRepository
import com.ivelosi.dnc.domain.repository.OwnAccountRepository
import com.ivelosi.dnc.domain.repository.OwnProfileRepository
import com.ivelosi.dnc.network.model.call.NetworkCallEnd
import com.ivelosi.dnc.network.model.call.NetworkCallRequest
import com.ivelosi.dnc.network.model.call.NetworkCallResponse
import com.ivelosi.dnc.network.model.device.NetworkDevice
import com.ivelosi.dnc.network.model.device.NetworkProfile
import com.ivelosi.dnc.network.model.keepalive.NetworkKeepalive
import com.ivelosi.dnc.network.model.message.NetworkAudioMessage
import com.ivelosi.dnc.network.model.message.NetworkFileMessage
import com.ivelosi.dnc.network.model.message.NetworkMessageAck
import com.ivelosi.dnc.network.model.message.NetworkTextMessage
import com.ivelosi.dnc.network.model.profile.NetworkProfileRequest
import com.ivelosi.dnc.network.model.profile.NetworkProfileResponse
import com.ivelosi.dnc.network.service.ClientService
import com.ivelosi.dnc.network.service.ServerService
import com.ivelosi.dnc.network.wifidirect.WiFiDirectBroadcastReceiver
import com.ivelosi.dnc.network.wifidirect.WiFiDirectBroadcastReceiver.Companion.IP_GROUP_OWNER
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File


class NetworkManager(
    ownAccountRepository: OwnAccountRepository,
    ownProfileRepository: OwnProfileRepository,
    private val handlerFactory: HandlerFactory,
    val receiver: WiFiDirectBroadcastReceiver,
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    private val fileManager: FileManager
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var ownDevice = Device(null, 0, Account(0, 0), Profile(0, 0, "", null))

    private val _connectedDevices: MutableStateFlow<List<NetworkDevice>> = MutableStateFlow(listOf())
    val connectedDevices: StateFlow<List<NetworkDevice>>
        get() = _connectedDevices

    private val _callRequest: MutableStateFlow<NetworkCallRequest?> = MutableStateFlow(null)
    val callRequest: StateFlow<NetworkCallRequest?>
        get() = _callRequest

    private val _callResponse: MutableStateFlow<NetworkCallResponse?> = MutableStateFlow(null)
    val callResponse: StateFlow<NetworkCallResponse?>
        get() = _callResponse

    private val _callEnd: MutableStateFlow<NetworkCallEnd?> = MutableStateFlow(null)
    val callEnd: StateFlow<NetworkCallEnd?>
        get() = _callEnd


    private val _callFragment: MutableStateFlow<ByteArray?> = MutableStateFlow(null)
    val callFragment: StateFlow<ByteArray?>
        get() = _callFragment

    private val serverService = ServerService()
    private val clientService = ClientService()

    init {
        coroutineScope.launch {
            ownAccountRepository.getAccountAsFlow().collect {
                ownDevice = ownDevice.copy(account = it)
            }
        }

        coroutineScope.launch {
            ownProfileRepository.getProfileAsFlow().collect {
                ownDevice = ownDevice.copy(profile = it)
            }
        }
    }

    fun startDiscoverPeersHandler() {
        val handler = handlerFactory.buildHandler()

        val runnable = object : Runnable {
            override fun run() {
                receiver.discoverPeers()
                handler.postDelayed(this, 5000)
            }
        }

        handler.post(runnable)
    }

    fun startSendKeepaliveHandler() {
        val handler = handlerFactory.buildHandler()

        val runnable = object : Runnable {
            override fun run() {
                sendKeepalive()
                handler.postDelayed(this, 1000)
            }
        }

        handler.post(runnable)
    }

    fun startUpdateConnectedDevicesHandler() {
        val handler = handlerFactory.buildHandler()

        val runnable = object : Runnable {
            override fun run() {
                updateConnectedDevices()
                handler.postDelayed(this, 1000)
            }
        }

        handler.post(runnable)
    }

    fun updateConnectedDevices() {
        val currentTimestamp = System.currentTimeMillis()
        _connectedDevices.value = _connectedDevices.value.filter { currentTimestamp - it.keepalive <= 3000 }
    }

    fun resetCallStateFlows() {
        _callRequest.value = null
        _callResponse.value = null
        _callEnd.value = null
    }

    fun sendKeepalive() {
        coroutineScope.launch {
            ownDevice.keepalive = System.currentTimeMillis()
            val networkKeepalive = NetworkKeepalive(connectedDevices.value + ownDevice.toNetworkDevice())

            if(ownDevice.ipAddress != IP_GROUP_OWNER) {
                clientService.sendKeepalive(IP_GROUP_OWNER, ownDevice, networkKeepalive)
            }

            connectedDevices.value.forEach { device ->
                device.ipAddress?.let {
                    clientService.sendKeepalive(it, ownDevice, networkKeepalive)
                }
            }
        }
    }

    fun sendProfileRequest(Nid: Long) {
        val connectedDevice = connectedDevices.value.find { it.account.Nid == Nid }

        connectedDevice?.let { device ->
            device.ipAddress?.let { ipAddress ->
                coroutineScope.launch {
                    val networkProfileRequest = NetworkProfileRequest(ownDevice.account.Nid, Nid)
                    clientService.sendProfileRequest(ipAddress, ownDevice, networkProfileRequest)
                }
            }
        }
    }

    fun sendProfileResponse(Nid: Long) {
        val connectedDevice = connectedDevices.value.find { it.account.Nid == Nid }

        connectedDevice?.let { device ->
            device.ipAddress?.let { ipAddress ->
                coroutineScope.launch {
                    val imageBase64 = ownDevice.profile.imageFileName?.let { fileName ->
                        fileManager.getFileBase64(fileName)
                    }

                    val networkProfile = NetworkProfile(ownDevice.profile, imageBase64)
                    val networkProfileResponse = NetworkProfileResponse(ownDevice.account.Nid, Nid, networkProfile)
                    clientService.sendProfileResponse(ipAddress, ownDevice, networkProfileResponse)
                }
            }
        }
    }

    fun sendTextMessage(Nid: Long, text: String) {
        val connectedDevice = connectedDevices.value.find { it.account.Nid == Nid }

        connectedDevice?.let { device ->
            device.ipAddress?.let { ipAddress ->
                coroutineScope.launch {
                    var textMessage = TextMessage(0, ownDevice.account.Nid, Nid, System.currentTimeMillis(), MessageState.MESSAGE_SENT, text)
                    textMessage = textMessage.copy(messageId = chatRepository.addMessage(textMessage))
                    clientService.sendTextMessage(ipAddress, ownDevice, textMessage.toNetworkTextMessage())
                }
            }
        }
    }

    fun sendFileMessage(Nid: Long, fileUri: Uri) {
        val connectedDevice = connectedDevices.value.find { it.account.Nid == Nid }

        connectedDevice?.let { device ->
            device.ipAddress?.let { ipAddress ->
                coroutineScope.launch {
                    fileManager.saveMessageFile(fileUri)?.let { fileName ->
                        var fileMessage = FileMessage(0, ownDevice.account.Nid, Nid, System.currentTimeMillis(), MessageState.MESSAGE_SENT, fileName)
                        fileMessage = fileMessage.copy(messageId = chatRepository.addMessage(fileMessage))

                        fileManager.getFileBase64(fileName)?.let { fileBase64 ->
                            clientService.sendFileMessage(ipAddress, ownDevice, NetworkFileMessage(fileMessage, fileBase64))

                        }
                    }
                }
            }
        }
    }

    fun sendAudioMessage(Nid: Long, file: File) {
        val connectedDevice = connectedDevices.value.find { it.account.Nid == Nid }

        connectedDevice?.let { device ->
            device.ipAddress?.let { ipAddress ->
                coroutineScope.launch {
                    val currentTimestamp = System.currentTimeMillis()

                    fileManager.saveMessageAudio(Uri.fromFile(file), Nid, currentTimestamp)?.let { fileName ->
                        var audioMessage = AudioMessage(0, ownDevice.account.Nid, Nid, currentTimestamp, MessageState.MESSAGE_SENT, fileName)
                        audioMessage = audioMessage.copy(messageId = chatRepository.addMessage(audioMessage))

                        fileManager.getFileBase64(fileName)?.let { audioBase64 ->
                            clientService.sendAudioMessage(ipAddress, ownDevice, NetworkAudioMessage(audioMessage, audioBase64))
                        }
                    }
                }
            }
        }
    }

    fun sendMessageReceivedAck(Nid: Long, messageId: Long) {
        val connectedDevice = connectedDevices.value.find { it.account.Nid == Nid }

        connectedDevice?.let { device ->
            device.ipAddress?.let { ipAddress ->
                coroutineScope.launch {
                    val networkMessageAck = NetworkMessageAck(messageId, ownDevice.account.Nid, Nid)
                    clientService.sendMessageReceivedAck(ipAddress, ownDevice, networkMessageAck)
                }
            }
        }
    }

    fun sendMessageReadAck(Nid: Long, messageId: Long) {
        val connectedDevice = connectedDevices.value.find { it.account.Nid == Nid }

        connectedDevice?.let { device ->
            device.ipAddress?.let { ipAddress ->
                coroutineScope.launch {
                    val networkMessageAck = NetworkMessageAck(messageId, ownDevice.account.Nid, Nid)
                    clientService.sendMessageReadAck(ipAddress, ownDevice, networkMessageAck)
                }
            }
        }
    }

    fun sendCallRequest(Nid: Long) {
        val connectedDevice = connectedDevices.value.find { it.account.Nid == Nid }

        connectedDevice?.let { device ->
            device.ipAddress?.let { ipAddress ->
                coroutineScope.launch {
                    val networkCallRequest = NetworkCallRequest(ownDevice.account.Nid, Nid)
                    clientService.sendCallRequest(ipAddress, ownDevice, networkCallRequest)
                }
            }
        }
    }

    fun sendCallResponse(Nid: Long, accepted: Boolean) {
        val connectedDevice = connectedDevices.value.find { it.account.Nid == Nid }

        connectedDevice?.let { device ->
            device.ipAddress?.let { ipAddress ->
                coroutineScope.launch {
                    val networkCallResponse = NetworkCallResponse(ownDevice.account.Nid, Nid, accepted)
                    clientService.sendCallResponse(ipAddress, ownDevice, networkCallResponse)
                }
            }
        }
    }

    fun sendCallEnd(Nid: Long) {
        val connectedDevice = connectedDevices.value.find { it.account.Nid == Nid }

        connectedDevice?.let { device ->
            device.ipAddress?.let { ipAddress ->
                coroutineScope.launch {
                    val networkCallEnd = NetworkCallEnd(ownDevice.account.Nid, Nid)
                    clientService.sendCallEnd(ipAddress, ownDevice, networkCallEnd)
                }
            }
        }
    }

    fun sendCallFragment(Nid: Long, callFragment: ByteArray) {
        val connectedDevice = connectedDevices.value.find { it.account.Nid == Nid }

        connectedDevice?.let { device ->
            device.ipAddress?.let { ipAddress ->
                coroutineScope.launch {
                    clientService.sendCallFragment(ipAddress, ownDevice, callFragment)
                }
            }
        }
    }

    fun startConnections() {
        startKeepaliveConnection()
        startTextMessageConnection()
        startFileMessageConnection()
        startProfileRequestConnection()
        startProfileResponseConnection()
        startAudioMessageConnection()
        startMessageReceivedAckConnection()
        startMessageReadAckConnection()
        startCallRequestConnection()
        startCallResponseConnection()
        startCallEndConnection()
        startCallFragmentConnection()
    }

    private fun startKeepaliveConnection() {
        coroutineScope.launch {
            while(true) {
                val networkKeepalive = serverService.listenKeepalive()

                networkKeepalive?.networkDevices?.forEach { networkDevice ->
                    if(networkDevice.account.Nid != ownDevice.account.Nid) {
                        handleDeviceKeepalive(networkDevice)
                    }
                }
            }
        }
    }

    private fun startProfileRequestConnection() {
        coroutineScope.launch {
            while(true) {
                val networkProfileRequest = serverService.listenProfileRequest()

                if(networkProfileRequest?.receiverId == ownDevice.account.Nid) {
                    handleProfileRequest(networkProfileRequest)
                }
            }
        }
    }

    private fun startProfileResponseConnection() {
        coroutineScope.launch {
            while(true) {
                val networkProfileResponse = serverService.listenProfileResponse()

                if(networkProfileResponse?.receiverId == ownDevice.account.Nid) {
                    handleProfileResponse(networkProfileResponse)
                }
            }
        }
    }

    private fun startTextMessageConnection() {
        coroutineScope.launch {
            while(true) {
                val networkTextMessage = serverService.listenTextMessage()

                if(networkTextMessage?.receiverId == ownDevice.account.Nid) {
                    handleTextMessage(networkTextMessage)
                }
            }
        }
    }
    
    private fun startFileMessageConnection() {
        coroutineScope.launch { 
            while(true) {
                val networkFileMessage = serverService.listenFileMessage()

                if(networkFileMessage?.receiverId == ownDevice.account.Nid) {
                    handleFileMessage(networkFileMessage)
                }
            }
        }
    }

    private fun startAudioMessageConnection() {
        coroutineScope.launch {
            while(true) {
                val networkAudioMessage = serverService.listenAudioMessage()

                if(networkAudioMessage?.receiverId == ownDevice.account.Nid) {
                    handleAudioMessage(networkAudioMessage)
                }
            }
        }
    }

    private fun startMessageReceivedAckConnection() {
        coroutineScope.launch {
            while(true) {
                val networkMessageAck = serverService.listenMessageReceivedAck()

                if(networkMessageAck?.receiverId == ownDevice.account.Nid) {
                    handleMessageReceivedAck(networkMessageAck)
                }
            }
        }
    }

    private fun startMessageReadAckConnection() {
        coroutineScope.launch {
            while(true) {
                val networkMessageAck = serverService.listenMessageReadAck()

                if(networkMessageAck?.receiverId == ownDevice.account.Nid) {
                    handleMessageReadAck(networkMessageAck)
                }
            }
        }
    }

    private fun startCallRequestConnection() {
        coroutineScope.launch {
            while(true) {
                val networkCallRequest = serverService.listenCallRequest()

                if(networkCallRequest?.receiverId == ownDevice.account.Nid) {
                    handleCallRequest(networkCallRequest)
                }
            }
        }
    }

    private fun startCallResponseConnection() {
        coroutineScope.launch {
            while(true) {
                val networkCallResponse = serverService.listenCallResponse()

                if(networkCallResponse?.receiverId == ownDevice.account.Nid) {
                    handleCallResponse(networkCallResponse)
                }
            }
        }
    }


    private fun startCallEndConnection() {
        coroutineScope.launch {
            while(true) {
                val networkCallEnd = serverService.listenCallEnd()

                if(networkCallEnd?.receiverId == ownDevice.account.Nid) {
                    handleCallEnd(networkCallEnd)
                }
            }
        }
    }


    private fun startCallFragmentConnection() {
        coroutineScope.launch {
            while(true) {
                serverService.listenCallFragment()?.let { callFragment ->
                    handleCallFragment(callFragment)
                }
            }
        }
    }

    private fun handleDeviceKeepalive(networkDevice: NetworkDevice) {
        coroutineScope.launch {
            val lastAccount = contactRepository.getAccountByNid(networkDevice.account.Nid)
            contactRepository.addOrUpdateAccount(networkDevice.account.toAccount())

            val profile = contactRepository.getProfileByNid(networkDevice.account.Nid)

            if(profile == null || (lastAccount != null && lastAccount.profileUpdateTimestamp < networkDevice.account.profileUpdateTimestamp)) {
                sendProfileRequest(networkDevice.account.Nid)
            }

            _connectedDevices.value = _connectedDevices.value.filter { it.account.Nid != networkDevice.account.Nid } + networkDevice
        }
    }

    private fun handleProfileRequest(networkProfileRequest: NetworkProfileRequest) {
        coroutineScope.launch {
            sendProfileResponse(networkProfileRequest.senderId)
        }
    }

    private fun handleProfileResponse(networkProfileResponse: NetworkProfileResponse) {
        coroutineScope.launch {
            val imageFileName = networkProfileResponse.profile.imageBase64?.let {
                fileManager.saveNetworkProfileImage(networkProfileResponse.profile)
            }

            contactRepository.addOrUpdateProfile(Profile(networkProfileResponse.profile, imageFileName))
        }
    }

    private fun handleTextMessage(networkTextMessage: NetworkTextMessage) {
        coroutineScope.launch {
            chatRepository.addMessage(TextMessage(networkTextMessage, MessageState.MESSAGE_RECEIVED))
            sendMessageReceivedAck(networkTextMessage.senderId, networkTextMessage.messageId)
        }
    }
    
    private fun handleFileMessage(networkFileMessage: NetworkFileMessage) {
        coroutineScope.launch {
            fileManager.saveNetworkFile(networkFileMessage)?.let {
                chatRepository.addMessage(FileMessage(networkFileMessage, MessageState.MESSAGE_RECEIVED))
                sendMessageReceivedAck(networkFileMessage.senderId, networkFileMessage.messageId)
            }
        }
    }

    private fun handleAudioMessage(networkAudioMessage: NetworkAudioMessage) {
        coroutineScope.launch {
            fileManager.saveNetworkAudio(networkAudioMessage)?.let { fileName ->
                chatRepository.addMessage(AudioMessage(networkAudioMessage, MessageState.MESSAGE_RECEIVED, fileName))
                sendMessageReceivedAck(networkAudioMessage.senderId, networkAudioMessage.messageId)
            }
        }
    }

    private fun handleMessageReceivedAck(networkMessageAck: NetworkMessageAck) {
        coroutineScope.launch {
            chatRepository.getMessageByMessageId(networkMessageAck.messageId)?.let { message ->
                if(message.messageState < MessageState.MESSAGE_RECEIVED) {
                    chatRepository.updateMessageState(message.messageId, MessageState.MESSAGE_RECEIVED)
                }
            }
        }
    }

    private fun handleMessageReadAck(networkMessageAck: NetworkMessageAck) {
        coroutineScope.launch {
            chatRepository.getAllMessagesByReceiverNid(networkMessageAck.senderId).forEach { message ->
                if(message.messageState < MessageState.MESSAGE_READ) {
                    chatRepository.updateMessageState(message.messageId, MessageState.MESSAGE_READ)
                }
            }
        }
    }

    private fun handleCallRequest(networkCallRequest: NetworkCallRequest) {
        resetCallStateFlows()
        _callRequest.value = networkCallRequest
    }

    private fun handleCallResponse(networkCallResponse: NetworkCallResponse) {
        resetCallStateFlows()
        _callResponse.value = networkCallResponse
    }

    private fun handleCallEnd(networkCallEnd: NetworkCallEnd) {
        resetCallStateFlows()
        _callEnd.value = networkCallEnd
    }

    private fun handleCallFragment(callFragment: ByteArray) {
        _callFragment.value = callFragment
    }
}