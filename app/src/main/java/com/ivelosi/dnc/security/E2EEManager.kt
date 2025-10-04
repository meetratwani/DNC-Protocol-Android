package com.ivelosi.dnc.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.*
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Complete E2EE Manager for mesh network
 * Handles key generation, exchange, encryption, decryption, and session management
 */
class E2EEManager(private val context: Context) {

    companion object {
        private const val TAG = "E2EEManager"
        private const val KEYSTORE_ALIAS = "dnc_identity_key"
        private const val KEY_SIZE = 256
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
        private const val SESSION_TIMEOUT_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    // Session data for each peer
    private data class PeerSession(
        val peerId: Long,
        val peerPublicKey: String,
        val sharedSecret: ByteArray,
        val createdAt: Long = System.currentTimeMillis(),
        val lastUsed: Long = System.currentTimeMillis(),
        val verified: Boolean = false
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - lastUsed > SESSION_TIMEOUT_MS

        fun touch(): PeerSession = copy(lastUsed = System.currentTimeMillis())
    }

    private val sessions = ConcurrentHashMap<Long, PeerSession>()
    private val sessionMutex = Mutex()
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val json = Json { ignoreUnknownKeys = true }

    // ======================== KEY MANAGEMENT ========================

    /**
     * Initialize identity key pair (call once on app start)
     */
    fun initialize() {
        try {
            getOrCreateIdentityKeyPair()
            Log.d(TAG, "E2EE Manager initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize E2EE Manager", e)
            throw e
        }
    }

    /**
     * Get or create device's identity key pair
     */
    private fun getOrCreateIdentityKeyPair(): KeyPair {
        if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            try {
                val privateKey = keyStore.getKey(KEYSTORE_ALIAS, null) as PrivateKey
                val publicKey = keyStore.getCertificate(KEYSTORE_ALIAS).publicKey
                return KeyPair(publicKey, privateKey)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to retrieve existing key, generating new one", e)
                keyStore.deleteEntry(KEYSTORE_ALIAS)
            }
        }
        return generateIdentityKeyPair()
    }

