from __future__ import annotations

import json
import os
from pathlib import Path
from typing import Any, Dict

BASE_DIR = Path(__file__).resolve().parent
FIXTURE_DIR = BASE_DIR / "fixtures"


def env_enabled(name: str) -> bool:
    value = os.getenv(name, "").strip().lower()
    return value in {"1", "true", "yes", "on"}


def load_fixture(filename: str) -> Dict[str, Any]:
    path = FIXTURE_DIR / filename
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def synthesize(text: str) -> Dict[str, Any]:
    connected = env_enabled("ELEVENLABS_ENABLED")
    data = load_fixture("voice_response.json")
    data["text"] = text

    if not connected:
        error = "The developer has not yet connected this voice system."
        status = "mock"
    else:
        error = None
        status = "connected"

    return {
        "feature": "voice",
        "connected": connected,
        "status": status,
        "error": error,
        "data": data,
    }
