#!/usr/bin/env python3
"""Export production manifest records to a vision-language JSONL shape."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


def load_rows(path: Path) -> list[dict]:
    with path.open("r", encoding="utf-8") as handle:
        return [json.loads(line) for line in handle if line.strip()]


def should_export(row: dict, include_pilot: bool) -> bool:
    if row["dataset_status"] == "excluded":
        return False
    if row["dataset_status"] == "pilot_seed" and not include_pilot:
        return False
    if row["split"] in {"validation", "test", "holdout"}:
        return False
    return row["media"].get("image_path") is not None


def to_record(row: dict, repo_root: Path) -> dict:
    image_path = str(repo_root / row["media"]["image_path"])
    system = (
        "You are BeeCare Anywhere, an offline assistant for Kenyan beekeepers. "
        "Diagnose cautiously from the provided hive media and give safe, practical advice. "
        "Do not recommend Carbaryl, Sevin dust, or unapproved insecticides inside hives."
    )
    user_text = (
        f"{row['prompt']}\n\n"
        f"Context: country={row['location_context']['country']}; "
        f"county={row['location_context'].get('county')}; "
        f"season={row['location_context']['season_context']}; "
        f"hive_type={row['hive_context']['hive_type']}."
    )
    return {
        "id": row["record_id"],
        "image": image_path,
        "messages": [
            {"role": "system", "content": system},
            {
                "role": "user",
                "content": [
                    {"type": "image", "image": image_path},
                    {"type": "text", "text": user_text},
                ],
            },
            {"role": "assistant", "content": row["target_answer"]},
        ],
        "metadata": {
            "primary_condition": row["labels"]["primary_condition"],
            "secondary_conditions": row["labels"]["secondary_conditions"],
            "severity": row["labels"]["severity"],
            "safety_flags": row["review"]["safety_flags"],
            "dataset_status": row["dataset_status"],
            "source_media_id": row["media"]["source_media_id"],
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("manifest", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--include-pilot", action="store_true", help="Include pilot_seed rows for smoke tests.")
    parser.add_argument(
        "--image-path-mode",
        choices=["absolute", "repo-relative"],
        default="absolute",
        help="Use absolute local image paths or repo-relative paths for Hugging Face-hosted training.",
    )
    args = parser.parse_args()

    repo_root = Path(__file__).resolve().parents[4]
    rows = load_rows(args.manifest)
    exported = []
    for row in rows:
        if not should_export(row, args.include_pilot):
            continue
        record = to_record(row, repo_root)
        if args.image_path_mode == "repo-relative":
            image_path = row["media"]["image_path"]
            record["image"] = image_path
            record["messages"][1]["content"][0]["image"] = image_path
        exported.append(record)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8") as handle:
        for row in exported:
            handle.write(json.dumps(row, ensure_ascii=False) + "\n")
    print(f"Exported {len(exported)} records to {args.output}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
