package com.beecareanywhere.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beecareanywhere.model.BeekeepingModel
import com.beecareanywhere.model.ModelConfig
import com.beecareanywhere.model.ModelState
import com.beecareanywhere.multimodal.AudioCapture
import com.beecareanywhere.multimodal.deleteCaptureFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class DiagnosticViewModel(
    private val model: BeekeepingModel,
) : ViewModel() {

    data class UiState(
        val text: String = "",
        val capturedImage: File? = null,
        val capturedAudio: ByteArray? = null,
        val isRecordingAudio: Boolean = false,
        val isGenerating: Boolean = false,
        val response: String = "",
        val error: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    val modelState: StateFlow<ModelState> = model.state

    private var audioCapture: AudioCapture? = null
    private var generationJob: Job? = null

    init {
        viewModelScope.launch {
            model.load(
                ModelConfig(
                    modelPath = STUB_MODEL_PATH,
                    systemPrompt = ModelConfig.DEFAULT_SYSTEM_PROMPT,
                ),
            )
        }
    }

    fun updateText(value: String) {
        _ui.update { it.copy(text = value) }
    }

    fun onImageCaptured(file: File?) {
        if (file == null) return
        _ui.value.capturedImage?.let(::deleteCaptureFile)
        _ui.update { it.copy(capturedImage = file) }
    }

    fun clearImage() {
        _ui.value.capturedImage?.let(::deleteCaptureFile)
        _ui.update { it.copy(capturedImage = null) }
    }

    /** Returns true if recording started. */
    fun startAudioRecording(): Boolean {
        if (audioCapture != null) return false
        val rec = AudioCapture()
        if (!rec.start()) return false
        audioCapture = rec
        _ui.update { it.copy(isRecordingAudio = true) }
        return true
    }

    fun stopAudioRecording() {
        val rec = audioCapture ?: return
        val bytes = rec.stop()
        audioCapture = null
        _ui.update { it.copy(isRecordingAudio = false, capturedAudio = bytes) }
    }

    fun clearAudio() {
        _ui.update { it.copy(capturedAudio = null) }
    }

    fun submit() {
        val current = _ui.value
        if (current.isGenerating) return
        if (current.text.isBlank() && current.capturedImage == null && current.capturedAudio == null) return
        if (modelState.value !is ModelState.Ready) return

        _ui.update { it.copy(isGenerating = true, response = "", error = null) }
        generationJob = viewModelScope.launch {
            try {
                model.diagnose(
                    text = current.text,
                    image = current.capturedImage,
                    audio = current.capturedAudio,
                ).collect { token ->
                    _ui.update { it.copy(response = it.response + token) }
                }
                _ui.update { it.copy(isGenerating = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _ui.update {
                    it.copy(
                        isGenerating = false,
                        error = e.message ?: "Generation failed",
                    )
                }
            } finally {
                current.capturedImage?.let(::deleteCaptureFile)
                _ui.update { it.copy(capturedImage = null) }
            }
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        generationJob = null
        _ui.update { it.copy(isGenerating = false) }
    }

    fun resetConversation() {
        cancelGeneration()
        _ui.value.capturedImage?.let(::deleteCaptureFile)
        _ui.value = UiState()
    }

    override fun onCleared() {
        audioCapture?.stop()
        audioCapture = null
        _ui.value.capturedImage?.let(::deleteCaptureFile)
        super.onCleared()
    }

    class Factory(private val model: BeekeepingModel) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(DiagnosticViewModel::class.java))
            return DiagnosticViewModel(model) as T
        }
    }

    companion object {
        private const val STUB_MODEL_PATH = "<stub>"
    }
}
