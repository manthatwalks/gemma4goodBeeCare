# BeeCare Anywhere Model Pipeline

This document is the handoff path from Gemma Apiary training artifacts to the Android app. The app currently runs `StubModel`; real LiteRT-LM integration is intentionally deferred until the tuned E2B model exists.

## Current App Target

- Base deployment model: `litert-community/gemma-4-E2B-it-litert-lm`
- Default app file: `gemma-4-E2B-it.litertlm`
- Default download URL: `https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm`
- Final Apiary file name: `apiary-4-E2B-int4.litertlm`
- Runtime swap point: replace `StubModel()` in `ServiceLocator` with the future `LiteRtLmModel(context)` implementation, then point settings/default config at the Apiary `.litertlm` file.

## Data Shape

Use supervised chat examples. Keep raw PDFs, field notes, images, and audio as source material, but train on normalized examples that show the model exactly how to answer.

Text-first JSONL:

```json
{"messages":[{"role":"system","content":"You are BeeCare Anywhere, an offline diagnostic assistant for smallholder beekeepers. Give practical, region-aware advice in the user's language. If evidence is incomplete, say what to inspect next."},{"role":"user","content":"Language: Swahili\nRegion: western Kenya\nSeason: rainy season\nHive observations: capped brood has pinholes, larvae smell sour, workers are removing brood, no visible mites.\nQuestion: What might be wrong and what should I do next?"},{"role":"assistant","content":"Dalili hizi zinaweza kuashiria ugonjwa wa brood, lakini zinahitaji uthibitisho zaidi. Kagua muundo wa brood, harufu, mabuu yaliyokufa, na wasiliana na afisa ugani kabla ya kutumia tiba yoyote..."}]}
```

Recommended first tune:

- 70-85% text-only diagnostic and advice examples.
- 10-20% multilingual variants in Swahili, Amharic, and English.
- 5-10% refusal/uncertainty examples where the right answer is to ask for inspection details or local expert review.

Keep image/audio records in a separate multimodal dataset until the text model is working. The seed set under `training_data/kenya_bee_health/` is useful for proving the data format, not enough for a trusted fine-tune.

## Training With Unsloth

Use Unsloth's Gemma 4 E2B text fine-tuning path first. Train LoRA adapters, evaluate them, then merge them into a Hugging Face safetensors model for LiteRT export.

High-level notebook flow:

```python
from datasets import load_dataset
from unsloth import FastLanguageModel
from trl import SFTTrainer

model, tokenizer = FastLanguageModel.from_pretrained(
    model_name="google/gemma-4-E2B-it",
    max_seq_length=8192,
    load_in_4bit=True,
)

model = FastLanguageModel.get_peft_model(
    model,
    r=16,
    lora_alpha=32,
    lora_dropout=0,
    target_modules=[
        "q_proj",
        "k_proj",
        "v_proj",
        "o_proj",
        "gate_proj",
        "up_proj",
        "down_proj",
    ],
)

dataset = load_dataset("json", data_files="apiary_sft.jsonl", split="train")

trainer = SFTTrainer(
    model=model,
    tokenizer=tokenizer,
    train_dataset=dataset,
    dataset_text_field="text",
    max_seq_length=8192,
)

trainer.train()
model.save_pretrained("apiary-lora")
tokenizer.save_pretrained("apiary-lora")
model.save_pretrained_merged(
    "apiary-4-E2B-merged",
    tokenizer,
    save_method="merged_16bit",
)
```

If the notebook uses a `messages` column instead of a preformatted `text` column, apply the Gemma chat template during preprocessing and train on the rendered text. Do not train on hidden chain-of-thought.

## Export To LiteRT-LM

Validate export on base Gemma before exporting Apiary. The current Google AI Edge Gemma 4 docs use `litert-torch export_hf` for safetensors to `.litertlm`.

Base model smoke test:

```sh
uv tool install litert-torch-nightly

litert-torch export_hf \
  --model=google/gemma-4-E2B-it \
  --output_dir=/tmp/gemma4-e2b-litert \
  --externalize_embedder \
  --jinja_chat_template_override=litert-community/gemma-4-E2B-it-litert-lm
```

Apiary export:

```sh
litert-torch export_hf \
  --model=/path/to/apiary-4-E2B-merged \
  --output_dir=/tmp/apiary-4-e2b-litert \
  --externalize_embedder \
  --jinja_chat_template_override=litert-community/gemma-4-E2B-it-litert-lm

mv /tmp/apiary-4-e2b-litert/model.litertlm apiary-4-E2B-int4.litertlm
sha256sum apiary-4-E2B-int4.litertlm
```

Record the SHA-256 and set it in app settings/config before enabling first-launch download. The Android downloader refuses invalid filenames, requires HTTPS, writes to a `.partial` file, verifies SHA-256 when pinned, and atomically renames on success.

## Acceptance Checklist

- LoRA evaluation beats base E2B on held-out bee-health prompts.
- Merged HF model answers the same eval set coherently before export.
- Exported `.litertlm` loads with `litert-lm run` locally.
- Android app loads the same `.litertlm` from app-private storage.
- Airplane-mode text prompt produces regional advice in the requested language.
- SHA-256 in app config matches the uploaded model file.

## References

- Google AI Edge Gemma 4 LiteRT-LM guide: https://ai.google.dev/edge/litert-lm/models/gemma-4
- Unsloth Gemma 4 fine-tuning guide: https://unsloth.ai/docs/models/gemma-4/train
- Prebuilt E2B LiteRT-LM model: https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm
