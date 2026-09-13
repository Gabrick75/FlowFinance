package com.flowfinance.app.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import android.util.Base64
import java.nio.charset.StandardCharsets

object CryptoUtils {
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 256
    private const val TAG_LENGTH = 128
    private const val IV_LENGTH = 12
    private const val SALT_LENGTH = 16
    private const val ITERATION_COUNT = 65536

    fun encrypt(data: String, password: String): String {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_LENGTH)
        SecureRandom().nextBytes(iv)
        
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH, iv))
        val encryptedData = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))

        // Combine salt + iv + encryptedData
        val combined = ByteArray(salt.size + iv.size + encryptedData.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(encryptedData, 0, combined, salt.size + iv.size, encryptedData.size)

        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    fun decrypt(encryptedDataString: String, password: String): String {
        val decoded = Base64.decode(encryptedDataString, Base64.DEFAULT)

        // Extract salt
        val salt = ByteArray(SALT_LENGTH)
        System.arraycopy(decoded, 0, salt, 0, SALT_LENGTH)

        // Extract IV
        val iv = ByteArray(IV_LENGTH)
        System.arraycopy(decoded, SALT_LENGTH, iv, 0, IV_LENGTH)

        // Extract Encrypted Content
        val encryptedContentLength = decoded.size - SALT_LENGTH - IV_LENGTH
        val encryptedContent = ByteArray(encryptedContentLength)
        System.arraycopy(decoded, SALT_LENGTH + IV_LENGTH, encryptedContent, 0, encryptedContentLength)

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH, iv))

        val decryptedBytes = cipher.doFinal(encryptedContent)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_SIZE)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEYSTORE_KEY_ALIAS = "flowfinance_backup_key"

    /**
     * Transparent, device-bound encryption for data written by background components
     * (e.g. WorkManager workers) that have no user-supplied password available.
     * The key never leaves the Android Keystore and is not exportable.
     */
    fun encryptWithKeystoreKey(data: String): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKeystoreKey())
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return iv + encryptedData
    }

    fun decryptWithKeystoreKey(data: ByteArray): String {
        val iv = data.copyOfRange(0, IV_LENGTH)
        val encryptedData = data.copyOfRange(IV_LENGTH, data.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKeystoreKey(), GCMParameterSpec(TAG_LENGTH, iv))
        return String(cipher.doFinal(encryptedData), StandardCharsets.UTF_8)
    }

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEYSTORE_KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .build()
        )
        return keyGenerator.generateKey()
    }
}
