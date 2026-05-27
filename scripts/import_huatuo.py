#!/usr/bin/env python3
"""Import Huatuo26M subsets into the health assistant knowledge base.

Usage:
    python scripts/import_huatuo.py --max-items 5000
    python scripts/import_huatuo.py --subsets kg,encyclopedia --dry-run

The backend must be running at --api-base (default http://localhost:8080).
"""

import argparse
import json
import os
import sys
import time
from typing import Optional

import requests

# ── supported subsets ──────────────────────────────────────────────
SUBSETS = {
    "kg": {
        "hf_path": "FreedomIntelligence/huatuo_knowledge_graph_qa",
        "source_name": "huatuo_knowledge_graph",
        "evidence_level": 3,
        "doc_type": "medical_qa",
    },
    "encyclopedia": {
        "hf_path": "FreedomIntelligence/huatuo_encyclopedia_qa",
        "source_name": "huatuo_encyclopedia",
        "evidence_level": 2,
        "doc_type": "health_encyclopedia",
    },
    "consultation": {
        "hf_path": "FreedomIntelligence/huatuo_consultation_qa",
        "source_name": "huatuo_consultation",
        "evidence_level": 1,
        "doc_type": "medical_qa",
    },
    "lite": {
        "hf_path": "FreedomIntelligence/Huatuo26M-Lite",
        "source_name": "huatuo26m_lite",
        "evidence_level": 2,
        "doc_type": "medical_qa",
    },
}


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Import Huatuo26M into health assistant")
    p.add_argument("--api-base", default="http://localhost:8080",
                   help="Backend base URL (default: http://localhost:8080)")
    p.add_argument("--max-items", type=int, default=20000,
                   help="Total items to import across all subsets (default: 20000)")
    p.add_argument("--subsets", default="kg,encyclopedia,lite",
                   help="Comma-separated subset keys: kg,encyclopedia,consultation,lite")
    p.add_argument("--batch-size", type=int, default=100,
                   help="Articles per HTTP batch (default: 100)")
    p.add_argument("--admin-user", default="admin",
                   help="Admin username (default: admin)")
    p.add_argument("--admin-pass", default=None,
                   help="Admin password (reads ADMIN_PASSWORD env var if not set)")
    p.add_argument("--dry-run", action="store_true",
                   help="Preview only — do not send data")
    p.add_argument("--hf-mirror", default="https://huggingface.co",
                   help="HuggingFace mirror endpoint")
    return p.parse_args()


def set_hf_mirror(mirror: str) -> None:
    """Point HuggingFace datasets to a mirror for faster downloads in China."""
    os.environ.setdefault("HF_ENDPOINT", mirror)
    print(f"[mirror] HF_ENDPOINT = {mirror}")


def get_token(api_base: str, username: str, password: str) -> str:
    """Log in and return a JWT access token."""
    resp = requests.post(
        f"{api_base}/api/v1/auth/login",
        json={"username": username, "password": password},
        timeout=10,
    )
    resp.raise_for_status()
    body = resp.json()
    token = body["data"]["accessToken"]
    print(f"[auth] logged in as {username}")
    return token


def load_subset(key: str, info: dict, max_items: int) -> list[dict]:
    """Load a HuggingFace dataset subset and return article dicts."""
    from datasets import load_dataset

    print(f"[load] {key} ← {info['hf_path']} (max {max_items})")
    try:
        ds = load_dataset(info["hf_path"], split="train", streaming=True)
    except Exception:
        try:
            ds = load_dataset(info["hf_path"], streaming=True)
            if isinstance(ds, dict):
                ds = ds.get("train", list(ds.values())[0])
        except Exception:
            print(f"[warn] {key}: failed to load, skipping")
            return []

    articles: list[dict] = []
    for row in ds:
        q = row.get("questions") or row.get("question") or ""
        a = row.get("answers") or row.get("answer") or ""

        # Handle list-type fields (kg subset returns lists)
        if isinstance(q, list):
            q = "；".join(str(x) for x in q)
        if isinstance(a, list):
            a = "；".join(str(x) for x in a)

        q = q.strip()
        a = a.strip()
        if not q or not a:
            continue

        # Enrich content with metadata when available
        extra_fields: list[str] = []
        for f in ("department", "disease", "label", "related_diseases"):
            v = row.get(f)
            if v and str(v).strip():
                extra_fields.append(f"{f}: {str(v).strip()}")
        prefix = ("；".join(extra_fields) + "\n") if extra_fields else ""
        content = prefix + a

        articles.append({
            "title": q[:500],
            "content": content,
            "sourceUrl": "",
            "publicationDate": None,
        })

        if len(articles) >= max_items:
            break

    print(f"[load] {key}: {len(articles)} valid articles")
    return articles


