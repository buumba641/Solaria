from __future__ import annotations

import json
import os
import random
import sys
from datetime import date
from pathlib import Path
from typing import Any, Dict, List, Optional

BASE_DIR = Path(__file__).resolve().parent
BACKEND_DIR = BASE_DIR.parent
if str(BACKEND_DIR) not in sys.path:
    sys.path.append(str(BACKEND_DIR))

from market.market_service import get_market_snapshot
from voice.voice_service import synthesize
FIXTURE_DIR = BASE_DIR / "fixtures"

FEATURES: Dict[str, Dict[str, str]] = {
    "ai": {"label": "AI bot", "fixture": "ai_bot.json", "env": "AI_ENABLED"},
    "market": {"label": "market analysis", "fixture": "market_analysis.json", "env": "MARKET_ENABLED"},
    "wallet": {"label": "wallet", "fixture": "wallet.json", "env": "WALLET_ENABLED"},
    "payment": {"label": "payment system", "fixture": "payment.json", "env": "PAYMENT_ENABLED"},
    "profile": {"label": "profile management", "fixture": "profile.json", "env": "PROFILE_ENABLED"},
    "voice": {"label": "voice", "fixture": "voice.json", "env": "VOICE_ENABLED"},
}

EXTERNAL_APIS_PATH = BASE_DIR / "external_apis.json"
PLACEHOLDER_KEY = "PASTE_API_KEY_HERE"


def env_enabled(name: str) -> bool:
    value = os.getenv(name, "").strip().lower()
    return value in {"1", "true", "yes", "on"}


def load_fixture(filename: str) -> Dict[str, Any]:
    path = FIXTURE_DIR / filename
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def load_external_apis() -> Dict[str, Dict[str, Any]]:
    if not EXTERNAL_APIS_PATH.exists():
        return {}
    with EXTERNAL_APIS_PATH.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def api_info(feature_key: str) -> Dict[str, Any]:
    registry = load_external_apis()
    info = registry.get(feature_key, {})
    key_value = str(info.get("key", "")).strip()
    included_flag = bool(info.get("included", False))
    included = included_flag and key_value and key_value != PLACEHOLDER_KEY
    return {
        "name": info.get("name", feature_key),
        "included": included,
        "file": info.get("file", str(EXTERNAL_APIS_PATH)),
    }


def seeded_rng() -> random.Random:
    seed = int(date.today().strftime("%Y%m%d"))
    return random.Random(seed)


def randomize_numbers(data: Any, rng: random.Random) -> Any:
    if isinstance(data, bool):
        return data
    if isinstance(data, int):
        factor = 0.85 + rng.random() * 0.3
        return max(0, int(round(data * factor)))
    if isinstance(data, float):
        factor = 0.85 + rng.random() * 0.3
        return round(data * factor, 4)
    if isinstance(data, list):
        return [randomize_numbers(item, rng) for item in data]
    if isinstance(data, dict):
        return {key: randomize_numbers(value, rng) for key, value in data.items()}
    return data


def build_messages(
    feature_key: str,
    connected: bool,
    api_meta: Dict[str, Any],
    fallback_used: bool,
    error_detail: Optional[str],
) -> List[str]:
    messages: List[str] = []
    if not api_meta.get("included"):
        messages.append(
            f"External API not included: {api_meta.get('name')}. Update {api_meta.get('file')}."
        )
    if error_detail:
        messages.append(error_detail)
        messages.append("Failed to integrate feature; check backend/control/control_service.py.")
    if fallback_used:
        messages.append("Fallback used from local fixtures.")
    if connected and not error_detail:
        messages.append("API available and returned expected results.")
    return messages


def should_fail(feature_key: str) -> bool:
    return env_enabled(f"FAIL_FEATURE_{feature_key.upper()}")


def build_response(feature_key: str) -> Dict[str, Any]:
    if feature_key not in FEATURES:
        return {
            "feature": feature_key,
            "connected": False,
            "error": "Unknown feature.",
            "messages": ["Unknown feature. Check backend/control/control_service.py."],
            "data": {},
        }

    config = FEATURES[feature_key]
    connected = env_enabled(config["env"])
    api_meta = api_info(feature_key)
    error_detail = None
    fallback_used = False

    try:
        if should_fail(feature_key):
            raise RuntimeError("Failed to access external API.")

        if feature_key == "market":
            data = get_market_snapshot("SOL")["data"]
        elif feature_key == "voice":
            data = synthesize("Hello from Solaria")["data"]
        else:
            data = load_fixture(config["fixture"])
    except Exception as exc:
        error_detail = f"Failed to access {api_meta.get('name')} API. {exc}"
        data = load_fixture(config["fixture"])
        fallback_used = True

    if not connected or not api_meta.get("included"):
        data = randomize_numbers(data, seeded_rng())
        fallback_used = True
        status = "mock"
        error = f"The developer has not yet connected this {config['label']}."
    else:
        status = "connected"
        error = None

    messages = build_messages(feature_key, connected, api_meta, fallback_used, error_detail)

    return {
        "feature": feature_key,
        "connected": connected and api_meta.get("included"),
        "status": status,
        "error": error,
        "messages": messages,
        "data": data,
    }


def handle_feature(feature_key: str) -> Dict[str, Any]:
    return build_response(feature_key)
