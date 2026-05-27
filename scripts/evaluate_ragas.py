#!/usr/bin/env python3
"""RAGAS evaluation script for the health assistant RAG pipeline.

This script is called by the Java backend (EvaluationService) as a subprocess
when an admin triggers an evaluation run. It can also be run manually:

    python scripts/evaluate_ragas.py --num-questions 10 --api-base http://localhost:8080

Requirements: pip install ragas datasets requests
"""

import argparse
import json
import os
import sys
import time
from typing import Optional

import requests

# ── CLI ────────────────────────────────────────────────────────────
def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="RAGAS evaluation for health assistant")
    p.add_argument("--api-base", default="http://localhost:8080",
                   help="Backend base URL")
    p.add_argument("--num-questions", type=int, default=100,
                   help="Number of test questions to evaluate (default: 100)")
    p.add_argument("--run-id", default=None,
                   help="Evaluation run ID (assigned by backend)")
    p.add_argument("--hf-mirror", default="https://huggingface.co",
                   help="HuggingFace mirror endpoint (set HF_ENDPOINT env var)")
    p.add_argument("--delay", type=float, default=0.5,
                   help="Delay between questions in seconds (default: 0.5)")
    return p.parse_args()


# ── helpers ─────────────────────────────────────────────────────────
def set_hf_mirror(mirror: str) -> None:
    os.environ.setdefault("HF_ENDPOINT", mirror)
    print(f"[mirror] HF_ENDPOINT = {mirror}")


def get_token(api_base: str) -> str:
    """Admin login. Credentials: admin / (ADMIN_PASSWORD env or 'admin123')."""
    password = os.getenv("ADMIN_PASSWORD", "admin123")
    resp = requests.post(
        f"{api_base}/api/v1/auth/login",
        json={"username": "admin", "password": password},
        timeout=10,
    )
    resp.raise_for_status()
    return resp.json()["data"]["accessToken"]


def generate_answer(api_base: str, token: str, question: str) -> dict:
    """Call the sync evaluation endpoint to get answer + contexts."""
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    resp = requests.post(
        f"{api_base}/api/v1/admin/evaluation/generate",
        json={"question": question},
        headers=headers,
        timeout=120,
    )
    resp.raise_for_status()
    return resp.json()["data"]


def submit_results(api_base: str, token: str, run_id: str,
                   metrics: dict, details: list) -> None:
    """POST computed RAGAS metrics back to the backend."""
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    payload = {
        "runId": run_id,
        "avgFaithfulness": metrics.get("faithfulness"),
        "avgAnswerRelevancy": metrics.get("answer_relevancy"),
        "avgContextPrecision": metrics.get("context_precision"),
        "avgContextRecall": metrics.get("context_recall"),
        "avgFactualCorrectness": metrics.get("factual_correctness"),
        "details": details,
    }
    resp = requests.post(
        f"{api_base}/api/v1/admin/evaluation/runs/{run_id}/results",
        json=payload,
        headers=headers,
        timeout=30,
    )
    resp.raise_for_status()
    print(f"\n[submit] Results posted for run {run_id}")


