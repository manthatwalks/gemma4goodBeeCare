package com.beecareanywhere.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class Settings(private val context: Context) {

    enum class Language { English, Swahili, Amharic }

    val language: Flow<Language> = context.dataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE]
            ?.let { stored -> runCatching { Language.valueOf(stored) }.getOrNull() }
            ?: Language.English
    }

    suspend fun setLanguage(language: Language) {
        context.dataStore.edit { it[KEY_LANGUAGE] = language.name }
    }

    val modelFilename: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MODEL_FILENAME] ?: DEFAULT_MODEL_FILENAME
    }

    suspend fun setModelFilename(filename: String) {
        context.dataStore.edit { it[KEY_MODEL_FILENAME] = filename }
    }

    val modelUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MODEL_URL] ?: DEFAULT_MODEL_URL
    }

    suspend fun setModelUrl(url: String) {
        context.dataStore.edit { it[KEY_MODEL_URL] = url }
    }

    /** Optional pinned SHA-256; null means "skip verification". */
    val modelSha256: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_MODEL_SHA256]?.takeIf { it.isNotBlank() }
    }

    suspend fun setModelSha256(sha: String?) {
        context.dataStore.edit { prefs ->
            if (sha.isNullOrBlank()) prefs.remove(KEY_MODEL_SHA256) else prefs[KEY_MODEL_SHA256] = sha
        }
    }

    companion object {
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_MODEL_FILENAME = stringPreferencesKey("model_filename")
        private val KEY_MODEL_URL = stringPreferencesKey("model_url")
        private val KEY_MODEL_SHA256 = stringPreferencesKey("model_sha256")

        // Placeholders — final values land in Phase 4 once Apiary is uploaded to HF Hub.
        // The URL follows the E2B naming convention; verify the live model card before relying
        // on this URL (see PLAN.md Sources note).
        const val DEFAULT_MODEL_FILENAME = "gemma-4-E2B-it-litert-lm.litertlm"
        const val DEFAULT_MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/$DEFAULT_MODEL_FILENAME"
    }
}
