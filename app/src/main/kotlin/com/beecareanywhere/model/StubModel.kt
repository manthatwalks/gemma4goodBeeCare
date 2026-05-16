package com.beecareanywhere.model

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import java.io.File

/**
 * Development-time [BeekeepingModel] that emits a canned response without doing any inference.
 *
 * Lets us build and test the rest of the app (UI, capture pipeline, download flow) before the real
 * LiteRT-LM model integration lands in Phase 2.
 */
class StubModel : BeekeepingModel {

    private val _state = MutableStateFlow<ModelState>(ModelState.Idle)
    override val state: StateFlow<ModelState> = _state.asStateFlow()

    override suspend fun load(config: ModelConfig) {
        _state.value = ModelState.Loading
        delay(SIMULATED_LOAD_MS)
        _state.value = ModelState.Ready
    }

    override fun diagnose(text: String, image: File?, audio: ByteArray?): Flow<String> = flow {
        check(_state.value is ModelState.Ready) { "Model not loaded — call load() first" }
        val response = buildResponse(text, hasImage = image != null, hasAudio = audio != null)
        for (token in response.split(' ')) {
            delay(SIMULATED_TOKEN_DELAY_MS)
            emit("$token ")
        }
    }

    override suspend fun unload() {
        _state.value = ModelState.Idle
    }

    private fun buildResponse(text: String, hasImage: Boolean, hasAudio: Boolean): String {
        val modalities = buildString {
            append("text")
            if (hasImage) append(" + image")
            if (hasAudio) append(" + audio")
        }
        return "[STUB MODEL — not a real diagnosis] " +
            "Received your $modalities query: \"$text\". " +
            "Once Phase 2 wires LiteRT-LM, the real Apiary fine-tune will respond here with " +
            "region-specific, source-grounded guidance. The rest of the app stays unchanged."
    }

    companion object {
        private const val SIMULATED_LOAD_MS = 800L
        private const val SIMULATED_TOKEN_DELAY_MS = 80L
    }
}
