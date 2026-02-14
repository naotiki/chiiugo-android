package me.naotiki.chiiugo.data.llm

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureApiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val FILE_NAME = "llm_secure_store"
        private const val KEY_API_KEY = "api_key"
    }

    private val preferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveApiKey(rawApiKey: String) {
        preferences.edit {
            putString(KEY_API_KEY, rawApiKey.trim().ifBlank { null })
        }
    }

    fun hasApiKey(): Boolean {
        return !preferences.getString(KEY_API_KEY, null).isNullOrBlank()
    }

    fun readApiKey(): String? {
        return preferences.getString(KEY_API_KEY, null)?.trim()?.takeIf { it.isNotBlank() }
    }
}
