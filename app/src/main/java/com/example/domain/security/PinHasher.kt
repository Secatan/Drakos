package com.example.domain.security

import java.security.MessageDigest

object PinHasher {
    private const val SALT_PREFIX = "kiosco_pos_arg_v1"

    fun hash(username: String, rawPin: String): String {
        val payload = "$SALT_PREFIX:${username.trim().lowercase()}:${rawPin.trim()}"
        val bytes = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isHashed(storedValue: String): Boolean {
        return storedValue.length == 64 && storedValue.all { it in '0'..'9' || it in 'a'..'f' }
    }

    fun verify(username: String, rawPin: String, storedValue: String): Boolean {
        val candidate = if (isHashed(storedValue)) {
            hash(username, rawPin)
        } else {
            rawPin.trim()
        }
        return MessageDigest.isEqual(
            candidate.toByteArray(Charsets.UTF_8),
            storedValue.toByteArray(Charsets.UTF_8)
        )
    }
}
