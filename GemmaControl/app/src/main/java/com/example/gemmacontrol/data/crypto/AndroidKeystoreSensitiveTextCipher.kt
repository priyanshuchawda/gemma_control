package com.example.gemmacontrol.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidKeystoreSensitiveTextCipher : SensitiveTextCipher {

    private val provider = "AndroidKeyStore"
    private val keyAlias = "gemma_control_message_key"
    private val transformation = "AES/GCM/NoPadding"

    init {
        try {
            initKeyStore()
        } catch (e: Exception) {
            throw KeyUnavailableFailure(e)
        }
    }

    private fun initKeyStore() {
        val keyStore = KeyStore.getInstance(provider)
        keyStore.load(null)
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, provider)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            )
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(provider)
            keyStore.load(null)
            val key = keyStore.getKey(keyAlias, null) as? SecretKey
            key ?: throw KeyUnavailableFailure(Exception("Key alias not found: $keyAlias"))
        } catch (e: Exception) {
            throw KeyUnavailableFailure(e)
        }
    }

    override fun encrypt(plaintext: String, associatedData: ByteArray?): EncryptedPayload {
        try {
            val secretKey = getSecretKey()
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            if (associatedData != null) {
                cipher.updateAAD(associatedData)
            }
            val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
            return EncryptedPayload(ciphertext, cipher.iv)
        } catch (e: SecureStorageFailure) {
            throw e
        } catch (e: Exception) {
            throw EncryptionFailure(e)
        }
    }

    override fun decrypt(payload: EncryptedPayload, associatedData: ByteArray?): String {
        try {
            val secretKey = getSecretKey()
            val cipher = Cipher.getInstance(transformation)
            val spec = GCMParameterSpec(128, payload.iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            if (associatedData != null) {
                cipher.updateAAD(associatedData)
            }
            val decryptedBytes = cipher.doFinal(payload.ciphertext)
            return String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: SecureStorageFailure) {
            throw e
        } catch (e: Exception) {
            throw EncryptionFailure(e)
        }
    }
}

