from __future__ import annotations

import os
import sys
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List

BACKEND_DIR = Path(__file__).resolve().parents[1]
if str(BACKEND_DIR) not in sys.path:
    sys.path.append(str(BACKEND_DIR))

from fastapi import FastAPI, HTTPException, Query
from pydantic import BaseModel

from control_service import FEATURES, handle_feature

from market.market_service import get_market_snapshot
from program.program_service import handle_command
from voice.voice_service import synthesize

app = FastAPI(title="Solaria Control API", version="1.0.0")


class ChatRequest(BaseModel):
    message: str
    walletAddress: str
    conversationId: str | None = None


def list_features() -> Dict[str, str]:
    return {key: value["label"] for key, value in FEATURES.items()}


@app.get("/health")
def health() -> Dict[str, str]:
    return {"status": "ok"}


@app.get("/features")
def features() -> Dict[str, str]:
    return list_features()


@app.get("/api/{feature}")
def feature(feature: str) -> Dict:
    if feature not in FEATURES:
        raise HTTPException(status_code=404, detail="Unknown feature")
    return handle_feature(feature)


@app.get("/api/market")
def market(symbol: str = Query("SOL", min_length=1, max_length=12)) -> Dict:
    feature = handle_feature("market")
    payload = get_market_snapshot(symbol)
    payload["messages"] = feature.get("messages", [])
    return payload


@app.get("/api/command")
def command(text: str = Query(..., min_length=1, max_length=280)) -> Dict:
    return handle_command(text)


@app.post("/api/chat")
def chat(body: ChatRequest) -> Dict:
    feature = handle_feature("ai")
    reply = feature.get("data", {}).get("reply", "")
    voice_feature = handle_feature("voice")
    audio_url = voice_feature.get("data", {}).get("audio_url") if voice_feature.get("connected") else None
    return {
        "reply": reply or "Hello from Solaria.",
        "audioUrl": audio_url,
        "action": None,
        "conversationId": body.conversationId,
        "messages": feature.get("messages", []) + voice_feature.get("messages", []),
    }


@app.get("/api/balance")
def balance(wallet: str = Query(..., min_length=1)) -> Dict:
    feature = handle_feature("wallet")
    data = feature.get("data", {})
    tokens = []
    for token in data.get("tokens", []) or []:
        tokens.append(
            {
                "mint": "xxx",
                "symbol": token.get("symbol"),
                "amount": token.get("amount"),
                "usdValue": None,
            }
        )
    return {
        "sol": data.get("sol_balance", 0.0),
        "tokens": tokens,
        "messages": feature.get("messages", []),
    }


@app.get("/api/token/performance")
def token_performance(symbol: str = Query("SOL", min_length=1), days: int = Query(7, ge=1, le=30)) -> Dict:
    feature = handle_feature("market")
    snapshot = get_market_snapshot(symbol).get("data", {})
    history_values: List[float] = snapshot.get("token", {}).get("history_7d", [])
    now = datetime.utcnow()
    history = [
        {
            "timestamp": int((now - timedelta(days=len(history_values) - i)).timestamp() * 1000),
            "price": value,
        }
        for i, value in enumerate(history_values)
    ]
    return {
        "symbol": snapshot.get("token", {}).get("symbol", symbol),
        "mint": None,
        "price": snapshot.get("token", {}).get("price", 0.0),
        "change24h": snapshot.get("token", {}).get("change_24h", 0.0),
        "volume24h": None,
        "marketCap": None,
        "history": history if days >= 7 else None,
        "messages": feature.get("messages", []),
    }


@app.get("/api/nfts/top")
def top_nfts() -> Dict:
    feature = handle_feature("market")
    snapshot = get_market_snapshot("SOL").get("data", {})
    collections = []
    for item in snapshot.get("top_nfts", []):
        collections.append(
            {
                "symbol": item.get("name", "NFT"),
                "name": item.get("name", "NFT"),
                "image": None,
                "floorPrice": item.get("floor", 0.0),
                "volume24h": item.get("volume_24h", 0.0),
                "change24h": None,
            }
        )
    return {"collections": collections, "messages": feature.get("messages", [])}


@app.post("/api/prepare-transfer")
def prepare_transfer(body: Dict) -> Dict:
    feature = handle_feature("payment")
    return {
        "unsignedTxBase64": "xxx",
        "feeSol": 0.000005,
        "messages": feature.get("messages", []),
    }


@app.post("/api/confirm-transfer")
def confirm_transfer(body: Dict) -> Dict:
    feature = handle_feature("payment")
    return {
        "txHash": "xxx",
        "status": "MOCK",
        "messages": feature.get("messages", []),
    }


@app.post("/api/lifi/quote")
def lifi_quote(body: Dict) -> Dict:
    feature = handle_feature("payment")
    return {
        "requestId": "mock_request",
        "walletConnectUri": "wc:mock",
        "estimate": {"toAmount": "0", "executionDuration": 0},
        "messages": feature.get("messages", []),
    }


@app.get("/api/lifi/status")
def lifi_status(txHash: str = Query("")) -> Dict:
    feature = handle_feature("payment")
    return {
        "status": "PENDING",
        "txHash": txHash,
        "substatus": "mock",
        "messages": feature.get("messages", []),
    }


@app.post("/api/lifi/settle")
def lifi_settle(body: Dict) -> Dict:
    feature = handle_feature("payment")
    return {"status": "mock", "messages": feature.get("messages", [])}


if __name__ == "__main__":
    import uvicorn

    port = int(os.getenv("CONTROL_PORT", "8788"))
    uvicorn.run(app, host="0.0.0.0", port=port)
