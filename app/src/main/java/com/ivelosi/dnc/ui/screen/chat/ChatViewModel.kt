package com.ivelosi.dnc.ui.screen.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivelosi.dnc.data.local.FileManager
import com.ivelosi.dnc.domain.model.device.Account
import com.ivelosi.dnc.domain.model.message.Message
import com.ivelosi.dnc.domain.model.message.MessageState
import com.ivelosi.dnc.domain.repository.ChatRepository
import com.ivelosi.dnc.domain.repository.ContactRepository
import com.ivelosi.dnc.domain.repository.OwnAccountRepository
import com.ivelosi.dnc.media.AudioReplayer
import com.ivelosi.dnc.network.NetworkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    private val ownAccountRepository: OwnAccountRepository,
    private val fileManager: FileManager,
    val networkManager: NetworkManager,
    private val Nid: Long
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatViewState())
    val uiState: StateFlow<ChatViewState> = _uiState.asStateFlow()

    val ownAccount: Flow<Account>
        get() = ownAccountRepository.getAccountAsFlow()

    private val audioReplayer = AudioReplayer()

    init {
        viewModelScope.launch {
            contactRepository.getContactByNidAsFlow(Nid).collect { contact ->
                if(contact != null) {
                    _uiState.value = _uiState.value.copy(contact = contact)
                }
            }
        }

        viewModelScope.launch {
            chatRepository.getAllMessagesByNidAsFlow(Nid).collect { messages ->
                _uiState.value = _uiState.value.copy(messages = messages)
            }
        }
    }

    fun sendTextMessage(text: String) {
        networkManager.sendTextMessage(Nid, text)
    }

    fun sendFileMessage(fileUri: Uri) {
        networkManager.sendFileMessage(Nid, fileUri)
    }

    fun sendAudioMessage() {
        audioReplayer.apply {
            if(isRecording) {
                stopRecording()
                recordingFile?.let { networkManager.sendAudioMessage(Nid, it) }
            }
        }
    }

    fun sendCallRequest() {
        networkManager.sendCallRequest(Nid)
    }

    fun startRecordingAudio() = audioReplayer.startRecording(fileManager.getAudioTempFile())
    fun stopRecordingAudio() = audioReplayer.stopRecording()

    fun startPlayingAudio(file: File, startPosition: Int) {
        audioReplayer.startPlaying(file, startPosition)
    }

    fun stopPlayingAudio() = audioReplayer.stopPlaying()

    fun getCurrentPlaybackPosition(): Int {
        return audioReplayer.getCurrentPlaybackPosition()
    }

    fun isPlaybackComplete(): Boolean {
        return audioReplayer.isPlaybackComplete()
    }

    fun updateMessagesState(messages: List<Message>) {
        viewModelScope.launch {
            val ownNid = ownAccountRepository.getAccount().Nid

            val receivedMessages = messages.filter { it.messageState < MessageState.MESSAGE_READ && it.senderId != ownNid }

            receivedMessages.forEach { message ->
                chatRepository.updateMessageState(message.messageId, MessageState.MESSAGE_READ)
            }

            receivedMessages.lastOrNull()?.let { message ->
                networkManager.sendMessageReadAck(message.senderId, message.messageId)
            }
        }
    }
}