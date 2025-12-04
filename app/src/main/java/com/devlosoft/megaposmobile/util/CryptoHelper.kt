package com.devlosoft.megaposmobile.util

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Security levels for encryption iterations
 */
enum class SecurityLevel(val iterations: Int) {
    SIMPLE(1),
    STANDARD(2),
    REMOTE(3),
    MAXIMUM(4)
}

/**
 * Helper class for TripleDES encryption/decryption
 * Compatible with C# CryptoHelper implementation
 */
object CryptoHelper {
    // IV de 8 bytes (bloque de 64 bits para 3DES)
    private val IV = "ATSSECUR".toByteArray(Charsets.US_ASCII)

    // Clave 3DES: debe decodificar a 16 o 24 bytes
    private val KEY = Base64.decode("atsSecuritySystems/26092014/v+01", Base64.DEFAULT)

    /**
     * Encrypts plain text using TripleDES (single pass)
     * @param plainText Text to encrypt
     * @return Base64 encoded encrypted string
     */
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""

        try {
            val cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding")
            val keySpec = SecretKeySpec(KEY, "DESede")
            val ivSpec = IvParameterSpec(IV)

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            return Base64.encodeToString(encrypted, Base64.NO_WRAP)
        } catch (e: Exception) {
            throw RuntimeException("Error encrypting text", e)
        }
    }

    /**
     * Encrypts plain text with multiple iterations
     * @param plainText Text to encrypt
     * @param level Security level (number of iterations)
     * @return Base64 encoded encrypted string
     */
    fun encrypt(plainText: String, level: SecurityLevel): String {
        var result = plainText
        repeat(level.iterations) {
            result = encrypt(result)
        }
        return result
    }

    /**
     * Decrypts Base64 encoded encrypted text (single pass)
     * @param base64Text Base64 encoded encrypted string
     * @return Decrypted plain text
     */
    fun decrypt(base64Text: String): String {
        if (base64Text.isEmpty()) return ""

        try {
            val cipherBytes = Base64.decode(base64Text, Base64.DEFAULT)

            val cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding")
            val keySpec = SecretKeySpec(KEY, "DESede")
            val ivSpec = IvParameterSpec(IV)

            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decrypted = cipher.doFinal(cipherBytes)

            return String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("Error decrypting text", e)
        }
    }

    /**
     * Decrypts text with multiple iterations
     * @param base64Text Base64 encoded encrypted string
     * @param level Security level (number of iterations)
     * @return Decrypted plain text
     */
    fun decrypt(base64Text: String, level: SecurityLevel): String {
        var result = base64Text
        repeat(level.iterations) {
            result = decrypt(result)
        }
        return result
    }
}
