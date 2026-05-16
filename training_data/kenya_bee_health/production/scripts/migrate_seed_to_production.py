#!/usr/bin/env python3
"""Migrate the seed Q/A/image triples into the production manifest schema."""

from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path


CONDITION_MAP = {
    "healthy_or_resource_frame": "healthy_resource_frame",
    "bearding_heat_stress": "heat_bearding",
    "wax_moth_webbing": "wax_moth_webbing",
    "wax_moth_damage": "wax_moth_damage",
    "small_hive_beetle": "small_hive_beetle_larvae",
    "safari_ants_siafu": "safari_ants_siafu",
    "deformed_wing_virus_varroa": "varroa_visible",
    "deformed_wing_virus": "deformed_wing_virus",
    "drought_dearth_proxy": "drought_dearth_starvation_risk",
}

TASK_MAP = {
    "visual_triage": "visual_triage",
    "advice": "advice",
    "counterfactual": "counterfactual_safety",
    "differential_diagnosis": "differential_diagnosis",
    "root_cause": "differential_diagnosis",
}

SEVERITY_BY_CONDITION = {
    "healthy_resource_frame": 0,
    "heat_bearding": 1,
    "drought_dearth_starvation_risk": 2,
    "wax_moth_webbing": 2,
    "wax_moth_damage": 2,
    "small_hive_beetle_larvae": 2,
    "safari_ants_siafu": 3,
    "varroa_visible": 2,
    "deformed_wing_virus": 2,
}

SAFETY_FLAG_MAP = {
    "no_treatment_needed": [],
    "monitor": ["collect_more_evidence"],
    "drought": ["collect_more_evidence"],
    "feeding": ["follow_label_and_extension_advice"],
    "harvest_caution": ["food_safety_warning"],
    "baseline": ["single_image_limit"],
    "heat_stress": ["avoid_false_swarm_alarm"],
    "water": [],
    "ventilation": [],
    "shade": [],
    "follow_up_required": ["collect_more_evidence"],
    "absconding_risk": ["urgent_inspection"],
    "wax_moth": ["urgent_inspection"],
    "weak_colony": ["collect_more_evidence"],
    "no_unapproved_insecticide": ["no_unapproved_insecticide_inside_hive"],
    "root_cause": ["collect_more_evidence"],
    "feeding_caution": ["follow_label_and_extension_advice"],
    "comb_management": [],
    "shb": ["food_safety_warning"],
    "differential_diagnosis": ["collect_more_evidence"],
    "food_safety": ["food_safety_warning"],
    "ants": ["urgent_inspection"],
    "physical_barrier": ["avoid_honey_contamination"],
    "contamination_avoidance": ["avoid_honey_contamination"],
    "no_engine_oil_inside_hive": ["avoid_honey_contamination"],
    "inspect_weakness": ["collect_more_evidence"],
    "varroa": ["registered_treatment_only"],
    "dwv": ["registered_treatment_only"],
    "no_carbaryl": ["no_carbaryl"],
    "approved_treatments_only": ["registered_treatment_only"],
    "treatment_label_required": ["follow_label_and_extension_advice"],
    "monitoring": ["collect_more_evidence"],
    "audio_context_needed": ["collect_more_evidence"],
    "counterfactual": ["collect_more_evidence"],
    "avoid_false_swarm_alarm": ["avoid_false_swarm_alarm"],
}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def source_media_id(row: dict) -> str:
    return Path(row["picture"]).stem


def production_safety_flags(seed_tags: list[str]) -> list[str]:
    flags: list[str] = []
    for tag in seed_tags:
        flags.extend(SAFETY_FLAG_MAP.get(tag, ["collect_more_evidence"]))
    return sorted(set(flags))


def main() -> int:
    root = Path(__file__).resolve().parents[4]
    data_dir = root / "training_data/kenya_bee_health"
    source_path = data_dir / "bee_health_qa_image_triples.seed.jsonl"
    output_path = data_dir / "production/manifests/production_seed_manifest.jsonl"
    output_path.parent.mkdir(parents=True, exist_ok=True)

    rows = [json.loads(line) for line in source_path.open("r", encoding="utf-8") if line.strip()]
    with output_path.open("w", encoding="utf-8") as output:
        for row in rows:
            primary = CONDITION_MAP[row["condition_label"]]
            image_path = root / row["picture"]
            record = {
                "record_id": row["id"],
                "dataset_status": "pilot_seed",
                "split": "pilot",
                "modality": "image",
                "media": {
                    "image_path": row["picture"],
                    "audio_path": None,
                    "source_media_id": source_media_id(row),
                    "sha256": sha256(image_path) if image_path.exists() else None,
                },
                "location_context": {
                    "country": "Kenya",
                    "county": None,
                    "region_type": "kenya_context_seed",
                    "season_context": "unknown_or_prompt_context",
                    "collection_date": None,
                },
                "hive_context": {
                    "hive_type": "unknown",
                    "bee_subspecies": "Apis mellifera scutellata target; source image may differ",
                    "frame_removed": None,
                    "inspection_context": row.get("kenya_region_context", ""),
                },
                "labels": {
                    "primary_condition": primary,
                    "secondary_conditions": [],
                    "task": TASK_MAP[row["task"]],
                    "severity": SEVERITY_BY_CONDITION.get(primary, 1),
                    "confidence": "medium",
                    "lab_confirmed": None,
                },
                "prompt": row["question"],
                "target_answer": row["answer"],
                "review": {
                    "reviewer_role": "dataset_curator",
                    "review_status": "single_reviewed",
                    "safety_flags": production_safety_flags(row.get("safety_tags", [])),
                    "notes": "Migrated seed row. Use for pipeline tests, not as final production training evidence.",
                },
                "provenance": {
                    "source_type": "public_seed_image",
                    "source_url": row.get("image_source_url"),
                    "license": row.get("image_license", "unknown"),
                    "attribution": row.get("attribution", ""),
                    "consent_status": "public_license_or_source_terms",
                },
            }
            output.write(json.dumps(record, ensure_ascii=False) + "\n")

    print(f"Wrote {len(rows)} production seed records to {output_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
