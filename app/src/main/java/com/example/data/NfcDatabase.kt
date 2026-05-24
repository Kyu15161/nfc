package com.example.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Database(entities = [NfcCard::class], version = 2, exportSchema = false)
abstract class NfcDatabase : RoomDatabase() {
    abstract fun nfcCardDao(): NfcCardDao

    companion object {
        @Volatile
        private var INSTANCE: NfcDatabase? = null
        private const val KEY_ALIAS = "NfcDbEncryptionKey"
        private const val PREF_NAME = "nfc_secure_prefs"
        private const val PREF_KEY_IV = "db_key_iv"
        private const val PREF_KEY_ENCRYPTED = "db_key_encrypted"

        private fun getDatabasePassphrase(context: Context): ByteArray {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

            // Ensure our SecretKey exists in the Keystore
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                keyGen.init(
                    KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                )
                keyGen.generateKey()
            }
            val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey

            val encryptedKeyBase64 = prefs.getString(PREF_KEY_ENCRYPTED, null)
            val ivBase64 = prefs.getString(PREF_KEY_IV, null)

            if (encryptedKeyBase64 != null && ivBase64 != null) {
                // Decrypt existing passphrase
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, Base64.decode(ivBase64, Base64.DEFAULT)))
                return cipher.doFinal(Base64.decode(encryptedKeyBase64, Base64.DEFAULT))
            } else {
                // Generate new passphrase
                val newPassphrase = ByteArray(32).apply { SecureRandom().nextBytes(this) }
                
                // Encrypt it and save to SharedPreferences
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val encryptedPassphrase = cipher.doFinal(newPassphrase)
                val iv = cipher.iv

                prefs.edit()
                    .putString(PREF_KEY_ENCRYPTED, Base64.encodeToString(encryptedPassphrase, Base64.DEFAULT))
                    .putString(PREF_KEY_IV, Base64.encodeToString(iv, Base64.DEFAULT))
                    .apply()

                return newPassphrase
            }
        }

        fun getDatabase(context: Context): NfcDatabase {
            return INSTANCE ?: synchronized(this) {
                // Initialize SQLCipher library
                System.loadLibrary("sqlcipher")
                val factory = SupportFactory(getDatabasePassphrase(context))
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NfcDatabase::class.java,
                    "nfc_database"
                )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
