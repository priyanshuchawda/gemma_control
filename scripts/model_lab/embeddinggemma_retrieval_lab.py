"""Local EmbeddingGemma retrieval lab for synthetic WhatsApp messages.

This mirrors the local SentenceTransformer module layout without installing
sentence-transformers: Gemma backbone, mean pooling, two dense layers, normalize.
"""

from __future__ import annotations

import os
import time
from pathlib import Path

import torch
import torch.nn.functional as F
from safetensors.torch import load_file
from transformers import AutoModel, AutoTokenizer
from transformers.utils import logging as transformers_logging


REPO_ROOT = Path(__file__).resolve().parents[2]
MODEL_DIR = REPO_ROOT / "models" / "embeddinggemma-300m"

DOCUMENTS = [
    "Mom: Dinner is ready. Come home by 8 pm.",
    "Office Group: Standup moved to 10:30 and client demo at 4.",
    "Bank: INR 5,000 credited to your account ending 2211.",
    "Boss: Please send the presentation before the meeting.",
    "Friend: Movie tonight? I booked two seats.",
]

QUERIES = [
    "meeting time and office updates",
    "money or bank payment messages",
    "family dinner messages from mom",
    "urgent task from boss",
]


def load_model():
    os.environ.setdefault("HF_HUB_DISABLE_PROGRESS_BARS", "1")
    os.environ.setdefault("TRANSFORMERS_VERBOSITY", "error")
    transformers_logging.set_verbosity_error()
    transformers_logging.disable_progress_bar()
    tokenizer = AutoTokenizer.from_pretrained(MODEL_DIR, local_files_only=True)
    model = AutoModel.from_pretrained(
        MODEL_DIR,
        local_files_only=True,
        torch_dtype=torch.float32,
        attn_implementation="eager",
    )
    model.eval()
    dense_1 = load_file(MODEL_DIR / "2_Dense" / "model.safetensors")[
        "linear.weight"
    ]
    dense_2 = load_file(MODEL_DIR / "3_Dense" / "model.safetensors")[
        "linear.weight"
    ]
    return tokenizer, model, dense_1, dense_2


def encode(tokenizer, model, dense_1, dense_2, texts: list[str], kind: str):
    prefix = (
        "task: search result | query: "
        if kind == "query"
        else "title: none | text: "
    )
    batch = tokenizer(
        [prefix + text for text in texts],
        padding=True,
        truncation=True,
        max_length=512,
        return_tensors="pt",
    )
    with torch.no_grad():
        hidden = model(**batch).last_hidden_state
        mask = batch["attention_mask"].unsqueeze(-1).to(hidden.dtype)
        pooled = (hidden * mask).sum(dim=1) / mask.sum(dim=1).clamp(min=1)
        projected = F.linear(F.linear(pooled, dense_1), dense_2)
        return F.normalize(projected, p=2, dim=1)


def main() -> None:
    start = time.time()
    print(f"Loading {MODEL_DIR}")
    tokenizer, model, dense_1, dense_2 = load_model()
    print(f"Loaded in {time.time() - start:.1f}s")

    documents = encode(tokenizer, model, dense_1, dense_2, DOCUMENTS, "document")
    queries = encode(tokenizer, model, dense_1, dense_2, QUERIES, "query")
    scores = queries @ documents.T

    for index, query in enumerate(QUERIES):
        ranked = torch.argsort(scores[index], descending=True).tolist()
        print("\n---")
        print(f"QUERY: {query}")
        for doc_index in ranked[:3]:
            print(f"{scores[index, doc_index].item():.3f}  {DOCUMENTS[doc_index]}")


if __name__ == "__main__":
    main()
