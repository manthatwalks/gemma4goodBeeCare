#!/usr/bin/env python3
"""Upload this seed dataset folder to Hugging Face Hub.

Usage:
    HF_TOKEN=hf_... python3 training_data/kenya_bee_health/upload_to_hf.py yahelr1/kenya-bee-health-qa-image-triples
"""

from __future__ import annotations

import argparse
import os
import subprocess
import sys
from pathlib import Path


def ensure_huggingface_hub() -> None:
    try:
        import huggingface_hub  # noqa: F401
    except ImportError as exc:
        raise SystemExit(
            "Missing dependency: huggingface_hub. Install it with:\n"
            "python3 -m pip install huggingface_hub"
        ) from exc


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("repo_id", help="Dataset repo id, for example yahelr1/kenya-bee-health-qa-image-triples")
    parser.add_argument("--private", action="store_true", help="Create/upload as a private dataset repo.")
    parser.add_argument("--commit-message", default="Add Kenya bee health Q/A image triples")
    args = parser.parse_args()

    ensure_huggingface_hub()
    from huggingface_hub import HfApi, get_token

    folder = Path(__file__).resolve().parent
    token = os.environ.get("HF_TOKEN") or os.environ.get("HUGGING_FACE_HUB_TOKEN") or get_token()
    if not token:
        raise SystemExit("Log in with `hf auth login` or set HF_TOKEN before uploading.")

    validation = subprocess.run(
        [sys.executable, str(folder / "download_images.py"), "--validate-only"],
        cwd=folder.parents[1],
        check=False,
    )
    if validation.returncode != 0:
        return validation.returncode

    api = HfApi(token=token)
    api.create_repo(repo_id=args.repo_id, repo_type="dataset", private=args.private, exist_ok=True)
    api.upload_folder(
        repo_id=args.repo_id,
        repo_type="dataset",
        folder_path=str(folder),
        path_in_repo=".",
        commit_message=args.commit_message,
    )
    print(f"Uploaded dataset: https://huggingface.co/datasets/{args.repo_id}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
