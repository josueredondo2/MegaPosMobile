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

/**
 * Character substitution encryption/decryption
 * Maps characters to their "encoded" equivalents (codes 161-254 in Latin-1/Unicode)
 * Compatible with C# TablaDesencriptado implementation
 */
object TablaDesencriptado {
    // Map of "encrypted" characters (codes 161-254 in Latin-1/Unicode U+00A1..U+00FE)
    // to their original character
    private val decryptionMap = mapOf(
        '\u00A1' to "a", // 161
        '\u00A2' to "b",
        '\u00A3' to "c",
        '\u00A4' to "d",
        '\u00A5' to "e",
        '\u00A6' to "f",
        '\u00A7' to "g",
        '\u00A8' to "h",
        '\u00A9' to "i",
        '\u00AA' to "j",
        '\u00AB' to "k",
        '\u00AC' to "l",
        '\u00AD' to "m",
        '\u00AE' to "n",
        '\u00AF' to "ñ",
        '\u00B0' to "o",
        '\u00B1' to "p",
        '\u00B2' to "q",
        '\u00B3' to "r",
        '\u00B4' to "s",
        '\u00B5' to "t",
        '\u00B6' to "u",
        '\u00B7' to "v",
        '\u00B8' to "w",
        '\u00B9' to "x",
        '\u00BA' to "y",
        '\u00BB' to "z",
        '\u00BC' to "0",
        '\u00BD' to "1",
        '\u00BE' to "2",
        '\u00BF' to "3",
        '\u00C0' to "4",
        '\u00C1' to "5",
        '\u00C2' to "6",
        '\u00C3' to "7",
        '\u00C4' to "8",
        '\u00C5' to "9",
        '\u00C6' to "A",
        '\u00C7' to "B",
        '\u00C8' to "C",
        '\u00C9' to "D",
        '\u00CA' to "E",
        '\u00CB' to "F",
        '\u00CC' to "G",
        '\u00CD' to "H",
        '\u00CE' to "I",
        '\u00CF' to "J",
        '\u00D0' to "K",
        '\u00D1' to "L",
        '\u00D2' to "M",
        '\u00D3' to "N",
        '\u00D4' to "Ñ",
        '\u00D5' to "O",
        '\u00D6' to "P",
        '\u00D7' to "Q",
        '\u00D8' to "R",
        '\u00D9' to "S",
        '\u00DA' to "T",
        '\u00DB' to "U",
        '\u00DC' to "V",
        '\u00DD' to "W",
        '\u00DE' to "X",
        '\u00DF' to "Y",
        '\u00E0' to "Z",
        '\u00E1' to "á",
        '\u00E2' to "é",
        '\u00E3' to "í",
        '\u00E4' to "ó",
        '\u00E5' to "ú",
        '\u00E6' to "À",
        '\u00E7' to "È",
        '\u00E8' to "Ì",
        '\u00E9' to "Ò",
        '\u00EA' to "Ù",
        '\u00EB' to "à",
        '\u00EC' to "è",
        '\u00ED' to "ì",
        '\u00EE' to "ò",
        '\u00EF' to "ù",
        '\u00F0' to "$",
        '\u00F1' to "%",
        '\u00F2' to "&",
        '\u00F3' to "+",
        '\u00F4' to "-",
        '\u00F5' to "/",
        '\u00F6' to "*",
        '\u00F7' to "(",
        '\u00F8' to ")",
        '\u00F9' to "=",
        '\u00FA' to "?",
        '\u00FB' to "¿",
        '\u00FC' to "#",
        '\u00FD' to "[",
        '\u00FE' to "]"
    )

    // Reverse map for encryption (original -> "encrypted")
    private val encryptionMap: Map<Char, Char> by lazy {
        val map = mutableMapOf<Char, Char>()
        decryptionMap.forEach { (encoded, original) ->
            if (original.isNotEmpty()) {
                val originalChar = original[0]
                if (!map.containsKey(originalChar)) {
                    map[originalChar] = encoded
                }
            }
        }
        map
    }

    /**
     * Encrypts text by substituting each original character with its encoded equivalent
     * @param text Text to encrypt
     * @return Encrypted text with character substitution
     */
    fun encrypt(text: String): String {
        if (text.isEmpty()) return ""

        return buildString(text.length) {
            text.forEach { char ->
                append(encryptionMap[char] ?: char)
            }
        }
    }

    /**
     * Decrypts text by substituting each encoded character with its original equivalent
     * @param text Encrypted text
     * @return Decrypted text
     */
    fun decrypt(text: String): String {
        if (text.isEmpty()) return ""

        return buildString(text.length) {
            text.forEach { char ->
                append(decryptionMap[char] ?: char.toString())
            }
        }
    }
}
