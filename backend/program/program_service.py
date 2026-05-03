from __future__ import annotations

import sys
from pathlib import Path
from typing import Any, Dict

ROOT_DIR = Path(__file__).resolve().parents[1]
sys.path.append(str(ROOT_DIR))

from control.control_service import handle_feature


def classify_intent(text: str) -> str:
    lower = text.lower()
    if any(word in lower for word in ["market", "price", "performance", "nft"]):
        return "market"
    if any(word in lower for word in ["balance", "wallet", "sol", "spl"]):
        return "wallet"
    if any(word in lower for word in ["pay", "payment", "invoice", "send"]):
        return "payment"
    if any(word in lower for word in ["profile", "account", "settings"]):
        return "profile"
    if any(word in lower for word in ["voice", "speak", "listen", "mic"]):
        return "voice"
    return "ai"


def handle_command(text: str) -> Dict[str, Any]:
    intent = classify_intent(text)
    result = handle_feature(intent)
    return {
        "input": text,
        "intent": intent,
        "result": result,
    }
