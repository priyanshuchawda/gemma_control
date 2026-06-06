"""Local FunctionGemma routing lab for synthetic WhatsApp commands.

This script loads a local FunctionGemma Hugging Face checkpoint once, applies
the model chat template with tool declarations, and prints raw function-call
outputs. It uses fake data only and is intended for prompt/schema tuning before
porting a tool surface to Android LiteRT.
"""

from __future__ import annotations

import argparse
import os
import time
from pathlib import Path

import torch
from transformers import AutoModelForCausalLM, AutoTokenizer
from transformers.utils import logging as transformers_logging


REPO_ROOT = Path(__file__).resolve().parents[2]
BASE_FUNCTIONGEMMA = REPO_ROOT / "models" / "functiongemma-270m-it"
MOBILE_ACTIONS_FUNCTIONGEMMA = (
    REPO_ROOT / "models" / "FunctionGemma_270M_Mobile_Actions"
)


WHATSAPP_TOOLS = [
    {
        "type": "function",
        "function": {
            "name": "read_whatsapp_notifications",
            "description": (
                "Read unread WhatsApp notification messages currently visible "
                "to Android notification listener. Use for latest, new, unread, "
                "recent messages unless user explicitly asks stored history."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "chat": {
                        "type": "string",
                        "description": "Optional exact contact or group name.",
                    },
                    "limit": {
                        "type": "integer",
                        "description": "Maximum messages to read. Default 20.",
                    },
                },
                "required": [],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "summarize_whatsapp_notifications",
            "description": (
                "Summarize unread WhatsApp notification messages currently "
                "visible to Android notification listener. Use for summarize, "
                "summary, catch me up, gist, what happened, too many messages, "
                "or overview unless user explicitly asks stored history."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "chat": {
                        "type": "string",
                        "description": "Optional exact contact or group name.",
                    },
                    "limit": {
                        "type": "integer",
                        "description": "Maximum messages to summarize. Default 50.",
                    },
                },
                "required": [],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "search_whatsapp_history",
            "description": (
                "Search stored WhatsApp message index/history. Use only when "
                "user says old, saved, stored, history, previous, yesterday, "
                "last week, or search."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {"type": "string", "description": "Semantic query."},
                    "chat": {"type": "string", "description": "Optional chat name."},
                    "limit": {"type": "integer", "description": "Maximum results."},
                },
                "required": ["query"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "draft_whatsapp_reply",
            "description": (
                "Prepare a WhatsApp reply draft. This function never sends "
                "immediately. Use when the user asks to reply or message a "
                "contact and both contact/chat and reply text are known."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "chat": {
                        "type": "string",
                        "description": "Exact contact or group name to reply to.",
                    },
                    "message": {"type": "string", "description": "Reply text."},
                },
                "required": ["chat", "message"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "ask_clarification",
            "description": (
                "Ask one short clarification question when action, contact, "
                "or reply text is missing or ambiguous."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "question": {
                        "type": "string",
                        "description": "Question to ask the user.",
                    }
                },
                "required": ["question"],
            },
        },
    },
]


CASES = [
    "Read my latest WhatsApp messages.",
    "Read messages from Mom.",
    "Summarize my WhatsApp messages.",
    "Catch me up on messages from Mom.",
    "What happened in WhatsApp?",
    "Search old messages about the meeting.",
    "Reply to Mom on WhatsApp that I am going for a meeting.",
    "Message him that I am late.",
    "Can you send her ok?",
]


DEVELOPER_PROMPT = """Current date and time given in YYYY-MM-DDTHH:MM:SS format: 2026-06-06T15:30:00
Day of week is Saturday
You are a model that can do function calling with the following functions.
Routing rules:
- latest/new/unread/recent WhatsApp messages => read_whatsapp_notifications.
- summarize/summary/catch me up/gist/what happened/overview/too many messages => summarize_whatsapp_notifications.
- old/saved/stored/history/previous/yesterday/search => search_whatsapp_history.
- reply/message/send text to a named contact => draft_whatsapp_reply. It never sends.
- pronouns like him/her/them without a named contact => ask_clarification.
Return only one function call."""


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--model",
        choices=["base", "mobile-actions"],
        default="base",
        help="Local checkpoint to test.",
    )
    parser.add_argument(
        "--command",
        action="append",
        help="Custom command to test. Repeat for multiple commands.",
    )
    parser.add_argument("--max-new-tokens", type=int, default=96)
    return parser.parse_args()


def load_model(model_dir: Path):
    os.environ.setdefault("HF_HUB_DISABLE_PROGRESS_BARS", "1")
    os.environ.setdefault("TRANSFORMERS_VERBOSITY", "error")
    transformers_logging.set_verbosity_error()
    transformers_logging.disable_progress_bar()
    tokenizer = AutoTokenizer.from_pretrained(model_dir, local_files_only=True)
    model = AutoModelForCausalLM.from_pretrained(
        model_dir,
        local_files_only=True,
        torch_dtype=torch.float32,
        attn_implementation="eager",
    )
    model.eval()
    return tokenizer, model


def generate(tokenizer, model, command: str, max_new_tokens: int) -> str:
    messages = [
        {"role": "developer", "content": DEVELOPER_PROMPT},
        {"role": "user", "content": command},
    ]
    prompt = tokenizer.apply_chat_template(
        messages,
        tools=WHATSAPP_TOOLS,
        tokenize=False,
        add_generation_prompt=True,
    )
    inputs = tokenizer(prompt, return_tensors="pt")
    with torch.no_grad():
        output = model.generate(
            **inputs,
            max_new_tokens=max_new_tokens,
            do_sample=False,
            pad_token_id=tokenizer.eos_token_id,
        )
    new_tokens = output[0][inputs["input_ids"].shape[-1] :]
    return tokenizer.decode(new_tokens, skip_special_tokens=False).strip()


def main() -> None:
    args = parse_args()
    model_dir = (
        BASE_FUNCTIONGEMMA
        if args.model == "base"
        else MOBILE_ACTIONS_FUNCTIONGEMMA
    )
    commands = args.command if args.command else CASES

    start = time.time()
    print(f"Loading {model_dir}")
    tokenizer, model = load_model(model_dir)
    print(f"Loaded in {time.time() - start:.1f}s")

    for command in commands:
        start = time.time()
        output = generate(tokenizer, model, command, args.max_new_tokens)
        print("\n---")
        print(f"USER: {command}")
        print(f"TIME: {time.time() - start:.1f}s")
        print(output)


if __name__ == "__main__":
    main()
