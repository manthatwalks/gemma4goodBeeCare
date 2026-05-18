package com.beecareanywhere.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beecareanywhere.data.CheckInRepository
import com.beecareanywhere.data.Settings
import com.beecareanywhere.model.BeekeepingKnowledgeBase
import com.beecareanywhere.model.BeekeepingModel
import com.beecareanywhere.model.DiagnosticPromptBuilder
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

enum class DiagnosticStage { Compose, Chat }

sealed class ChatMessage {
    data class User(val text: String, val hasPhoto: Boolean = false) : ChatMessage()
    data class BeeResponse(val text: String) : ChatMessage()
}

class DiagnosticViewModel(
    private val model: BeekeepingModel,
    private val settings: Settings,
    private val checkIns: CheckInRepository,
) : ViewModel() {

    data class UiState(
        // Navigation
        val stage: DiagnosticStage = DiagnosticStage.Compose,

        // Compose-screen inputs
        val description: String = "",
        val question: String = "",
        val showDescribePopup: Boolean = false,
        val showQuestionPopup: Boolean = false,
        val capturedImage: File? = null,
        val capturedAudio: ByteArray? = null,
        val isRecordingAudio: Boolean = false,

        // Chat-screen state
        val messages: List<ChatMessage> = emptyList(),
        val streamingResponse: String = "",   // accumulates current generation
        val chatDraft: String = "",

        // Shared
        val isGenerating: Boolean = false,
        val error: String? = null,
        val language: Settings.Language = Settings.Language.English,

        // Legacy fields — kept so existing SettingsScreen / ModelDownloadScreen compile unchanged
        val text: String = "",
        val response: String = "",
        val needsImageDescription: Boolean = false,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    val modelState: StateFlow<ModelState> = model.state

    private var audioCapture: AudioCapture? = null
    private var generationJob: Job? = null

    init {
        viewModelScope.launch {
            model.load(ModelConfig(modelPath = STUB_MODEL_PATH, systemPrompt = ModelConfig.DEFAULT_SYSTEM_PROMPT))
        }
        viewModelScope.launch {
            settings.language.collect { lang -> _ui.update { it.copy(language = lang) } }
        }
    }

    // ── Compose screen ─────────────────────────────────────────────────────

    fun updateDescription(value: String) = _ui.update { it.copy(description = value) }
    fun updateQuestion(value: String) = _ui.update { it.copy(question = value) }
    fun setShowDescribePopup(show: Boolean) = _ui.update { it.copy(showDescribePopup = show) }
    fun setShowQuestionPopup(show: Boolean) = _ui.update { it.copy(showQuestionPopup = show) }

    fun showError(message: String) = _ui.update { it.copy(error = message) }

    fun onImageCaptured(file: File?) {
        if (file == null) return
        _ui.value.capturedImage?.let(::deleteCaptureFile)
        _ui.update { it.copy(capturedImage = file, error = null) }
    }

    fun clearImage() {
        _ui.value.capturedImage?.let(::deleteCaptureFile)
        _ui.update { it.copy(capturedImage = null) }
    }

    fun startAudioRecording(): Boolean {
        if (audioCapture != null) return false
        val rec = AudioCapture()
        if (!rec.start()) {
            _ui.update { it.copy(error = "Could not start microphone recording") }
            return false
        }
        audioCapture = rec
        _ui.update { it.copy(isRecordingAudio = true, error = null) }
        return true
    }

    fun stopAudioRecording() {
        val rec = audioCapture ?: return
        val bytes = rec.stop()
        audioCapture = null
        _ui.update {
            if (bytes.isEmpty()) {
                it.copy(isRecordingAudio = false, capturedAudio = null, error = "No audio was captured")
            } else {
                it.copy(isRecordingAudio = false, capturedAudio = bytes, error = null)
            }
        }
    }

    fun clearAudio() = _ui.update { it.copy(capturedAudio = null) }

    /** Combine description + question, build the RAG prompt, and transition to Chat. */
    fun submitDiagnosis() {
        val current = _ui.value
        if (current.isGenerating) return
        if (modelState.value !is ModelState.Ready) return

        val userText = listOf(current.description, current.question)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
            .ifBlank { if (current.language == Settings.Language.Swahili) "Tafadhali kagua mzinga." else "Please check the hive." }

        val userMessage = ChatMessage.User(userText, hasPhoto = current.capturedImage != null)

        val knowledge = BeekeepingKnowledgeBase.retrieve(
            query = userText,
            language = current.language,
            hasImage = current.capturedImage != null,
            hasAudio = current.capturedAudio != null,
        )
        val prompt = DiagnosticPromptBuilder.build(
            userText = userText,
            hasImage = current.capturedImage != null,
            hasAudio = current.capturedAudio != null,
            responseLanguage = current.language,
            knowledge = knowledge,
        )

        _ui.update {
            it.copy(
                stage = DiagnosticStage.Chat,
                messages = listOf(userMessage),
                streamingResponse = "",
                isGenerating = true,
                error = null,
            )
        }

        viewModelScope.launch { checkIns.add(userText) }

        generationJob = viewModelScope.launch {
            try {
                model.diagnose(
                    text = prompt,
                    image = current.capturedImage,
                    audio = current.capturedAudio,
                ).collect { token ->
                    _ui.update { it.copy(streamingResponse = it.streamingResponse + token) }
                }
                _ui.update { state ->
                    state.copy(
                        messages = state.messages + ChatMessage.BeeResponse(state.streamingResponse),
                        streamingResponse = "",
                        isGenerating = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _ui.update { it.copy(isGenerating = false, error = e.message ?: "Generation failed") }
            } finally {
                current.capturedImage?.let(::deleteCaptureFile)
                _ui.update { it.copy(capturedImage = null) }
            }
        }
    }

    // ── Chat screen ────────────────────────────────────────────────────────

    fun updateChatDraft(value: String) = _ui.update { it.copy(chatDraft = value) }

    fun sendFollowup() {
        val current = _ui.value
        if (current.chatDraft.isBlank() || current.isGenerating) return
        if (modelState.value !is ModelState.Ready) return

        val followupMsg = ChatMessage.User(current.chatDraft)
        _ui.update {
            it.copy(
                messages = it.messages + followupMsg,
                chatDraft = "",
                streamingResponse = "",
                isGenerating = true,
                error = null,
            )
        }

        val followupPrompt = DiagnosticPromptBuilder.build(
            userText = current.chatDraft,
            hasImage = false,
            hasAudio = false,
            responseLanguage = current.language,
            knowledge = BeekeepingKnowledgeBase.retrieve(
                query = current.chatDraft,
                language = current.language,
                hasImage = false,
                hasAudio = false,
            ),
        )

        generationJob = viewModelScope.launch {
            try {
                model.diagnose(text = followupPrompt, image = null, audio = null)
                    .collect { token -> _ui.update { it.copy(streamingResponse = it.streamingResponse + token) } }
                _ui.update { state ->
                    state.copy(
                        messages = state.messages + ChatMessage.BeeResponse(state.streamingResponse),
                        streamingResponse = "",
                        isGenerating = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _ui.update { it.copy(isGenerating = false, error = e.message ?: "Generation failed") }
            }
        }
    }

    fun goToCompose() {
        cancelGeneration()
        _ui.update {
            it.copy(
                stage = DiagnosticStage.Compose,
                messages = emptyList(),
                streamingResponse = "",
                chatDraft = "",
                error = null,
            )
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

    // ── Legacy submit() kept so any existing call sites still compile ──────

    fun updateText(value: String) = _ui.update { it.copy(text = value) }

    fun submit() {
        _ui.update { it.copy(description = it.text) }
        submitDiagnosis()
    }

    override fun onCleared() {
        audioCapture?.stop()
        audioCapture = null
        _ui.value.capturedImage?.let(::deleteCaptureFile)
        super.onCleared()
    }

    class Factory(
        private val model: BeekeepingModel,
        private val settings: Settings,
        private val checkIns: CheckInRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(DiagnosticViewModel::class.java))
            return DiagnosticViewModel(model, settings, checkIns) as T
        }
    }

    companion object {
        private const val STUB_MODEL_PATH = "<stub>"
    }
}
