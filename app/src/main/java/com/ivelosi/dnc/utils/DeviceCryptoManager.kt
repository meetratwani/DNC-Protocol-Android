package com.ivelosi.dnc.utils

import android.content.Context
import android.util.Base64
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object DeviceCryptoManager {

    private const val PREFS_NAME = "device_crypto_prefs"
    private const val PRIVATE_KEY_PREF = "private_key"
    private const val PUBLIC_KEY_PREF = "public_key"
    private const val AES_KEY_SIZE = 256
    private const val GCM_IV_SIZE = 12
    private const val GCM_TAG_SIZE = 128

    private lateinit var privateKey: PrivateKey
    private lateinit var publicKey: PublicKey

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val privateBase64 = prefs.getString(PRIVATE_KEY_PREF, null)
        val publicBase64 = prefs.getString(PUBLIC_KEY_PREF, null)

        if (privateBase64 != null && publicBase64 != null) {
            // Load existing keys
            privateKey = KeyFactory.getInstance("EC")
                .generatePrivate(PKCS8EncodedKeySpec(Base64.decode(privateBase64, Base64.NO_WRAP)))
            publicKey = KeyFactory.getInstance("EC")
                .generatePublic(X509EncodedKeySpec(Base64.decode(publicBase64, Base64.NO_WRAP)))
        } else {
            // Generate new keys
            val keyPair = generateECCKeyPair()
            privateKey = keyPair.private
            publicKey = keyPair.public

            // Save keys to SharedPreferences
            prefs.edit().apply {
                putString(PRIVATE_KEY_PREF, Base64.encodeToString(privateKey.encoded, Base64.NO_WRAP))
                putString(PUBLIC_KEY_PREF, Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP))
                apply()
            }
        }
    }

    // -------------------
    // Key Pair Generation
    // -------------------
    private fun generateECCKeyPair(): KeyPair {
        val keyGen = KeyPairGenerator.getInstance("EC")
        keyGen.initialize(256)
        return keyGen.generateKeyPair()
    }

    fun getPrivateKey(): PrivateKey = privateKey
    fun getPublicKey(): PublicKey = publicKey

    // -------------------
    // AES Encryption/Decryption
    // -------------------
    fun encryptAES(data: ByteArray, secretKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        return iv + encrypted
    }

    fun decryptAES(encryptedData: ByteArray, secretKey: SecretKey): ByteArray {
        val iv = encryptedData.sliceArray(0 until GCM_IV_SIZE)
        val actualData = encryptedData.sliceArray(GCM_IV_SIZE until encryptedData.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(actualData)
    }

    // -------------------
    // AES Key Encryption/Decryption using ECC
    // -------------------
    fun encryptAESKey(secretKey: SecretKey, recipientPublicKey: PublicKey): ByteArray {
        val cipher = Cipher.getInstance("ECIES")
        cipher.init(Cipher.ENCRYPT_MODE, recipientPublicKey)
        return cipher.doFinal(secretKey.encoded)
    }

    fun decryptAESKey(encryptedKey: ByteArray): SecretKey {
        val cipher = Cipher.getInstance("ECIES")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        val decodedKey = cipher.doFinal(encryptedKey)
        return SecretKeySpec(decodedKey, 0, decodedKey.size, "AES")
    }

    // -------------------
    // Helper: AES Key Generation
    // -------------------
    fun generateAESKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(AES_KEY_SIZE)
        return keyGen.generateKey()
    }

    // -------------------
    // Base64 helpers (optional)
    // -------------------
    fun toBase64(data: ByteArray): String = Base64.encodeToString(data, Base64.NO_WRAP)
    fun fromBase64(data: String): ByteArray = Base64.decode(data, Base64.NO_WRAP)
}