def post_batch(api_base: str, token: str, batch: list[dict],
               source_name: str, doc_type: str,
               evidence_level: int) -> bool:
    """POST one batch to the import API. Returns True on success."""
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
    }
    payload = {
        "sourceName": source_name,
        "documentType": doc_type,
        "evidenceLevel": evidence_level,
        "articles": batch,
    }

    for attempt in range(3):
        try:
            resp = requests.post(
                f"{api_base}/api/v1/admin/knowledge/import/batch",
                json=payload,
                headers=headers,
                timeout=120,
            )
            if resp.status_code == 200:
                return True
            print(f"  [warn] HTTP {resp.status_code}: {resp.text[:200]}")
        except requests.RequestException as e:
            print(f"  [warn] attempt {attempt + 1} failed: {e}")
            time.sleep(2)

    return False


def progress_bar(done: int, total: int) -> str:
    width = 30
    pct = done / total if total else 0
    filled = int(width * pct)
    return f"[{'=' * filled}{' ' * (width - filled)}] {pct:.0%} ({done}/{total})"


def main() -> None:
    args = parse_args()
    set_hf_mirror(args.hf_mirror)

    # Resolve admin password
    password = args.admin_pass or os.getenv("ADMIN_PASSWORD")
    if not password:
        password = input("Admin password: ").strip()
    if not password:
        print("[err] No admin password provided", file=sys.stderr)
        sys.exit(1)

    subset_keys = [k.strip() for k in args.subsets.split(",") if k.strip() in SUBSETS]
    if not subset_keys:
        print(f"[err] No valid subsets. Choose from: {list(SUBSETS)}", file=sys.stderr)
        sys.exit(1)

    per_subset = args.max_items // len(subset_keys)
    print(f"[plan] subsets={subset_keys}, total max={args.max_items}, "
          f"per-subset max={per_subset}, batch={args.batch_size}")

    if not args.dry_run:
        token = get_token(args.api_base, args.admin_user, password)

    grand_total = 0
    grand_success = 0
    for key in subset_keys:
        info = SUBSETS[key]
        articles = load_subset(key, info, per_subset)
        if not articles:
            continue

        if args.dry_run:
            print(f"[dry-run] would import {len(articles)} articles from {key}")
            grand_total += len(articles)
            continue

        total = len(articles)
        success = 0
        t0 = time.time()
        for i in range(0, total, args.batch_size):
            batch = articles[i : i + args.batch_size]
            ok = post_batch(args.api_base, token, batch,
                            info["source_name"], info["doc_type"],
                            info["evidence_level"])
            if ok:
                success += len(batch)

            elapsed = time.time() - t0
            rate = (i + len(batch)) / elapsed if elapsed > 0 else 0
            bar = progress_bar(i + len(batch), total)
            eta = (total - i - len(batch)) / rate if rate > 0 else 0
            print(f"\r  {key}: {bar}  {rate:.0f} q/s  ETA {eta:.0f}s", end="")

        print()  # newline after progress bar
        pct = success / total * 100 if total else 0
        print(f"  {key}: {success}/{total} ({pct:.1f}%) succeeded")
        grand_total += total
        grand_success += success

    print(f"\n[done] {grand_success}/{grand_total} articles imported")


if __name__ == "__main__":
    main()
