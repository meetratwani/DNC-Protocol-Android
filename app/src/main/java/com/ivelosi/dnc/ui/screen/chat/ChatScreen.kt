package com.ivelosi.dnc.ui.screen.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ivelosi.dnc.R
import com.ivelosi.dnc.domain.model.message.AudioMessage
import com.ivelosi.dnc.domain.model.message.FileMessage
import com.ivelosi.dnc.domain.model.message.Message
import com.ivelosi.dnc.domain.model.message.TextMessage
import com.ivelosi.dnc.ui.ChatTopAppBar
import com.ivelosi.dnc.ui.components.AudioMessageComponent
import com.ivelosi.dnc.ui.components.AudioRecordingControls
import com.ivelosi.dnc.ui.components.FileMessageComponent
import com.ivelosi.dnc.ui.components.FileMessageInput
import com.ivelosi.dnc.ui.components.TextMessageComponent
import com.ivelosi.dnc.ui.components.TextMessageInput
import com.ivelosi.dnc.ui.navigation.NavigationDestination
import com.ivelosi.dnc.ui.screen.call.CallDestination
import com.ivelosi.dnc.ui.screen.call.CallState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ChatDestination : NavigationDestination {
    override val route = "chat"
    override val titleRes = R.string.chat_screen
    const val NidArg = "Nid"
    val routeWithArgs = "$route/{$NidArg}"
}

@Composable
fun ChatScreen(
    Nid: Long,
    chatViewModel: ChatViewModel,
    navController: NavHostController,
    onInfoButtonClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val callRequest by chatViewModel.networkManager.callRequest.collectAsState()

    LaunchedEffect(callRequest) {
        callRequest?.let {
            navController.navigate("${CallDestination.route}/${it.senderId}/${CallState.RECEIVED_CALL_REQUEST.name}")
        }
    }

    val currentAccount by chatViewModel.ownAccount.collectAsState(initial = null)
    val chatState by chatViewModel.uiState.collectAsState()

    chatViewModel.updateMessagesState(chatState.messages)

    Scaffold(
        topBar = {
            ChatTopAppBar(
                title = chatState.contact.username ?: "Connecting...",
                canNavigateBack = true,
                onCallButtonClick = {
                    chatViewModel.sendCallRequest()
                    navController.navigate("${CallDestination.route}/${Nid}/${CallState.SENT_CALL_REQUEST.name}")
                },
                onInfoButtonClick = { onInfoButtonClick(Nid) },
                modifier = modifier,
                navigateUp = { navController.navigateUp() },
                contactImageFileName = chatState.contact.imageFileName
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            currentAccount?.let { account ->
                MessagesList(
                    messages = chatState.messages,
                    chatViewModel = chatViewModel,
                    Nid = account.Nid,
                    modifier = Modifier.weight(1f)
                )

                SendMessageInput(
                    onStartRecording = {
                        chatViewModel.startRecordingAudio()
                    },
                    onStopRecording = {
                        chatViewModel.stopRecordingAudio()
                    },
                    onCancelRecording = {
                        chatViewModel.stopRecordingAudio()
                    },
                    onSendTextMessage = { messageText ->
                        chatViewModel.sendTextMessage(messageText)
                    },
                    onSendFileMessage = { fileUri ->
                        chatViewModel.sendFileMessage(fileUri)
                    },
                    onSendAudioMessage = {
                        chatViewModel.sendAudioMessage()
                    }
                )
            }
        }
    }
}

@Composable
fun MessagesList(
    messages: List<Message>, chatViewModel: ChatViewModel, Nid: Long, modifier: Modifier
) {

    val listState = rememberLazyListState()

    val groupedMessages = remember(messages) { groupMessagesByDate(messages) }

    LaunchedEffect(groupedMessages) {
        if (groupedMessages.isNotEmpty()) {
            listState.animateScrollToItem(groupedMessages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        items(groupedMessages) { item ->
            when (item) {
                is String -> DateSeparator(date = item) // Se è una data
                is Message -> MessageItem(
                    message = item,
                    Nid = Nid,
                    chatViewModel = chatViewModel
                )
            }
        }
    }
}

@Composable
fun MessageItem(message: Message, Nid: Long, chatViewModel: ChatViewModel) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        when (message) {
            is TextMessage -> {
                TextMessageComponent(message, currentNid = Nid)
            }

            is FileMessage -> {
                FileMessageComponent(message, currentNid = Nid)
            }

            is AudioMessage -> {
                AudioMessageComponent(
                    message,
                    currentNid = Nid,
                    startPlayingAudio = { fileName, startPosition ->
                        chatViewModel.startPlayingAudio(
                            File(context.filesDir, fileName),
                            startPosition
                        )
                    },
                    stopPlayingAudio = {
                        chatViewModel.stopPlayingAudio()
                    },
                    getCurrentPlaybackPosition = {
                        chatViewModel.getCurrentPlaybackPosition()
                    },
                    isPlaybackComplete = { chatViewModel.isPlaybackComplete() }
                )
            }
        }
    }
}

@Composable
fun SendMessageInput(
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onSendTextMessage: (String) -> Unit,
    onSendFileMessage: (Uri) -> Unit,
    onSendAudioMessage: () -> Unit,
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue()) }
    var isRecording by remember { mutableStateOf(false) }
    var isTyping by remember { mutableStateOf(false) }

    var attachedFileUri by remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            attachedFileUri = it
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp, bottom = 10.dp)
    ) {

        if (!isTyping && !isRecording) {
            FileMessageInput(
                fileUri = attachedFileUri,
                onSendFile = { uri ->
                    onSendFileMessage(uri)
                    attachedFileUri = null
                },
                onClick = {
                    filePickerLauncher.launch("*/*")
                },
                onDeleteFile = {
                    attachedFileUri = null
                }
            )
        }

        /* INPUT DI TESTO */
        if (attachedFileUri == null && !isRecording) {
            TextMessageInput(
                isTyping = isTyping,
                textFieldValue = textFieldValue,
                onValueChange = {
                    textFieldValue = it
                    isTyping = it.text.isNotEmpty()
                },
                onSendTextMessage = onSendTextMessage,
                modifier = Modifier.weight(1f)
            )
        }

        /* BOTTONE REGISTRAZIONE E GESTIONE AUDIO */
        if ((!isTyping && attachedFileUri == null) || isRecording) {
            AudioRecordingControls(
                isRecording = isRecording,
                onStartRecording = {
                    onStartRecording()
                    isRecording = true
                },
                onStopRecording = {
                    onStopRecording()
                    isRecording = false
                },
                onCancelRecording = {
                    onCancelRecording()
                    isRecording = false
                },
                onSendAudioMessage = {
                    onSendAudioMessage()
                    isRecording = false
                },
                modifier = Modifier
            )
        }
    }
}

@Composable
fun DateSeparator(date: String) {
    Text(
        text = date,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        textAlign = TextAlign.Center
    )
}

fun groupMessagesByDate(messages: List<Message>): List<Any> {
    val groupedMessages = mutableListOf<Any>()
    var lastDate: String? = null
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    for (message in messages) {
        val messageDate = dateFormat.format(Date(message.timestamp))

        if (messageDate != lastDate) {
            lastDate = messageDate
            groupedMessages.add(messageDate)
        }
        groupedMessages.add(message)
    }

    return groupedMessages
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    SendMessageInput(
        onStartRecording = {},
        onStopRecording = {},
        onCancelRecording = {},
        onSendTextMessage = {},
        onSendFileMessage = {},
        onSendAudioMessage = {}
    )
}
