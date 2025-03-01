package com.application.audio_recorder_application.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class EncryptionService(private val context: Context) {

    private val keyAlias = "audio_recorder_key"
    private val transformation = "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"

    private fun getMasterKey(): MasterKey {
        return MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    fun encryptFile(inputFile: File, outputFile: File): Boolean {
        return try {
            val masterKey = getMasterKey()

            val encryptedFile = EncryptedFile.Builder(
                context,
                outputFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            inputFile.inputStream().use { inputStream ->
                encryptedFile.openFileOutput().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            Log.d("EncryptionService", "File encrypted successfully: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("EncryptionService", "Error encrypting file: ${e.message}")
            false
        }
    }

    fun decryptFile(encryptedFile: File, outputFile: File): Boolean {
        return try {
            val masterKey = getMasterKey()

            val encryptedFileObj = EncryptedFile.Builder(
                context,
                encryptedFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            encryptedFileObj.openFileInput().use { inputStream ->
                outputFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            Log.d("EncryptionService", "File decrypted successfully: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("EncryptionService", "Error decrypting file: ${e.message}")
            false
        }
    }

    fun isFileEncrypted(file: File): Boolean {
        // Простая проверка - пытаемся открыть файл как зашифрованный
        return try {
            val masterKey = getMasterKey()
            val encryptedFile = EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            
            encryptedFile.openFileInput().close()
            true
        } catch (e: Exception) {
            false
        }
    }
} 