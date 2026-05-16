#!/usr/bin/env python3
"""Validate BeeCare production manifest JSONL files."""

from __future__ import annotations

import argparse
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path


REQUIRED_TOP_LEVEL = {
    "record_id",
    "dataset_status",
    "split",
    "modality",
    "media",
    "location_context",
    "hive_context",
    "labels",
    "prompt",
    "target_answer",
    "review",
    "provenance",
}


def load_taxonomy(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def load_rows(path: Path) -> list[dict]:
    rows = []
    with path.open("r", encoding="utf-8") as handle:
        for line_number, line in enumerate(handle, start=1):
            if not line.strip():
                continue
            try:
                row = json.loads(line)
            except json.JSONDecodeError as exc:
                raise SystemExit(f"{path}:{line_number}: invalid JSON: {exc}") from exc
            row["_line_number"] = line_number
            rows.append(row)
    return rows


def fail(errors: list[str]) -> None:
    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        raise SystemExit(1)


def validate(path: Path, repo_root: Path, require_media: bool) -> None:
    taxonomy = load_taxonomy(path.parents[1] / "label_taxonomy.json")
    rows = load_rows(path)
    errors: list[str] = []
    seen_ids: set[str] = set()
    by_media: dict[str, set[str]] = defaultdict(set)
    label_counts: Counter[str] = Counter()
    split_counts: Counter[str] = Counter()

    allowed_conditions = set(taxonomy["primary_conditions"])
    allowed_tasks = set(taxonomy["tasks"])
    allowed_flags = set(taxonomy["safety_flags"])
    allowed_modalities = set(taxonomy["modalities"])

    for row in rows:
        line = row["_line_number"]
        missing = REQUIRED_TOP_LEVEL - set(row)
        if missing:
            errors.append(f"line {line}: missing top-level fields: {sorted(missing)}")
            continue

        record_id = row["record_id"]
        if record_id in seen_ids:
            errors.append(f"line {line}: duplicate record_id {record_id}")
        seen_ids.add(record_id)

        modality = row["modality"]
        if modality not in allowed_modalities:
            errors.append(f"line {line}: unknown modality {modality}")

        labels = row["labels"]
        primary = labels.get("primary_condition")
        task = labels.get("task")
        severity = labels.get("severity")
        if primary not in allowed_conditions:
            errors.append(f"line {line}: unknown primary_condition {primary}")
        if task not in allowed_tasks:
            errors.append(f"line {line}: unknown task {task}")
        if not isinstance(severity, int) or severity < 0 or severity > 3:
            errors.append(f"line {line}: severity must be integer 0-3")
        for secondary in labels.get("secondary_conditions", []):
            if secondary not in allowed_conditions:
                errors.append(f"line {line}: unknown secondary_condition {secondary}")

        for flag in row["review"].get("safety_flags", []):
            if flag not in allowed_flags:
                errors.append(f"line {line}: unknown safety flag {flag}")

        media = row["media"]
        source_media_id = media.get("source_media_id")
        split = row["split"]
        by_media[source_media_id].add(split)
        label_counts[primary] += 1
        split_counts[split] += 1

        for field in ("image_path", "audio_path"):
            value = media.get(field)
            if value:
                media_path = repo_root / value
                if require_media and not media_path.exists():
                    errors.append(f"line {line}: missing {field}: {media_path}")

        if modality == "image" and not media.get("image_path"):
            errors.append(f"line {line}: image modality requires image_path")
        if modality == "audio" and not media.get("audio_path"):
            errors.append(f"line {line}: audio modality requires audio_path")
        if len(row["prompt"].strip()) < 20:
            errors.append(f"line {line}: prompt is too short")
        if len(row["target_answer"].strip()) < 40:
            errors.append(f"line {line}: target_answer is too short")

    leakage = {
        media_id: splits
        for media_id, splits in by_media.items()
        if len(splits - {"pilot", "unassigned"}) > 1
    }
    if leakage:
        preview = ", ".join(f"{media_id}:{sorted(splits)}" for media_id, splits in list(leakage.items())[:10])
        errors.append(f"source_media_id appears across train/validation/test-style splits: {preview}")

    fail(errors)

    print(f"Validated {len(rows)} records in {path}")
    print(f"Splits: {dict(sorted(split_counts.items()))}")
    print(f"Primary labels: {dict(sorted(label_counts.items()))}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("manifest", type=Path)
    parser.add_argument("--allow-missing-media", action="store_true")
    args = parser.parse_args()
    repo_root = Path(__file__).resolve().parents[4]
    validate(args.manifest.resolve(), repo_root, require_media=not args.allow_missing_media)
    return 0


if __name__ == "__main__":
    sys.exit(main())
