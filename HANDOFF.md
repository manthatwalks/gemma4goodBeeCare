# HANDOFF

## Goal

Build BeeCare Anywhere: an Android app shell for on-device Gemma Apiary diagnostics. Real LiteRT-LM integration is intentionally skipped until the tuned Gemma 4 E2B `.litertlm` model is ready; current work should keep the app usable through `StubModel` while preserving the runtime swap point.

## Current Progress

- Local `main` is 5 commits ahead of `origin/main`; origin is still `13fde0d feat: phase 1 buildable shell`.
- Local-only commits add: `BeekeepingModel`/`StubModel`, image capture, audio capture, diagnostic UI, model download flow, DataStore settings, settings screen, splash, and launcher icon.
- Codex has additional uncommitted changes on top:
  - AGP `8.13.2`, Gradle `8.13`, Compose BOM `2026.04.01` so Compose UI resolves to `1.11.0`.
  - Backup disabled and backup/device-transfer XML excludes app data.
  - `ModelRepository` hardened: HTTPS-only URLs, safe `.litertlm` filename validation, partial cleanup, cancellation-aware OkHttp call, SHA format validation, truncated-download detection.
  - Default prebuilt E2B filename fixed to `gemma-4-E2B-it.litertlm`.
  - Camera temp files cleaned on failure; audio capture startup/stop is safer and capped at 30 seconds.
  - `docs/MODEL_PIPELINE.md` added for Unsloth -> merged HF -> `litert-torch export_hf` -> `.litertlm`.
  - `README.md` updated to describe the current stub demo app.
- `training_data/kenya_bee_health/` exists as untracked local work; Codex did not edit its contents.

## What Worked

- Verified commands after Codex changes:
  - `./gradlew assembleDebug`
  - `./gradlew lintDebug`
  - `./gradlew testDebugUnitTest` (passes, but no tests exist yet)
  - `./gradlew installDebug` on Pixel 8 emulator
  - `adb shell am start -n com.beecareanywhere/.MainActivity`
- Compose warning from Android Studio is fixed by Compose BOM `2026.04.01`, which resolves `androidx.compose.ui:ui:1.11.0`.
- Lint crash from newer Compose/old AGP was fixed by aligning AGP to `8.13.2` and Gradle wrapper to `8.13`.
- Current E2B prebuilt LiteRT-LM file name is `gemma-4-E2B-it.litertlm` in `litert-community/gemma-4-E2B-it-litert-lm`.

## What Didn't Work

- Do not push only the 5 local commits without the uncommitted dependency changes: local builds were verified with those dirty Gradle/Compose edits present.
- Compose BOM `2026.05.00` resolved Compose UI to `1.11.1`, but lint still crashed with the old AGP setup. The stable local fix is BOM `2026.04.01` plus AGP `8.13.2`.
- `./gradlew lintDebug` previously crashed with `NonNullableMutableLiveDataDetector`/Kotlin analysis API errors under AGP `8.7.3` after the Compose bump.
- Real LiteRT-LM Android integration is not present. `ServiceLocator` still returns `StubModel()`.

## Next Steps

1. Review the dirty working tree and decide commit shape. Recommended: one follow-up commit for Codex hardening/docs/dependency alignment, then push the full local stack together.
2. Include or intentionally ignore `training_data/kenya_bee_health/`; it is untracked. If included, keep downloaded `training_data/**/images/` ignored.
3. Manually smoke-test on emulator: diagnostic text submit, cancel streaming, camera permission/capture, audio record/stop, settings language chips, model download screen without starting a full 2+ GB download.
4. Add minimal unit tests around `ModelRepository` filename validation/SHA handling if time permits.
5. When tuned E2B is ready, implement `LiteRtLmModel`, swap `StubModel()` in `ServiceLocator`, set Apiary URL/filename/SHA, and rerun install + airplane-mode demo.
