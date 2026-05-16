package com.beecareanywhere.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beecareanywhere.data.Settings
import com.beecareanywhere.model.ModelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(
    private val settings: Settings,
    private val repository: ModelRepository,
) : ViewModel() {

    data class ModelInfo(
        val filename: String,
        val installed: Boolean,
        val sizeBytes: Long,
        val sha256: String?,
        val path: String?,
    )

    val language: Flow<Settings.Language> = settings.language
    val modelUrl: Flow<String> = settings.modelUrl

    private val _modelInfo = MutableStateFlow<ModelInfo?>(null)
    val modelInfo: StateFlow<ModelInfo?> = _modelInfo.asStateFlow()

    init {
        viewModelScope.launch {
            combine(settings.modelFilename, settings.modelSha256) { name, sha -> name to sha }
                .collect { (name, sha) -> _modelInfo.value = inspect(name, sha) }
        }
    }

    fun setLanguage(language: Settings.Language) {
        viewModelScope.launch { settings.setLanguage(language) }
    }

    fun deleteModel() {
        viewModelScope.launch {
            val filename = settings.modelFilename.first()
            repository.delete(filename)
            _modelInfo.value = inspect(filename, settings.modelSha256.first())
        }
    }

    private fun inspect(filename: String, sha: String?): ModelInfo {
        val file: File? = repository.installedModel(filename)
        return ModelInfo(
            filename = filename,
            installed = file != null,
            sizeBytes = file?.length() ?: 0L,
            sha256 = sha,
            path = file?.absolutePath,
        )
    }

    class Factory(
        private val settings: Settings,
        private val repository: ModelRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
            return SettingsViewModel(settings, repository) as T
        }
    }
}
