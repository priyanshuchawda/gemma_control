package com.example.gemmacontrol.data.crypto

sealed class SecureStorageFailure(message: String, cause: Throwable? = null) :
    Exception(message, cause)

class KeyUnavailableFailure(cause: Throwable? = null) :
    SecureStorageFailure("Secure local storage key is unavailable.", cause)

class EncryptionFailure(cause: Throwable? = null) :
    SecureStorageFailure("Secure local storage encryption failed.", cause)

class TokenGenerationFailure(cause: Throwable? = null) :
    SecureStorageFailure("Secure local storage token generation failed.", cause)

interface SensitiveTextCipher {
    @Throws(SecureStorageFailure::class)
    fun encrypt(plaintext: String, associatedData: ByteArray? = null): EncryptedPayload
    
    @Throws(SecureStorageFailure::class)
    fun decrypt(payload: EncryptedPayload, associatedData: ByteArray? = null): String
}

