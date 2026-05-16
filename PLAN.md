# BeeCare Anywhere — Android App Plan

## What we're building

BeeCare Anywhere is the Android app that delivers Gemma Apiary to beekeepers. It runs the fine-tuned model fully on-device via Google's LiteRT runtime, accepts text + image + audio input, and returns regional diagnostic advice in the beekeeper's local language. After a one-time model download (~2 GB), the app works completely offline on a commodity Android phone.

This is the deployment vehicle for the Gemma Apiary fine-tune. The original hackathon brief said "no Android app" because shipping a polished Android app in three days while also fine-tuning is genuinely hard. We're choosing to do it anyway because:

1. A live phone demo in the submission video is meaningfully stronger than a laptop Streamlit demo — judges who score "impact" want to see the actual delivery mechanism, not a simulation of it.
2. The fine-tune work and the app work are mostly parallel — they don't compete for the same hours, since the app shell can be developed against the prebuilt `litert-community/gemma-4-E2B-it-litert-lm` while the fine-tune trains.
3. Building a model-agnostic backbone now means the app is ready to receive Apiary the moment the fine-tune finishes — zero coupling between the two tracks.

We're not trying to ship to Google Play. We're shipping a sideloadable APK from GitHub Releases that works on a $80–100 Android phone, demonstrated end-to-end in the submission video.

## Why this is not a thin wrapper

The pitch from the original brief — "we change what the model knows by fine-tuning, not what it can retrieve at runtime" — applies just as strongly on the deployment side. There's a temptation to build a chat UI around a hosted API and call it a mobile app. That's not what we're doing.

The technical claim is that a ~2 GB LiteRT model file, fine-tuned on regional apiculture data, runs locally on commodity Android hardware and produces useful diagnostics with no network connection. This is the part that's hard, that other hackathon submissions won't have, and that judges can verify by airplane-moding the demo phone before asking it a Swahili Varroa question.

## Technical approach

### Runtime

Google's LiteRT (formerly TensorFlow Lite), specifically the **LiteRT-LM Android library** — Google's LLM-specific extension to LiteRT that provides tokenization, KV cache management, multimodal preprocessing, and a Kotlin-native streaming API. The model file format is `.litertlm`, which bundles the quantized weights with the tokenizer and the required encoder components.

The LiteRT-LM Android API surface is stable enough to write real code against on the first pass:
- `EngineConfig` for model path, system prompt, generation parameters
- `Engine` for session lifecycle (load, generate, unload)
- Streaming `Flow<String>` for token-by-token output
- `Content.ImageFile(File)` and `Content.AudioBytes(ByteArray)` for multimodal inputs

We are **not** using MediaPipe LLM Inference (even though it sits on top of the same runtime) because we want the deployment story to read "LiteRT" without a higher-level wrapper, and LiteRT-LM gives us direct control over generation parameters and session lifecycle. We are **not** using llama.cpp / GGUF because the entire production target in the hackathon writeup is LiteRT — mixing runtime stacks dilutes the narrative.

### Model

Gemma 4 E2B, quantized INT4, distributed as `litert-community/gemma-4-E2B-it-litert-lm`. Approximate file size: **~2 GB** (estimate; verify against the current model card before locking the download URL). We ship one variant only — no E4B upgrade path in this build. The trade-off goes the other way from E4B: smaller weights mean broader hardware coverage and faster decode at the cost of some quality, especially on multimodal cross-attention. Going with E2B opens up phones in the $50–80 range that E4B would have ruled out; we keep $80–100 as the documented target since that's what the rest of the plan was designed around.

**Performance expectations:**
- Published LiteRT-LM benchmarks (Gemma 4 **E4B** — used as reference): Galaxy S26 Ultra ~17–18 tok/s CPU decode, ~22 tok/s GPU; Raspberry Pi 5 (CPU) ~3 tok/s
- Gemma 4 **E2B** is roughly half the size and should decode meaningfully faster, but official Android benchmarks are not yet published for E2B at the time of writing
- Target hardware ($80–100 Android phone, CPU only): planning estimate 6–15 tok/s — not officially benchmarked

We plan for CPU-only as the baseline. GPU acceleration is upside, not a dependency.

### Conversion pipeline (Unsloth → on-device)

The path from fine-tune weights to a file the app can load:

1. **Unsloth fine-tune** on Colab T4 → LoRA adapter weights
2. **Merge LoRA** into base Gemma 4 E2B weights → full Hugging Face safetensors
3. **Convert with `litert-torch export_hf`** (the current Google-recommended tool for safetensors → `.litertlm`)
4. **Quantize to INT4** as part of the export step
5. **Bundle into `.litertlm`** with tokenizer and encoder components
6. **Distribute** via Hugging Face Hub under the project's account
7. **Download on first app launch** with SHA-256 verification

