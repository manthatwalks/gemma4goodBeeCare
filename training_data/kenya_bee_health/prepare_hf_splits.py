#!/usr/bin/env python3
"""Build Hugging Face ImageFolder-style train/validation/test splits."""

from __future__ import annotations

import json
import shutil
import sys
from pathlib import Path


SPLITS = {
    "train": [
        "kbht_0001",
        "kbht_0002",
        "kbht_0003",
        "kbht_0004",
        "kbht_0005",
        "kbht_0007",
        "kbht_0008",
        "kbht_0009",
        "kbht_0010",
        "kbht_0011",
        "kbht_0013",
        "kbht_0014",
        "kbht_0016",
        "kbht_0017",
        "kbht_0018",
        "kbht_0020",
        "kbht_0021",
        "kbht_0022",
        "kbht_0023",
        "kbht_0024",
    ],
    "validation": ["kbht_0006", "kbht_0012"],
    "test": ["kbht_0015", "kbht_0019"],
}


def load_rows(dataset_path: Path) -> list[dict]:
    with dataset_path.open("r", encoding="utf-8") as handle:
        return [json.loads(line) for line in handle if line.strip()]


def main() -> int:
    folder = Path(__file__).resolve().parent
    rows_by_id = {row["id"]: row for row in load_rows(folder / "bee_health_qa_image_triples.seed.jsonl")}
    assigned = {row_id for row_ids in SPLITS.values() for row_id in row_ids}
    if assigned != set(rows_by_id):
        missing = sorted(set(rows_by_id) - assigned)
        unknown = sorted(assigned - set(rows_by_id))
        raise SystemExit(f"Split mismatch. Missing={missing}; unknown={unknown}")

    for split, row_ids in SPLITS.items():
        split_dir = folder / split
        images_dir = split_dir / "images"
        images_dir.mkdir(parents=True, exist_ok=True)
        metadata_path = split_dir / "metadata.jsonl"
        with metadata_path.open("w", encoding="utf-8") as output:
            for row_id in row_ids:
                row = rows_by_id[row_id]
                source_image = folder.parents[1] / row["picture"]
                target_name = f"{row_id}_{source_image.name}"
                target_image = images_dir / target_name
                shutil.copy2(source_image, target_image)
                metadata = {
                    "file_name": f"images/{target_name}",
                    "id": row["id"],
                    "condition_label": row["condition_label"],
                    "task": row["task"],
                    "question": row["question"],
                    "answer": row["answer"],
                    "image_source_url": row["image_source_url"],
                    "image_license": row["image_license"],
                    "attribution": row["attribution"],
                    "kenya_region_context": row["kenya_region_context"],
                    "safety_tags": row["safety_tags"],
                }
                output.write(json.dumps(metadata, ensure_ascii=False) + "\n")

    print("Prepared Hugging Face splits: train=20, validation=2, test=2")
    return 0


if __name__ == "__main__":
    sys.exit(main())