    private fun generateIdentityKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            "AndroidKeyStore"
        )

        val parameterSpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_AGREE_KEY or KeyProperties.PURPOSE_SIGN
        ).apply {
            setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
            setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
        }.build()

        keyPairGenerator.initialize(parameterSpec)
        val keyPair = keyPairGenerator.generateKeyPair()
        Log.d(TAG, "Generated new identity key pair")
        return keyPair
    }

    /**
     * Get public key as Base64 string for sharing
     */
    fun getMyPublicKey(): String {
        val keyPair = getOrCreateIdentityKeyPair()
        return Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
    }

    /**
     * Get public key fingerprint for verification (first 8 bytes of SHA-256 hash)
     */
    fun getMyPublicKeyFingerprint(): String {
        val publicKey = getMyPublicKey()
        val hash = MessageDigest.getInstance("SHA-256").digest(publicKey.toByteArray())
        return hash.take(8).joinToString("") { "%02x".format(it) }.uppercase()
    }

    /**
     * Get peer's public key fingerprint
     */
    fun getPeerPublicKeyFingerprint(peerPublicKey: String): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(peerPublicKey.toByteArray())
        return hash.take(8).joinToString("") { "%02x".format(it) }.uppercase()
    }

    // ======================== KEY EXCHANGE ========================

    /**
     * Create key exchange request
     */
    fun createKeyExchangeRequest(myNodeId: Long): KeyExchangeMessage {
        return KeyExchangeMessage(
            senderId = myNodeId,
            publicKey = getMyPublicKey(),
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Process incoming key exchange and establish session
     */
    suspend fun processKeyExchange(peerNodeId: Long, peerPublicKey: String): Boolean {
        return try {
            sessionMutex.withLock {
                val sharedSecret = deriveSharedSecret(peerPublicKey)
                val session = PeerSession(
                    peerId = peerNodeId,
                    peerPublicKey = peerPublicKey,
                    sharedSecret = sharedSecret
                )
                sessions[peerNodeId] = session
                Log.d(TAG, "Session established with peer $peerNodeId")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process key exchange with peer $peerNodeId", e)
            false
        }
    }

    /**
     * Derive shared secret using ECDH
     */
    private fun deriveSharedSecret(peerPublicKeyString: String): ByteArray {
        val peerPublicKeyBytes = Base64.decode(peerPublicKeyString, Base64.NO_WRAP)
        val keyFactory = KeyFactory.getInstance("EC")
        val peerPublicKey = keyFactory.generatePublic(X509EncodedKeySpec(peerPublicKeyBytes))

        val keyPair = getOrCreateIdentityKeyPair()
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(keyPair.private)
        keyAgreement.doPhase(peerPublicKey, true)

        val sharedSecret = keyAgreement.generateSecret()
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(sharedSecret)
    }

    // ======================== ENCRYPTION / DECRYPTION ========================

    /**
     * Encrypt byte array using AES-256-GCM
     */
    private fun encryptBytes(data: ByteArray, sharedSecret: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(sharedSecret, "AES")

        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val ciphertext = cipher.doFinal(data)

        // Return: IV + Ciphertext + Auth Tag
        return iv + ciphertext
    }

    /**
     * Decrypt byte array using AES-256-GCM
     */
    private fun decryptBytes(encryptedData: ByteArray, sharedSecret: ByteArray): ByteArray {
        if (encryptedData.size < GCM_IV_LENGTH) {
            throw IllegalArgumentException("Invalid encrypted data")
        }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(sharedSecret, "AES")

        val iv = encryptedData.sliceArray(0 until GCM_IV_LENGTH)
        val ciphertext = encryptedData.sliceArray(GCM_IV_LENGTH until encryptedData.size)

        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        return cipher.doFinal(ciphertext)
    }

    /**
     * Encrypt string message for a peer
     */
    fun encryptMessage(peerNodeId: Long, message: String): String? {
        return try {
            val session = sessions[peerNodeId]?.touch()
            if (session == null) {
                Log.w(TAG, "No session found for peer $peerNodeId")
                return null
            }

            // Update last used timestamp
            sessions[peerNodeId] = session

            val encrypted = encryptBytes(message.toByteArray(Charsets.UTF_8), session.sharedSecret)
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt message for peer $peerNodeId", e)
            null
        }
    }

    /**
     * Decrypt string message from a peer
     */
    fun decryptMessage(peerNodeId: Long, encryptedMessage: String): String? {
        return try {
            val session = sessions[peerNodeId]?.touch()
            if (session == null) {
                Log.w(TAG, "No session found for peer $peerNodeId")
                return null
            }

            // Update last used timestamp
            sessions[peerNodeId] = session

            val encryptedBytes = Base64.decode(encryptedMessage, Base64.NO_WRAP)
            val decrypted = decryptBytes(encryptedBytes, session.sharedSecret)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt message from peer $peerNodeId", e)
            null
        }
    }

    /**
     * Encrypt data packet (for any binary data)
     */
    fun encryptPacket(peerNodeId: Long, data: ByteArray): ByteArray? {
        return try {
            val session = sessions[peerNodeId]?.touch()
            if (session == null) {
                Log.w(TAG, "No session found for peer $peerNodeId")
                return null
            }

            sessions[peerNodeId] = session
            encryptBytes(data, session.sharedSecret)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt packet for peer $peerNodeId", e)
            null
        }
    }

    /**
     * Decrypt data packet (for any binary data)
     */
    fun decryptPacket(peerNodeId: Long, encryptedData: ByteArray): ByteArray? {
        return try {
            val session = sessions[peerNodeId]?.touch()
            if (session == null) {
                Log.w(TAG, "No session found for peer $peerNodeId")
                return null
            }

            sessions[peerNodeId] = session
            decryptBytes(encryptedData, session.sharedSecret)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt packet from peer $peerNodeId", e)
            null
        }
    }

    // ======================== SESSION MANAGEMENT ========================

    /**
     * Check if session exists for peer
     */
    fun hasSession(peerNodeId: Long): Boolean {
        val session = sessions[peerNodeId]
        return session != null && !session.isExpired()
    }

    /**
     * Get session info for peer
     */
    fun getSessionInfo(peerNodeId: Long): SessionInfo? {
        val session = sessions[peerNodeId] ?: return null
        return SessionInfo(
            peerId = session.peerId,
            fingerprint = getPeerPublicKeyFingerprint(session.peerPublicKey),
            createdAt = session.createdAt,
            lastUsed = session.lastUsed,
            verified = session.verified,
            expired = session.isExpired()
        )
    }

    /**
     * Mark session as verified (after manual verification)
     */
    suspend fun markSessionAsVerified(peerNodeId: Long) {
        sessionMutex.withLock {
            sessions[peerNodeId]?.let { session ->
                sessions[peerNodeId] = session.copy(verified = true)
                Log.d(TAG, "Session with peer $peerNodeId marked as verified")
            }
        }
    }

    /**
     * Remove session (on disconnect)
     */
    suspend fun removeSession(peerNodeId: Long) {
        sessionMutex.withLock {
            sessions.remove(peerNodeId)
            Log.d(TAG, "Session removed for peer $peerNodeId")
        }
    }

    /**
     * Clear all sessions
     */
    suspend fun clearAllSessions() {
        sessionMutex.withLock {
            sessions.clear()
            Log.d(TAG, "All sessions cleared")
        }
    }

    /**
     * Get all active sessions
     */
    fun getActiveSessions(): List<Long> {
        return sessions.keys.toList()
    }

    /**
     * Clean up expired sessions
     */
    suspend fun cleanupExpiredSessions() {
        sessionMutex.withLock {
            val expiredPeers = sessions.filter { it.value.isExpired() }.keys
            expiredPeers.forEach { peerId ->
                sessions.remove(peerId)
                Log.d(TAG, "Removed expired session for peer $peerId")
            }
        }
    }

    /**
     * Rotate session key (for forward secrecy)
     */
    suspend fun rotateSessionKey(peerNodeId: Long): Boolean {
        return try {
            sessionMutex.withLock {
                val currentSession = sessions[peerNodeId] ?: return false

                // Derive new shared secret
                val newSharedSecret = deriveSharedSecret(currentSession.peerPublicKey)
                val newSession = currentSession.copy(
                    sharedSecret = newSharedSecret,
                    createdAt = System.currentTimeMillis(),
                    lastUsed = System.currentTimeMillis()
                )
                sessions[peerNodeId] = newSession
                Log.d(TAG, "Rotated session key for peer $peerNodeId")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate session key for peer $peerNodeId", e)
            false
        }
    }

    // ======================== SIGNING & VERIFICATION ========================

    /**
     * Sign data with private key
     */
    fun signData(data: ByteArray): ByteArray {
        val keyPair = getOrCreateIdentityKeyPair()
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(keyPair.private)
        signature.update(data)
        return signature.sign()
    }

    /**
     * Verify signature with peer's public key
     */
    fun verifySignature(data: ByteArray, signatureBytes: ByteArray, peerPublicKey: String): Boolean {
        return try {
            val peerPublicKeyBytes = Base64.decode(peerPublicKey, Base64.NO_WRAP)
            val keyFactory = KeyFactory.getInstance("EC")
            val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(peerPublicKeyBytes))

            val signature = Signature.getInstance("SHA256withECDSA")
            signature.initVerify(publicKey)
            signature.update(data)
            signature.verify(signatureBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Signature verification failed", e)
            false
        }
    }

    // ======================== DATA CLASSES ========================

    @Serializable
    data class KeyExchangeMessage(
        val senderId: Long,
        val publicKey: String,
        val timestamp: Long
    )

    data class SessionInfo(
        val peerId: Long,
        val fingerprint: String,
        val createdAt: Long,
        val lastUsed: Long,
        val verified: Boolean,
        val expired: Boolean
    )

    // ======================== UTILITY FUNCTIONS ========================

    /**
     * Serialize KeyExchangeMessage to JSON
     */
    fun serializeKeyExchange(message: KeyExchangeMessage): String {
        return json.encodeToString(message)
    }

    /**
     * Deserialize KeyExchangeMessage from JSON
     */
    fun deserializeKeyExchange(jsonString: String): KeyExchangeMessage {
        return json.decodeFromString(jsonString)
    }

    /**
     * Get encryption statistics
     */
    fun getStats(): EncryptionStats {
        return EncryptionStats(
            activeSessions = sessions.size,
            verifiedSessions = sessions.count { it.value.verified },
            expiredSessions = sessions.count { it.value.isExpired() }
        )
    }

    data class EncryptionStats(
        val activeSessions: Int,
        val verifiedSessions: Int,
        val expiredSessions: Int
    )
}