# ── main ────────────────────────────────────────────────────────────
def main() -> None:
    args = parse_args()
    set_hf_mirror(args.hf_mirror)

    # 1. Load test data
    print(f"[load] Loading huatuo26M-testdatasets...")
    from datasets import load_dataset

    try:
        ds = load_dataset("FreedomIntelligence/huatuo26M-testdatasets", split="test")
    except Exception:
        ds = load_dataset("FreedomIntelligence/huatuo26M-testdatasets")
        if isinstance(ds, dict):
            ds = ds["test"] if "test" in ds else ds["train"]

    n = min(args.num_questions, len(ds))
    print(f"[load] Using {n}/{len(ds)} test questions")

    # 2. Authenticate
    token = get_token(args.api_base)

    # 3. Collect QA pairs
    questions: list[str] = []
    answers: list[str] = []
    contexts_list: list[list[str]] = []
    ground_truths: list[str] = []
    failed = 0

    t0 = time.time()
    for i in range(n):
        row = ds[i]
        question = (row.get("questions") or row.get("question") or "").strip()
        ground_truth = (row.get("answers") or row.get("answer") or "").strip()
        if not question or not ground_truth:
            continue

        try:
            result = generate_answer(args.api_base, token, question)
            answer = result.get("answer", "")
            contexts = result.get("contexts", [])
        except Exception as e:
            print(f"\n  [err] Q{i}: {e}")
            answer = "[生成失败]"
            contexts = []
            failed += 1

        questions.append(question)
        answers.append(answer)
        contexts_list.append(contexts)
        ground_truths.append(ground_truth)

        elapsed = time.time() - t0
        rate = (i + 1) / elapsed if elapsed > 0 else 0
        print(f"\r  [{i + 1}/{n}] rate={rate:.1f} q/s, failed={failed}", end="")
        time.sleep(args.delay)

    print(f"\n[collect] {len(questions)} questions, {failed} failed generations")

    # 4. Compute RAGAS metrics
    run_id = args.run_id or f"eval-{int(time.time())}"
    metrics: dict[str, Optional[float]] = {}
    details: list[dict] = []

    # RAGAS depends on langchain_community.chat_models.vertexai which was removed
    # in newer langchain-community versions. Stub it out before import.
    import sys as _sys, types as _types
    _stub = _types.ModuleType('langchain_community.chat_models.vertexai')
    # Create a dummy ChatVertexAI that RAGAS can reference but we won't actually use
    _stub.ChatVertexAI = type('ChatVertexAI', (), {'__module__': 'langchain_community.chat_models.vertexai'})
    _sys.modules['langchain_community.chat_models.vertexai'] = _stub

    try:
        from ragas import evaluate, EvaluationDataset
        from ragas.metrics import (
            Faithfulness,
            AnswerRelevancy,
            ContextPrecision,
            ContextRecall,
        )
        from ragas.llms import LangchainLLMWrapper
        from ragas.embeddings import LangchainEmbeddingsWrapper
        from langchain_openai import ChatOpenAI
        from langchain_core.embeddings import Embeddings

        # Helper to read DASHSCOPE_API_KEY from env or .env
        def get_dashscope_key() -> str:
            key = os.getenv("DASHSCOPE_API_KEY", "")
            if key:
                return key
            from pathlib import Path
            env_file = Path(__file__).resolve().parent.parent / ".env"
            if env_file.exists():
                with open(env_file, encoding="utf-8") as f:
                    for line in f:
                        line = line.strip()
                        if line.startswith("DASHSCOPE_API_KEY="):
                            return line.split("=", 1)[1].strip()
            return ""

        api_key = get_dashscope_key()
        dashscope_base = "https://dashscope.aliyuncs.com/compatible-mode/v1"

        # LLM judge
        judge_llm = LangchainLLMWrapper(ChatOpenAI(
            model="qwen-max",
            openai_api_base=dashscope_base,
            openai_api_key=api_key,
            temperature=0,
        ))

        # Custom DashScope embedding wrapper compatible with RAGAS
        class DashScopeEmbeddings(Embeddings):
            def embed_documents(self, texts: list[str]) -> list[list[float]]:
                result = []
                for text in texts:
                    resp = requests.post(
                        f"{dashscope_base}/embeddings",
                        headers={"Authorization": f"Bearer {api_key}",
                                 "Content-Type": "application/json"},
                        json={"model": "text-embedding-v3", "input": text},
                        timeout=30,
                    )
                    data = resp.json()
                    if "data" in data and len(data["data"]) > 0:
                        result.append(data["data"][0]["embedding"])
                    else:
                        result.append([0.0] * 1024)
                return result

            def embed_query(self, text: str) -> list[float]:
                return self.embed_documents([text])[0]

        judge_embeddings = LangchainEmbeddingsWrapper(DashScopeEmbeddings())

        # Build RAGAS dataset (0.2.x format: list of dicts)
        samples = []
        for i in range(len(questions)):
            samples.append({
                "user_input": questions[i],
                "response": answers[i],
                "retrieved_contexts": contexts_list[i],
                "reference": ground_truths[i],
            })
        eval_dataset = EvaluationDataset.from_dict(samples)

        # Select metrics to compute
        metric_list = [
            Faithfulness(),
            AnswerRelevancy(),
            ContextPrecision(),
            ContextRecall(),
        ]

        print(f"[ragas] Computing {len(metric_list)} metrics on {len(questions)} samples...")
        result = evaluate(eval_dataset, metrics=metric_list, llm=judge_llm, embeddings=judge_embeddings)

        # Extract results via pandas DataFrame (works across RAGAS versions)
        result_df = result.to_pandas()
        metric_names = ["faithfulness", "answer_relevancy", "context_precision",
                        "context_recall"]
        for col in metric_names:
            if col in result_df.columns:
                vals = result_df[col].dropna()
                if len(vals) > 0:
                    metrics[col] = float(vals.mean())
                    print(f"  {col}: {metrics[col]:.4f}")

        # Build per-question details
        for i in range(len(questions)):
            detail = {
                "index": i,
                "question": questions[i][:200],
                "answer": answers[i][:300],
                "contexts_count": len(contexts_list[i]),
            }
            for col in result_df.columns:
                if col not in ("question", "answer", "contexts", "ground_truth",
                               "user_input", "response", "retrieved_contexts", "reference"):
                    val = result_df.iloc[i][col]
                    if hasattr(val, "item"):
                        val = val.item()
                    if not isinstance(val, (int, float, str, bool, type(None))):
                        continue
                    detail[col] = val
            details.append(detail)

    except ImportError as e:
        print(f"[warn] RAGAS not available: {e}")
        print("[fallback] Computing simple overlap metrics instead")
        metrics = compute_simple_metrics(questions, answers, contexts_list, ground_truths)
        details = [{"index": i, "question": q[:200]} for i, q in enumerate(questions)]

    # 5. Clean NaN values and save local report
    import math
    def clean_nan(obj):
        if isinstance(obj, dict):
            return {k: clean_nan(v) for k, v in obj.items()}
        if isinstance(obj, list):
            return [clean_nan(v) for v in obj]
        if isinstance(obj, float) and math.isnan(obj):
            return None
        return obj

    metrics = clean_nan(metrics)
    details = clean_nan(details)

    report = {
        "run_id": run_id,
        "num_questions": len(questions),
        "failed": failed,
        "metrics": metrics,
        "details": details,
    }
    report_path = os.path.join(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
        "evaluation_report.json",
    )
    with open(report_path, "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2, default=str)
    print(f"[save] Local report: {report_path}")

    # 6. Submit to backend
    try:
        submit_results(args.api_base, token, run_id, metrics, details)
    except Exception as e:
        print(f"[err] Failed to submit results: {e}")
        print("[info] Results saved locally, retry manually if needed")

    print(f"\n[done] Evaluation run {run_id} complete")


def compute_simple_metrics(
    questions: list[str],
    answers: list[str],
    contexts_list: list[list[str]],
    ground_truths: list[str],
) -> dict[str, Optional[float]]:
    """Fallback: compute simple lexical overlap and retrieval coverage metrics."""
    import re

    total = len(questions)
    if total == 0:
        return {}

    total_context_hits = 0
    total_answer_overlap = 0.0

    for i in range(total):
        # Simple retrieval coverage: does any context contain keywords from ground truth?
        gt_words = set(re.findall(r"[一-鿿\w]+", ground_truths[i]))
        for ctx in contexts_list[i]:
            ctx_words = set(re.findall(r"[一-鿿\w]+", ctx))
            if gt_words & ctx_words:
                total_context_hits += 1
                break

        # Simple answer relevance: word overlap between answer and ground truth
        ans_words = set(re.findall(r"[一-鿿\w]+", answers[i]))
        if gt_words:
            overlap = len(ans_words & gt_words) / len(gt_words)
            total_answer_overlap += overlap

    return {
        "faithfulness": None,
        "answer_relevancy": round(total_answer_overlap / total, 4) if total else None,
        "context_precision": round(total_context_hits / total, 4) if total else None,
        "context_recall": None,
        "factual_correctness": None,
    }


if __name__ == "__main__":
    main()
