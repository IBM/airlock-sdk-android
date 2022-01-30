package com.ibm.airlock.sdk.util;
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
/**
 * Utility class for encrypting a String to byte array with AES algorithm
 */
class AesGcmEncryptionUtil private constructor() {
    private val secureRandom: SecureRandom = SecureRandom()
    fun encrypt(rawEncryptionKey: ByteArray, rawData: ByteArray, addIVLength: Boolean): ByteArray {
        require(rawEncryptionKey.size >= 16) { "key length must be longer than 16 bytes" }
        val iv = ByteArray(IV_LENGTH_BYTE)
        secureRandom.nextBytes(iv)
        val cipher = cipherWrapper.get()
        cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(rawEncryptionKey, "AES"),
                GCMParameterSpec(TAG_LENGTH_BIT, iv))
        val encrypted: ByteArray = cipher.doFinal(rawData)
        val byteBuffer: ByteBuffer
        if (addIVLength) {
            byteBuffer = ByteBuffer.allocate(1 + iv.size + encrypted.size)
            byteBuffer.put(iv.size.toByte())
        } else {
            byteBuffer = ByteBuffer.allocate(iv.size + encrypted.size)
        }
        byteBuffer.put(iv)
        byteBuffer.put(encrypted)
        return byteBuffer.array()
    }
    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        const val IV_LENGTH_BYTE = 12
        private const val TAG_LENGTH_BIT = 128
        @JvmField
        val INSTANCE = AesGcmEncryptionUtil()
        private val cipherWrapper = object : ThreadLocal<Cipher>() {
            override fun initialValue(): Cipher {
                return Cipher.getInstance(ALGORITHM)
            }
            // !! because the ThreadLocal has an initializer that can't be null unless someone sets it to null, in which case we want an NPE
            override fun get(): Cipher {
                return super.get()!!
            }
        }
    }
}