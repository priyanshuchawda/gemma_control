package com.example.gemmacontrol.data.crypto

interface MessageBodyCipher {
    fun encrypt(plaintext: String): EncryptedPayload
    fun decrypt(payload: EncryptedPayload): String
}
