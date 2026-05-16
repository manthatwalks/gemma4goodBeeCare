package com.beecareanywhere.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * The model-agnostic contract for on-device beekeeping diagnostics.
 *
 * Phase 2 will add a `LiteRtLmModel` implementation backed by the LiteRT-LM Android library.
 * Until then, [StubModel] provides a development-time fake that streams a canned response so the
 * rest of the app can be built and tested end-to-end.
 *
 * Swap point: replace the implementation returned by `ServiceLocator.provideModel()` to switch
 * from the stub to the real model. No other code in the app touches a concrete model class.
 */
interface BeekeepingModel {
    val state: StateFlow<ModelState>

    /** Load weights and prepare the engine. Updates [state] to Loading -> Ready (or Error). */
    suspend fun load(config: ModelConfig)

    /**
     * Stream tokens for a diagnostic query. Caller must ensure [state] is [ModelState.Ready] first.
     *
     * @param image Optional photo of the hive frame, persisted as a cache File (LiteRT-LM consumes
     *     image content via `Content.ImageFile`).
     * @param audio Optional 16 kHz mono PCM bytes (LiteRT-LM consumes audio via
     *     `Content.AudioBytes`).
     */
    fun diagnose(
        text: String,
        image: File? = null,
        audio: ByteArray? = null,
    ): Flow<String>

    suspend fun unload()
}