Note: this replaces the earlier `ai-edge-torch` path. `ai-edge-torch` is still referenced in older classic `.tflite` material, but the current Gemma 4 LiteRT-LM guides direct custom safetensors conversion through `litert-torch`.

The pipeline is validated on **base Gemma 4 E2B first** (Conversion Track Phase A below), independently of and in parallel with app development, so we know the toolchain works before fine-tuned weights become the critical path.

### App architecture

Kotlin 2.0+ with Jetpack Compose. Manual dependency injection (no Hilt). The entire app is structured around one abstraction:

```
interface BeekeepingModel {
    suspend fun load(config: ModelConfig)
    suspend fun diagnose(text, image?, audio?): Flow<String>
    suspend fun unload()
}
```

The implementation (`LiteRtLmModel`) is the only place that knows about LiteRT. Everything else — UI, multimodal capture, conversation persistence — talks to the interface. This is the swap point: when Apiary weights replace base Gemma weights, only the `.litertlm` file on disk changes. No code changes, no rebuild.

Repository layout (everything at repo root, since `gemma4goodBeeCare` is exclusively this Android project):

```
gemma4goodBeeCare/
├── PLAN.md                          # this file
├── README.md
├── build.gradle.kts                 # root
├── settings.gradle.kts
├── gradle/libs.versions.toml        # version catalog
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── kotlin/com/beecareanywhere/
│           ├── BeeCareApp.kt
│           ├── MainActivity.kt
│           ├── model/
│           │   ├── BeekeepingModel.kt      # interface — the swap point
│           │   ├── LiteRtLmModel.kt        # LiteRT-LM implementation
│           │   ├── ModelConfig.kt          # path, system prompt, gen params
│           │   └── ModelRepository.kt      # download, verify SHA-256, cache
│           ├── multimodal/
│           │   ├── ImageCapture.kt         # CameraX → cache File → Content.ImageFile
│           │   └── AudioCapture.kt         # AudioRecord → PCM ByteArray → Content.AudioBytes
│           ├── ui/
│           │   ├── DiagnosticScreen.kt
│           │   ├── ChatScreen.kt
│           │   ├── ModelDownloadScreen.kt
│           │   └── theme/
│           ├── data/
│           │   ├── ConversationStore.kt    # Room
│           │   └── Settings.kt             # language pref, model path
│           └── di/
│               └── ServiceLocator.kt       # manual DI
└── docs/
    ├── ARCHITECTURE.md
    └── MODEL_PIPELINE.md                   # Unsloth → litert-torch → .litertlm walkthrough
```

### Multimodal input

The LiteRT-LM `Content` API distinguishes between file-backed and byte-backed inputs, which shapes the capture code:

- **Image** (`Content.ImageFile(File)`): CameraX captures a frame → write to app cache file → pass `File` reference to the engine → delete after query completes
- **Audio** (`Content.AudioBytes(ByteArray)`): `AudioRecord` captures 16 kHz mono PCM → keep in memory as `ByteArray` → pass to engine → discard
- **Text**: standard Compose `TextField`

Gemma 4 E2B is natively multimodal, so the same model handles all three input modes. We're not training new encoders.

### Target hardware

- **minSdk 28** (Android 9.0, 2018)
- **targetSdk 35**
- **Documented minimum**: 3 GB RAM, ~2.5 GB free storage
- **Demo recording device**: 4 GB+ RAM preferred so OOM isn't a demo failure mode. A ~2 GB E2B model plus app and system overhead is workable on 3 GB but tight; 4 GB gives comfortable headroom. Acquiring this device is a Phase 1 dependency.

## What we explicitly are not doing

- **Not shipping to Google Play.** Sideload APK from GitHub Releases is sufficient for a hackathon submission.
- **Not building a polished onboarding flow.** First-launch UX is "download model, then chat" — nothing more.
- **Not implementing offline-first sync, multi-user accounts, or cloud backup.** Single-user, single-device, fully local.
- **Not supporting tablets or foldables specially.** Phone-only portrait layout.
- **Not fine-tuning the vision or audio encoders.** Text-only fine-tune; multimodal capability rides on Gemma 4's existing encoders.
- **Not writing a custom tokenizer or sampling loop.** LiteRT-LM provides these.
- **Not shipping multiple model size variants.** E2B only, one binary, one download.
- **Not localizing the app UI itself.** English buttons; the model speaks Swahili and Amharic. Future work.
- **Not implementing a RAG layer for citations.** Optional stretch only; not in scope.

