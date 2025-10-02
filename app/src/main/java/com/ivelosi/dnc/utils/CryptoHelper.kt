package com.ivelosi.dnc.utils

import android.util.Base64
import java.security.*
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoHelper {

    companion object {
        private const val AES_KEY_SIZE = 256
        private const val GCM_IV_SIZE = 12
        private const val GCM_TAG_SIZE = 128

        // -------------------
        // Key Pair Generation (ECC)
        // -------------------
        fun generateECCKeyPair(): KeyPair {
            val keyGen = KeyPairGenerator.getInstance("EC")
            keyGen.initialize(256)
            return keyGen.generateKeyPair()
        }

        // -------------------
        // AES Key Generation
        // -------------------
        fun generateAESKey(): SecretKey {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(AES_KEY_SIZE)
            return keyGen.generateKey()
        }

        // -------------------
        // AES Encryption
        // -------------------
        fun encryptAES(data: ByteArray, secretKey: SecretKey): ByteArray {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(data)
            return iv + encrypted // prepend IV for decryption
        }

        // -------------------
        // AES Decryption
        // -------------------
        fun decryptAES(encryptedData: ByteArray, secretKey: SecretKey): ByteArray {
            val iv = encryptedData.sliceArray(0 until GCM_IV_SIZE)
            val actualData = encryptedData.sliceArray(GCM_IV_SIZE until encryptedData.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_SIZE, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            return cipher.doFinal(actualData)
        }

        // -------------------
        // ECC Encryption (encrypt AES key with recipient's public key)
        // -------------------
        fun encryptAESKey(secretKey: SecretKey, recipientPublicKey: PublicKey): ByteArray {
            val cipher = Cipher.getInstance("ECIES")
            cipher.init(Cipher.ENCRYPT_MODE, recipientPublicKey)
            return cipher.doFinal(secretKey.encoded)
        }

        // -------------------
        // ECC Decryption (decrypt AES key with your private key)
        // -------------------
        fun decryptAESKey(encryptedKey: ByteArray, privateKey: PrivateKey): SecretKey {
            val cipher = Cipher.getInstance("ECIES")
            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            val decodedKey = cipher.doFinal(encryptedKey)
            return SecretKeySpec(decodedKey, 0, decodedKey.size, "AES")
        }

        // -------------------
        // Helper: Base64 Encoding/Decoding (Optional, for sending keys/data as strings)
        // -------------------
        fun toBase64(data: ByteArray): String = Base64.encodeToString(data, Base64.NO_WRAP)
        fun fromBase64(data: String): ByteArray = Base64.decode(data, Base64.NO_WRAP)
    }
}
