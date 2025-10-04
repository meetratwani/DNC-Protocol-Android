package com.ivelosi.dnc.security

import android.util.Log
import com.ivelosi.dnc.security.E2EEManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Handler for encrypted messages in the mesh network
 * Wraps E2EEManager to provide high-level encryption/decryption for different message types
 */
class EncryptedMessageHandler(private val e2eeManager: E2EEManager) {

    companion object {
        private const val TAG = "EncryptedMessageHandler"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Message wrapper with metadata
     */
    @Serializable
    data class EncryptedMessage(
        val senderId: Long,
        val receiverId: Long,
        val messageType: MessageType,
        val encryptedPayload: String,
        val timestamp: Long,
        val signature: String? = null // Optional signature for verification
    )

    @Serializable
    enum class MessageType {
        TEXT,           // Plain text messages
        FILE,           // File transfers
        VOICE,          // Voice messages
        SYSTEM,         // System messages (keepalive, routing, etc.)
        KEY_EXCHANGE    // Key exchange messages
    }

    // ======================== PEER DISCOVERY & KEY EXCHANGE ========================

    /**
     * Step 1: Send key exchange request when discovering a new peer
     */
    suspend fun initiateKeyExchange(myNodeId: Long, peerNodeId: Long): String {
        val keyExchangeMsg = e2eeManager.createKeyExchangeRequest(myNodeId)
        val serialized = e2eeManager.serializeKeyExchange(keyExchangeMsg)

        Log.d(TAG, "Initiated key exchange with peer $peerNodeId")
        return serialized
    }

    /**
     * Step 2: Process received key exchange and send response
     */
    suspend fun handleKeyExchangeRequest(
        myNodeId: Long,
        peerNodeId: Long,
        keyExchangeJson: String
    ): String? {
        return try {
            val keyExchangeMsg = e2eeManager.deserializeKeyExchange(keyExchangeJson)

            // Establish session with peer
            val success = e2eeManager.processKeyExchange(peerNodeId, keyExchangeMsg.publicKey)

            if (success) {
                Log.d(TAG, "Key exchange processed for peer $peerNodeId")
                // Send our public key back
                val response = e2eeManager.createKeyExchangeRequest(myNodeId)
                e2eeManager.serializeKeyExchange(response)
            } else {
                Log.e(TAG, "Failed to process key exchange for peer $peerNodeId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling key exchange request", e)
            null
        }
    }

    /**
     * Step 3: Complete key exchange by processing response
     */
    suspend fun completeKeyExchange(peerNodeId: Long, responseJson: String): Boolean {
        return try {
            val keyExchangeMsg = e2eeManager.deserializeKeyExchange(responseJson)
            val success = e2eeManager.processKeyExchange(peerNodeId, keyExchangeMsg.publicKey)

            if (success) {
                Log.d(TAG, "Key exchange completed with peer $peerNodeId")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error completing key exchange", e)
            false
        }
    }

    // ======================== SENDING ENCRYPTED MESSAGES ========================

    /**
     * Send encrypted text message
     */
    fun sendTextMessage(
        senderId: Long,
        receiverId: Long,
        message: String
    ): String? {
        if (!e2eeManager.hasSession(receiverId)) {
            Log.w(TAG, "No active session with peer $receiverId. Initiate key exchange first.")
            return null
        }

        return try {
            val encrypted = e2eeManager.encryptMessage(receiverId, message)
            if (encrypted == null) {
                Log.e(TAG, "Failed to encrypt message for peer $receiverId")
                return null
            }

            val wrappedMessage = EncryptedMessage(
                senderId = senderId,
                receiverId = receiverId,
                messageType = MessageType.TEXT,
                encryptedPayload = encrypted,
                timestamp = System.currentTimeMillis()
            )

            json.encodeToString(wrappedMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending text message", e)
            null
        }
    }

    /**
     * Send encrypted binary data (files, voice, etc.)
     */
    fun sendBinaryMessage(
        senderId: Long,
        receiverId: Long,
        data: ByteArray,
        messageType: MessageType
    ): String? {
        if (!e2eeManager.hasSession(receiverId)) {
            Log.w(TAG, "No active session with peer $receiverId. Initiate key exchange first.")
            return null
        }

        return try {
            val encrypted = e2eeManager.encryptPacket(receiverId, data)
            if (encrypted == null) {
                Log.e(TAG, "Failed to encrypt data for peer $receiverId")
                return null
            }

            // Encode encrypted bytes as Base64 for JSON transport
            val base64Payload = android.util.Base64.encodeToString(
                encrypted,
                android.util.Base64.NO_WRAP
            )

            val wrappedMessage = EncryptedMessage(
                senderId = senderId,
                receiverId = receiverId,
                messageType = messageType,
                encryptedPayload = base64Payload,
                timestamp = System.currentTimeMillis()
            )

            json.encodeToString(wrappedMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending binary message", e)
            null
        }
    }

    /**
     * Send encrypted system message
     */
    fun sendSystemMessage(
        senderId: Long,
        receiverId: Long,
        systemData: String
    ): String? {
        return sendTextMessage(senderId, receiverId, systemData)
    }

    // ======================== RECEIVING ENCRYPTED MESSAGES ========================

    /**
     * Receive and decrypt any message
     */
    fun receiveMessage(encryptedMessageJson: String): ReceivedMessage? {
        return try {
            val wrappedMessage = json.decodeFromString<EncryptedMessage>(encryptedMessageJson)

            when (wrappedMessage.messageType) {
                MessageType.TEXT, MessageType.SYSTEM -> {
                    val decrypted = e2eeManager.decryptMessage(
                        wrappedMessage.senderId,
                        wrappedMessage.encryptedPayload
                    )

                    if (decrypted != null) {
                        ReceivedMessage.TextMessage(
                            senderId = wrappedMessage.senderId,
                            receiverId = wrappedMessage.receiverId,
                            message = decrypted,
                            messageType = wrappedMessage.messageType,
                            timestamp = wrappedMessage.timestamp
                        )
                    } else {
                        Log.e(TAG, "Failed to decrypt text message from ${wrappedMessage.senderId}")
                        null
                    }
                }

                MessageType.FILE, MessageType.VOICE -> {
                    val encryptedBytes = android.util.Base64.decode(
                        wrappedMessage.encryptedPayload,
                        android.util.Base64.NO_WRAP
                    )

                    val decrypted = e2eeManager.decryptPacket(
                        wrappedMessage.senderId,
                        encryptedBytes
                    )

                    if (decrypted != null) {
                        ReceivedMessage.BinaryMessage(
                            senderId = wrappedMessage.senderId,
                            receiverId = wrappedMessage.receiverId,
                            data = decrypted,
                            messageType = wrappedMessage.messageType,
                            timestamp = wrappedMessage.timestamp
                        )
                    } else {
                        Log.e(TAG, "Failed to decrypt binary message from ${wrappedMessage.senderId}")
                        null
                    }
                }

                MessageType.KEY_EXCHANGE -> {
                    // Key exchange should be handled separately
                    Log.w(TAG, "Received key exchange message in regular message flow")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error receiving message", e)
            null
        }
    }

    // ======================== RECEIVED MESSAGE TYPES ========================

    sealed class ReceivedMessage {
        data class TextMessage(
            val senderId: Long,
            val receiverId: Long,
            val message: String,
            val messageType: MessageType,
            val timestamp: Long
        ) : ReceivedMessage()

        data class BinaryMessage(
            val senderId: Long,
            val receiverId: Long,
            val data: ByteArray,
            val messageType: MessageType,
            val timestamp: Long
        ) : ReceivedMessage() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as BinaryMessage
                if (senderId != other.senderId) return false
                if (receiverId != other.receiverId) return false
                if (!data.contentEquals(other.data)) return false
                if (messageType != other.messageType) return false
                if (timestamp != other.timestamp) return false
                return true
            }

            override fun hashCode(): Int {
                var result = senderId.hashCode()
                result = 31 * result + receiverId.hashCode()
                result = 31 * result + data.contentHashCode()
                result = 31 * result + messageType.hashCode()
                result = 31 * result + timestamp.hashCode()
                return result
            }
        }
    }

    // ======================== SESSION MANAGEMENT ========================

    /**
     * Check if encryption is available for peer
     */
    fun canSendEncrypted(peerNodeId: Long): Boolean {
        return e2eeManager.hasSession(peerNodeId)
    }

    /**
     * Get session information
     */
    fun getSessionInfo(peerNodeId: Long): E2EEManager.SessionInfo? {
        return e2eeManager.getSessionInfo(peerNodeId)
    }

    /**
     * Get fingerprint for manual verification
     */
    fun getMyFingerprint(): String {
        return e2eeManager.getMyPublicKeyFingerprint()
    }

    /**
     * Get peer's fingerprint for manual verification
     */
    fun getPeerFingerprint(peerNodeId: Long): String? {
        val session = e2eeManager.getSessionInfo(peerNodeId)
        return session?.fingerprint
    }

    /**
     * Mark session as verified after manual verification
     */
    suspend fun verifySession(peerNodeId: Long) {
        e2eeManager.markSessionAsVerified(peerNodeId)
    }

    /**
     * Remove session on peer disconnect
     */
    suspend fun removeSession(peerNodeId: Long) {
        e2eeManager.removeSession(peerNodeId)
    }

    /**
     * Get encryption statistics
     */
    fun getStats(): E2EEManager.EncryptionStats {
        return e2eeManager.getStats()
    }
}