## Development plan

Two parallel tracks. The App Track is sequential; the Conversion Track starts after App Track Phase 1 and runs independently until they converge at App Track Phase 4 (swap-in).

### App Track

#### Phase 1 — Buildable shell

Set up the Android project so it compiles, installs, and launches to a blank screen with the app name. No model integration yet. Includes the environment setup that's prerequisite to all downstream work.

**Definition of done:**
- Android SDK installed (cmdline-tools sufficient), SDK licenses accepted
- Gradle wrapper generated; `./gradlew tasks` succeeds
- `./gradlew installDebug` deploys to a connected device
- App launches and shows a Compose screen titled "BeeCare Anywhere"
- All package coordinates, signing config, version catalog, and resource scaffolding in place
- A 4 GB+ Android device identified and available for the demo
- `.gitignore`, `README.md`, basic project metadata committed

#### Phase 2 — Prebuilt model integration (text-only)

Load the prebuilt `litert-community/gemma-4-E2B-it-litert-lm` from the device's app-private storage, run text-in / text-out inference, stream tokens to the UI. Model file is side-loaded via `adb push` during development. **Critically, this uses the prebuilt model — not a custom conversion** — so any failures here are unambiguously Android-integration failures.

**Definition of done:**
- `LiteRtLmModel` fully implements `BeekeepingModel` for text using real `EngineConfig` / `Engine` APIs
- Hard-coded path: `/data/data/com.beecareanywhere/files/models/gemma-4-E2B-it-litert-lm.litertlm`
- Send a beekeeping question → see streamed tokens appear in the UI
- Model loads in <20 seconds on the demo phone (E2B is smaller than E4B; load time scales with file size)
- Generation produces coherent text at ≥6 tok/s on the demo phone

#### Phase 3 — Multimodal capture

Add image and audio input paths.

**Definition of done:**
- Camera permission flow + capture UI works; frames written to cache file, passed as `Content.ImageFile`
- Microphone permission flow + capture UI works; PCM bytes passed as `Content.AudioBytes`
- Image attached to query measurably changes the response — sanity check that bytes reach the engine, not silently dropped
- Audio attached produces a different response than text-only
- Combined modalities work (text + image + audio in one query)
- Temp file cleanup verified — no leaked image files in cache after queries complete

#### Phase 4 — Model swap-in readiness

Make swapping from prebuilt Gemma to Apiary trivial. Adds the in-app download flow, model verification, and the configuration point that selects which model to load.

**Definition of done:**
- First launch detects no model present, shows download screen, fetches from a configured URL with progress + resume on failure
- `ModelConfig.modelPath` is the single string that decides which model the app uses
- Switching from prebuilt to Apiary requires changing one constant
- Model file integrity verified by SHA-256 before load
- App refuses to load corrupted or truncated files with a clear error
- This phase requires Conversion Track Phase C complete (Apiary `.litertlm` uploaded to a download URL)

#### Phase 5 — Demo polish

Everything needed for the submission video to look credible.

**Definition of done:**
- App icon + adaptive icon
- Splash screen with BeeCare Anywhere branding
- Friendly error messages for: model missing, model corrupted, out of memory, camera/mic denied
- Settings screen: model info, language preference toggle (Swahili / Amharic / English), "delete model" debug action
- README and `docs/MODEL_PIPELINE.md` complete
- APK builds, signs, and installs cleanly from a fresh checkout

### Conversion Track (parallel, starts after App Track Phase 1)

#### Phase A — Validate `litert-torch export_hf` on base Gemma 4 E2B

Run the full conversion pipeline on base Gemma 4 E2B weights from Hugging Face. The output should be byte-identical-in-spirit to the prebuilt `litert-community/gemma-4-E2B-it-litert-lm` (same architecture, same quantization).

**Definition of done:**
- `litert-torch export_hf` runs to completion on base Gemma 4 E2B safetensors
- Output `.litertlm` file produced, ~2 GB, loads in the LiteRT-LM Android library
- Inference output matches prebuilt model qualitatively on a handful of test prompts
- Conversion script committed to `scripts/convert_to_litertlm.py` (or equivalent)

#### Phase B — Apply pipeline to Apiary fine-tune

Once the Apiary fine-tune is trained: merge LoRA adapters into base weights, run the same conversion script, produce `apiary-4-E2B-int4.litertlm`.

**Definition of done:**
- Merged HF weights produced from Apiary LoRA adapters
- Conversion succeeds, producing valid `.litertlm`
- File loads in the app and produces region-specific responses
- SHA-256 computed and recorded

#### Phase C — Upload + distribution

