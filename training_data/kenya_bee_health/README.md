---
pretty_name: Kenya Bee Health Q/A Image Triples
language:
  - en
license: other
task_categories:
  - visual-question-answering
  - image-classification
  - text-generation
tags:
  - bees
  - beekeeping
  - kenya
  - agriculture
  - computer-vision
  - multimodal
  - gemma
  - unsloth
size_categories:
  - n<1K
---

# Kenya Bee Health Training Data

This folder is a starter database for BeeCare Anywhere / Gemma Apiary. It is intentionally small, transparent, and license-aware: use it to prove the Q/A/image-triple pipeline, then expand it with Kenyan field data before trusting model behavior in production.

## Important Model Note

`google/gemma-2b` is a text-to-text, decoder-only model. It cannot directly read pictures. Use these image triples with a vision-capable model path, for example Gemma 4 E2B/E4B, PaliGemma, or Qwen-VL through Unsloth vision fine-tuning. If you must stay on Gemma 2B, train a separate image/audio classifier first, then pass structured findings into Gemma 2B as text.

## Files

- `bee_health_qa_image_triples.seed.jsonl` - Q/A/picture triples with source URL, license, attribution, condition label, and Kenya-specific advice.
- `train/metadata.jsonl`, `validation/metadata.jsonl`, `test/metadata.jsonl` - Hugging Face ImageFolder-style training rows. These are the actual split files to load for training.
- `train/images/`, `validation/images/`, `test/images/` - split-local image files referenced by each `metadata.jsonl`.
- `sources.json` - source manifest and recommended datasets to scale beyond the seed set.
- `download_images.py` - downloads the seed images from Wikimedia Commons `Special:Redirect/file/...` URLs and validates required fields.
- `prepare_hf_splits.py` - regenerates the split layout from the source JSONL.
- `production/` - production dataset schema, label taxonomy, field collection protocol, validator, and Unsloth export tooling.

## Quick Start

From the repo root:

```sh
python3 training_data/kenya_bee_health/download_images.py
python3 training_data/kenya_bee_health/download_images.py --validate-only
python3 training_data/kenya_bee_health/prepare_hf_splits.py
```

To mirror the public Hugging Face dataset `iphonezoomcalll/kenya-bee-diagnostics` into your own dataset account, run:

```sh
HF_TOKEN=hf_... python3 training_data/kenya_bee_health/mirror_hf_dataset.py \
  your-username/kenya-bee-diagnostics
```

That script uses `datasets.load_dataset("iphonezoomcalll/kenya-bee-diagnostics")` and then pushes the loaded dataset into your target dataset repo.

Downloaded images land under `training_data/kenya_bee_health/images/`, matching each record's `picture` field.

For training from Hugging Face, use the split metadata files. Each row contains:

- `file_name` - relative image path for the Hub dataset viewer and image loader.
- `question` - user prompt.
- `answer` - target assistant response.
- `condition_label`, `task`, `safety_tags`, and provenance fields for filtering, evaluation, and audits.

For the strongest currently available public-resource training data, use `production/manifests/public_resource_bilingual_manifest.jsonl` and the exports in `production/exports/`. It contains English + Swahili examples. The smaller `production_seed_manifest.jsonl` is only a smoke test.

## Recommended Databases To Use

Use a mixed data strategy instead of one monolithic dataset:

- Visual bee/parasite classification: TensorFlow `bee_dataset` / BeeAlarmed BeeDataset for varroa, pollen, cooling behavior, and wasp distinction.
- Varroa detection: Zenodo `VarroaDataset` for high-volume healthy/infected bee images and predefined train/test/validation splits.
- Rare visual conditions: Wikimedia Commons and Bugwood/IPMImages for license-checkable examples of small hive beetle larvae, wax moth webbing, deformed wing virus, bearding, and safari ants.
- Kenyan context labels: local photos from Langstroth and Kenya Top Bar Hive inspections, labeled by extension officers or experienced beekeepers. This is the critical gap because imported datasets underrepresent `Apis mellifera scutellata`, ASAL conditions, local hives, local lighting, and phone-camera quality.
- Acoustic hive health: Mendeley `Queenless honeybee acoustic patterns`, BeeTogether/OSBH/NuHive, AI-Belha, MSPB, and UrBAN are useful starting points. Starvation/dearth and pre-absconding audio need local collection because public datasets are not well labeled for Kenyan drought-driven absconding.

## Unsloth Shape

For Unsloth vision fine-tuning, convert each row into the chat format expected by the selected VLM. The `picture` field is the local image path; `question` is the user message; `answer` is the assistant response. Keep the metadata fields for audit, split stratification, and license review.

## Safety Rules Baked Into Answers

- The model should never recommend Sevin/Carbaryl dust inside a hive.
- Pest advice should emphasize inspection, reducing excess hive space, freezing/destroying badly infested comb, sanitation, barriers, water, shade, ventilation, and locally approved treatments.
- Any treatment answer should tell the beekeeper to follow the product label and local extension guidance.
- Ant barriers should stay outside the hive and away from comb/honey. This dataset avoids teaching engine-oil use because it can contaminate hive products and soil.

## Expansion Targets

Before serious fine-tuning, aim for at least:

- 1,000+ Kenyan healthy frame / entrance / top-bar images.
- 500+ bearding vs swarming-lookalike entrance images with temperature/time notes.
- 500+ wax moth, SHB, ant, and mixed-pest images.
- 500+ worker closeups for DWV/varroa/normal/pollen lookalikes.
- 100+ hours of labeled ASAL hive audio covering nectar flow, drought dearth, queenright, queenless, active robbing, rain, wind, and post-harvest stress.
