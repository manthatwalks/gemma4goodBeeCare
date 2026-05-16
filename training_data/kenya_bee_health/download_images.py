#!/usr/bin/env python3
"""Download and validate the Kenya bee-health seed image triples."""

from __future__ import annotations

import argparse
import json
import ssl
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path


REQUIRED_FIELDS = (
    "id",
    "condition_label",
    "question",
    "answer",
    "picture",
    "image_source_url",
    "image_license",
    "attribution",
)


def load_rows(dataset_path: Path) -> list[dict]:
    rows: list[dict] = []
    with dataset_path.open("r", encoding="utf-8") as handle:
        for line_number, line in enumerate(handle, start=1):
            if not line.strip():
                continue
            try:
                row = json.loads(line)
            except json.JSONDecodeError as exc:
                raise SystemExit(f"{dataset_path}:{line_number}: invalid JSON: {exc}") from exc
            missing = [field for field in REQUIRED_FIELDS if not row.get(field)]
            if missing:
                raise SystemExit(f"{dataset_path}:{line_number}: missing fields: {', '.join(missing)}")
            rows.append(row)
    return rows


def validate(rows: list[dict], repo_root: Path, require_images: bool) -> None:
    seen_ids: set[str] = set()
    seen_questions: set[str] = set()
    for row in rows:
        row_id = row["id"]
        question = row["question"]
        if row_id in seen_ids:
            raise SystemExit(f"duplicate id: {row_id}")
        if question in seen_questions:
            raise SystemExit(f"duplicate question: {question}")
        seen_ids.add(row_id)
        seen_questions.add(question)
        if require_images:
            image_path = repo_root / row["picture"]
            if not image_path.exists() or image_path.stat().st_size == 0:
                raise SystemExit(f"missing downloaded image for {row_id}: {image_path}")
    print(f"Validated {len(rows)} dataset rows.")


def ssl_context(insecure: bool) -> ssl.SSLContext | None:
    if insecure:
        return ssl._create_unverified_context()
    try:
        import certifi
    except ImportError:
        return None
    return ssl.create_default_context(cafile=certifi.where())


def thumbnail_url(url: str, width: int = 1200) -> str:
    if "commons.wikimedia.org/wiki/Special:Redirect/file/" not in url:
        return url
    separator = "&" if "?" in url else "?"
    if "width=" in url:
        return url
    return f"{url}{separator}width={width}"


def download(url: str, destination: Path, insecure: bool, retries: int = 3) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(
        thumbnail_url(url),
        headers={
            "User-Agent": "BeeCareAnywhereDatasetBuilder/0.1 (license-aware educational dataset)"
        },
    )
    for attempt in range(1, retries + 1):
        try:
            with urllib.request.urlopen(request, timeout=60, context=ssl_context(insecure)) as response:
                data = response.read()
            break
        except urllib.error.HTTPError as exc:
            if exc.code == 429 and attempt < retries:
                wait_seconds = 12 * attempt
                print(f"Rate limited; waiting {wait_seconds}s before retry {attempt + 1}/{retries}")
                time.sleep(wait_seconds)
                continue
            raise RuntimeError(f"download failed: {url}: {exc}") from exc
        except urllib.error.URLError as exc:
            raise RuntimeError(f"download failed: {url}: {exc}") from exc
    else:
        raise RuntimeError(f"download failed after {retries} attempts: {url}")
    if len(data) < 1024:
        raise RuntimeError(f"download looked too small ({len(data)} bytes): {url}")
    destination.write_bytes(data)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--validate-only", action="store_true", help="Validate JSONL and existing local images.")
    parser.add_argument("--force", action="store_true", help="Redownload images even when files already exist.")
    parser.add_argument("--insecure", action="store_true", help="Disable TLS verification for seed-image downloads.")
    args = parser.parse_args()

    folder = Path(__file__).resolve().parent
    repo_root = folder.parents[1]
    dataset_path = folder / "bee_health_qa_image_triples.seed.jsonl"
    rows = load_rows(dataset_path)

    if args.validate_only:
        validate(rows, repo_root, require_images=True)
        return 0

    downloaded = 0
    skipped = 0
    unique_images: dict[str, str] = {}
    for row in rows:
        unique_images[row["picture"]] = row.get("image_download_url", "")

    for picture, url in sorted(unique_images.items()):
        if not url:
            print(f"Skipping {picture}: no download URL")
            skipped += 1
            continue
        destination = repo_root / picture
        if destination.exists() and destination.stat().st_size > 0 and not args.force:
            print(f"Already present: {destination}")
            skipped += 1
            continue
        print(f"Downloading {destination.name}")
        download(url, destination, args.insecure)
        time.sleep(2)
        downloaded += 1

    validate(rows, repo_root, require_images=True)
    print(f"Downloaded {downloaded} image(s); skipped {skipped}.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
