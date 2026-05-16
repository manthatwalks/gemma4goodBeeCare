package com.beecareanywhere.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beecareanywhere.data.Settings
import com.beecareanywhere.model.DownloadState
import com.beecareanywhere.model.ModelRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ModelDownloadViewModel(
    private val repository: ModelRepository,
    private val settings: Settings,
) : ViewModel() {

    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state.asStateFlow()

    val url: Flow<String> = settings.modelUrl
    val filename: Flow<String> = settings.modelFilename

    private var downloadJob: Job? = null

    fun startDownload() {
        if (downloadJob?.isActive == true) return
        _state.value = DownloadState.Idle
        downloadJob = viewModelScope.launch {
            val url = settings.modelUrl.first()
            val filename = settings.modelFilename.first()
            val sha = settings.modelSha256.first()
            try {
                repository.download(url = url, filename = filename, expectedSha256 = sha)
                    .collect { _state.value = it }
            } catch (e: CancellationException) {
                _state.value = DownloadState.Idle
                throw e
            } catch (e: Throwable) {
                _state.value = DownloadState.Error(e.message ?: "Download failed")
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _state.value = DownloadState.Idle
    }

    fun reset() {
        _state.value = DownloadState.Idle
    }

    class Factory(
        private val repository: ModelRepository,
        private val settings: Settings,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ModelDownloadViewModel::class.java))
            return ModelDownloadViewModel(repository, settings) as T
        }
    }
}
