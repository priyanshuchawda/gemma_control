package com.example.gemmacontrol.data.crypto

interface DedupeTokenGenerator {
    @Throws(SecureStorageFailure::class)
    fun generate(canonicalIdentityMaterial: String): String
}

