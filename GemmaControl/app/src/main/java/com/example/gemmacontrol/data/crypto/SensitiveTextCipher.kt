package com.example.gemmacontrol.data.crypto

interface SensitiveTextCipher {
    fun encrypt(plaintext: String, associatedData: ByteArray? = null): EncryptedPayload
    fun decrypt(payload: EncryptedPayload, associatedData: ByteArray? = null): String
}
