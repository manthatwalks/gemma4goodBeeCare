#!/usr/bin/env python3
"""Export text-only production rows for Gemma-style supervised fine-tuning."""

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
    return row["modality"] == "text_only"


def to_record(row: dict) -> dict:
    language = row.get("language", "en")
    system = (
        "You are BeeCare Anywhere, an offline assistant for Kenyan beekeepers. "
        "Give cautious, practical, locally relevant bee-health advice. "
        "Never recommend Carbaryl, Sevin dust, or unapproved insecticides inside hives."
    )
    if language == "sw":
        system = (
            "Wewe ni BeeCare Anywhere, msaidizi wa wafugaji nyuki wa Kenya anayefanya kazi bila intaneti. "
            "Toa ushauri wa tahadhari, wa vitendo, na unaofaa mazingira ya Kenya. "
            "Usipendekeze Carbaryl, Sevin dust, au dawa zisizoidhinishwa ndani ya mzinga."
        )
    context = (
        f"Country: {row['location_context']['country']}; "
        f"county: {row['location_context'].get('county')}; "
        f"season: {row['location_context']['season_context']}; "
        f"hive_type: {row['hive_context']['hive_type']}."
    )
    return {
        "id": row["record_id"],
        "messages": [
            {"role": "system", "content": system},
            {"role": "user", "content": f"{row['prompt']}\n\n{context}"},
            {"role": "assistant", "content": row["target_answer"]},
        ],
        "metadata": {
            "language": language,
            "primary_condition": row["labels"]["primary_condition"],
            "secondary_conditions": row["labels"]["secondary_conditions"],
            "severity": row["labels"]["severity"],
            "safety_flags": row["review"]["safety_flags"],
            "dataset_status": row["dataset_status"],
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("manifest", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--include-pilot", action="store_true")
    args = parser.parse_args()

    rows = load_rows(args.manifest)
    exported = [to_record(row) for row in rows if should_export(row, args.include_pilot)]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8") as handle:
        for row in exported:
            handle.write(json.dumps(row, ensure_ascii=False) + "\n")
    print(f"Exported {len(exported)} text records to {args.output}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
