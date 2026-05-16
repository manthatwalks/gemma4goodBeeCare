# Production Dataset Package

This directory is the production data contract for BeeCare Anywhere. It is designed for real Kenyan field collection and supervised fine-tuning, not just a demo upload.

## The Honest Status

The current repository contains a seed dataset with 24 Q/A/image examples. That is useful for testing the pipeline, but it is not large or local enough to be the final model-training dataset.

A production dataset needs real, labeled observations from Kenyan apiaries:

- Movable-frame hive photos from Langstroth and Kenya Top Bar Hive inspections.
- Entrance photos for bearding, robbing, absconding aftermath, and normal traffic.
- Worker closeups for varroa, deformed wings, pollen lookalikes, and normal bees.
- Comb/frame photos for wax moth, small hive beetle slimeout, healthy brood, stores, and weak-colony patterns.
- Hive audio with local context: drought/dearth, queenright, queenless, noisy disturbance, wind/rain, and normal nectar flow.
- Reviewer labels from trained beekeepers, extension officers, or apiculture researchers.

## Production Files

- `schema.json` - required record format for every production row.
- `label_taxonomy.json` - allowed condition labels, tasks, safety flags, and severity levels.
- `manifests/production_seed_manifest.jsonl` - seed rows migrated into the production schema and clearly marked as pilot data.
- `manifests/public_resource_bilingual_manifest.jsonl` - the strongest public-resource dataset currently generated here: English + Swahili, image-backed + text-only rows, grounded in public apiculture guidance and public seed images.
- `manifests/field_collection_template.jsonl` - blank-ish examples showing how new Kenyan field rows should look.
- `docs/collection_protocol.md` - how to collect photos/audio in the field.
- `docs/annotation_guide.md` - how reviewers should label records.
- `docs/minimum_dataset_targets.md` - target counts before fine-tuning should be trusted.
- `scripts/validate_manifest.py` - validates schema, labels, media paths, and split leakage.
- `scripts/export_unsloth_vision.py` - exports rows into a vision-language fine-tuning JSONL shape.
- `scripts/export_unsloth_text.py` - exports text-only rows for Gemma 2B-style supervised fine-tuning.
- `scripts/generate_public_resource_bilingual.py` - regenerates the bilingual public-resource manifest.

## Production Rule

Do not train a final field-deployment model only on `production_seed_manifest.jsonl`. Use `public_resource_bilingual_manifest.jsonl` as the best currently available public-resource training set, then replace or augment it with field data as soon as you have it.

```sh
python3 training_data/kenya_bee_health/production/scripts/validate_manifest.py \
  training_data/kenya_bee_health/production/manifests/public_resource_bilingual_manifest.jsonl

python3 training_data/kenya_bee_health/production/scripts/export_unsloth_vision.py \
  training_data/kenya_bee_health/production/manifests/public_resource_bilingual_manifest.jsonl \
  training_data/kenya_bee_health/production/exports/unsloth_vision_public_bilingual.jsonl

python3 training_data/kenya_bee_health/production/scripts/export_unsloth_text.py \
  training_data/kenya_bee_health/production/manifests/public_resource_bilingual_manifest.jsonl \
  training_data/kenya_bee_health/production/exports/unsloth_text_public_bilingual.jsonl
```

Current public-resource counts:

- 160 total records.
- 80 English, 80 Swahili.
- 80 image + question + answer records.
- 80 text-only diagnostic/advice records.
- Split counts: train 144, validation 8, test 8.

## Fine-Tuning Target

For image+text training, use a vision-capable model path. Plain Gemma 2B is text-only; it can use image-classifier outputs as text, but it cannot directly consume the images.
