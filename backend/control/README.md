# Control Layer (CLI)

This folder provides a small, testable control layer that simulates frontend calls
and routes them to feature handlers. It supports mock fallbacks when APIs are not
connected.

## Usage

```bash
cd backend/control
python3 control_cli.py ai
python3 control_cli.py market
python3 control_cli.py wallet
python3 control_cli.py payment
python3 control_cli.py profile
python3 control_cli.py voice
```

## HTTP server

```bash
cd backend/control
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python3 server.py
```

Default port: `8788` (override with `CONTROL_PORT`).

Endpoints:
- `GET /health`
- `GET /features`
- `GET /api/{feature}` where `feature` is one of: ai, wallet, payment, profile, voice
- `GET /api/market?symbol=SOL`
- `GET /api/command?text=Show%20balance`
- `POST /api/chat`
- `GET /api/balance?wallet=<pubkey>`
- `GET /api/token/performance?symbol=SOL&days=7`
- `GET /api/nfts/top`
- `POST /api/prepare-transfer`
- `POST /api/confirm-transfer`
- `POST /api/lifi/quote`
- `GET /api/lifi/status?txHash=<hash>`
- `POST /api/lifi/settle`

## Feature flags

Set any of these to enable a feature (otherwise mock data is returned):

```bash
export AI_ENABLED=true
export MARKET_ENABLED=true
export WALLET_ENABLED=true
export PAYMENT_ENABLED=true
export PROFILE_ENABLED=true
export VOICE_ENABLED=true
```

## External API registry

Edit `external_apis.json` to paste API keys and mark providers as included.
When a key is missing or set to `PASTE_API_KEY_HERE`, responses include a
"not included" message and fallback to mock data.
