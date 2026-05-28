package com.example.gemmacontrol.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

class AndroidKeystoreHmacDedupeTokenGenerator : DedupeTokenGenerator {

    private val provider = "AndroidKeyStore"
    private val keyAlias = "gemma_control_hmac_key"
    private val algorithm = "HmacSHA256"

    init {
        try {
            initHmacKey()
        } catch (e: Exception) {
            throw KeyUnavailableFailure(e)
        }
    }

    private fun initHmacKey() {
        val keyStore = KeyStore.getInstance(provider)
        keyStore.load(null)
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, provider)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_SIGN
                )
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
            key ?: throw KeyUnavailableFailure(Exception("HMAC key alias not found: $keyAlias"))
        } catch (e: Exception) {
            throw KeyUnavailableFailure(e)
        }
    }

    override fun generate(canonicalIdentityMaterial: String): String {
        try {
            val secretKey = getSecretKey()
            val mac = Mac.getInstance(algorithm)
            mac.init(secretKey)
            val hmacBytes = mac.doFinal(canonicalIdentityMaterial.toByteArray(StandardCharsets.UTF_8))
            return hmacBytes.joinToString("") { "%02x".format(it) }
        } catch (e: SecureStorageFailure) {
            throw e
        } catch (e: Exception) {
            throw TokenGenerationFailure(e)
        }
    }
}

