package com.example.gemmacontrol.data.crypto

interface DedupeTokenGenerator {
    fun generate(canonicalIdentityMaterial: String): String
}