Push the Apiary `.litertlm` to Hugging Face Hub under the project's account. Wire up the download URL into `ModelConfig`.

**Definition of done:**
- File public on HF Hub at a stable URL
- SHA-256 documented alongside the file
- App's download flow successfully fetches it on a fresh install

## Risk profile

**What can go wrong:**

1. **LiteRT-LM Android library version mismatch.** The current docs show concrete Kotlin signatures, but artifact versions move. Mitigation: pin specific versions in `libs.versions.toml`, validate against current docs before bumping.

2. **Conversion pipeline fails on the Apiary fine-tune even after validating on base.** Standard Unsloth LoRA fine-tunes preserve structure, but custom training configs can produce weights that `litert-torch` rejects. Mitigation: validate on base first (Conversion Phase A), keep Unsloth training config as vanilla as possible. If Phase B fails specifically, demo with base Gemma in the app and acknowledge the gap in the writeup.

3. **The model runs too slowly on the target $80–100 phone to feel usable.** Even with E2B's smaller size, low-end ARM CPUs may decode at the bottom of the 6–15 tok/s estimate. A 200-token response at 6 tok/s is ~33 seconds — better than E4B but still a wait. Mitigation: pre-warm model on app launch, stream tokens as proof of life, frame the wait as "thinking" with a progress indicator. Record demo on the 4 GB+ device first, where decode is closer to the upper end of the range.

4. **Multimodal input degrades badly under INT4 quantization.** Cross-modal attention is the most quantization-sensitive part of Gemma 4. Mitigation: test multimodal output with the prebuilt model during Phase 3, before any fine-tuning is on the critical path. If it's broken on the prebuilt model, it's a published-model issue, not ours; fall back to text-only demo with a known-good visual mockup of the multimodal feature.

5. **Demo phone OOM kills the app mid-recording.** Lower risk with E2B than it was with E4B — a ~2 GB model on a 4 GB device has reasonable headroom — but still real on 3 GB devices or if the OS is memory-pressured. Mitigation: insist on the 4 GB+ demo device. Add a debug log of peak memory during model load + first query so we know how close to the edge we're running.

6. **Permissions UX traps a non-technical viewer in the video.** Mitigation: explanation cards before each permission request, fresh-install flow captured end-to-end in the recording.

**Mitigations baked into the plan:**

- App Track runs against the prebuilt model first — the prebuilt and the custom conversion are decoupled failure modes
- The `BeekeepingModel` interface means we can swap runtimes without touching UI code
- Side-load model via `adb` during development; defer the download flow to Phase 4 so download UX doesn't block model integration
- Validate the entire Unsloth → LiteRT conversion path on base weights before fine-tuning starts, not after

## Stack summary

- **App**: Kotlin 2.0+, Jetpack Compose (Compose Compiler Gradle plugin from Kotlin), Compose BOM
- **Build**: Gradle 8.x with Kotlin DSL, version catalog (`libs.versions.toml`)
- **Min/target SDK**: 28 / 35
- **Runtime**: LiteRT + LiteRT-LM Android library
- **Camera**: CameraX
- **Audio**: AudioRecord (raw PCM, 16 kHz mono)
- **Persistence**: Room (conversation history), DataStore (settings)
- **Dependency injection**: Manual (`ServiceLocator`), no Hilt
- **Model format**: `.litertlm`, INT4 quantized, ~2 GB
- **Conversion tool**: `litert-torch export_hf`
- **Distribution**: Sideloaded APK from GitHub Releases; model from Hugging Face Hub
- **Repository**: github.com/manthatwalks/gemma4goodBeeCare
- **Local working directory**: `~/BeeCare`

## The pitch sentence

BeeCare Anywhere is the Android shell that puts Gemma Apiary in a beekeeper's hand — a $80 phone, 2 GB on disk, no network connection required, text + photo + audio in, regional diagnostic advice out, with a one-file swap to upgrade from base Gemma to the fine-tuned model the day after our hackathon submission.

## Sources (verified May 16, 2026)

- [Gemma 4 — Google DeepMind](https://deepmind.google/models/gemma/gemma-4/)
- [LiteRT overview](https://ai.google.dev/edge/litert/)
- [LiteRT-LM overview](https://ai.google.dev/edge/litert-lm/overview)
- [LiteRT-LM Android guide](https://ai.google.dev/edge/litert-lm/android)
- [Gemma 4 LiteRT-LM guide](https://ai.google.dev/edge/litert-lm/models/gemma-4)
- [E2B LiteRT-LM model card](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm) *(URL follows the E4B naming convention; verify presence and exact file size on the live page before locking the download URL in Phase 4)*
- [Compose setup](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler)
