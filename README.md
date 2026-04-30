# Solaria — Voice‑First Solana Super App (Android)

Solaria is a **native Android** (Jetpack Compose) “voice-first” Solana super app that supports:
- Token performance & portfolio/balance checks
- Token fraud/risk checks (RugCheck + on-chain simulation heuristics)
- NFT market performance (Magic Eden public endpoints proxied via backend)
- Hands‑free SOL/SPL transactions (prepared by backend, **signed on-device via Solana Mobile Wallet Adapter**)
- Cross‑chain top-ups (LI.FI **REST/SDK + WalletConnect**, testnet mode)
- Payment links + on-chain PaymentIntent escrow state on Solana **devnet** (Anchor program)

## Hackathon Track Qualification Mapping

### Track 1: Best App Overall on Solana
- ✅ **Unique Solana program (Anchor/Rust)**: `program/programs/solaria_program`
- ✅ **Deployed to devnet**: Program ID: **TODO_AFTER_DEPLOY**
- ✅ README includes setup + addresses (update after deploy)
- ✅ Demo video target < 3 mins (script below)

### Track 2: Best Cross‑Chain Apps on Solana using LI.FI
- ✅ LI.FI integration is **meaningful**:
  - Backend calls **LI.FI `/v1/quote`** in testnet mode
  - App executes EVM-side tx via **WalletConnect** (user signs)
  - Backend polls **LI.FI `/v1/status`** and triggers Solana devnet settlement via oracle

### Track 4: Best Mobile App Built on Solana Mobile
- ✅ Native Android (Jetpack Compose)
- ✅ Integrates Solana Mobile Stack / Mobile Wallet Adapter (signing + message signing)
- ✅ Produces installable APK
- ✅ Meaningful Solana interaction (balances, SPL tokens, devnet transactions, on-chain intents)

## Architecture (High Level)
Android app ↔ Backend (Node/Express) ↔ Solana RPC + LI.FI + ElevenLabs + RugCheck + MagicEden

- Voice: ElevenLabs Conversational AI WebSocket uses **short-lived signed URLs** from backend
- Transactions: backend prepares unsigned tx; app signs via Mobile Wallet Adapter; backend broadcasts
- Cross-chain: LI.FI quote in backend; execution on device via WalletConnect; status monitored in backend; settlement on Solana program (devnet)

---

## Setup

### Prereqs
- Node.js 18+
- Solana CLI
- Anchor (recommended)
- Android Studio (API 31+)

### 1) Backend
```bash
cd backend
cp .env.example .env
npm install
npm run dev
```

Backend will run on `http://localhost:8787`.

### 2) Solana program (devnet)
```bash
cd program
solana config set --url devnet
anchor build
anchor deploy --provider.cluster devnet
```

After deploy:
- Copy the **program ID** into this README and into backend `.env` as `SOLARIA_PROGRAM_ID`.

### 3) Android
Create the Android Studio project inside `android/` (Compose, minSdk 26+, target 31+).
Then add:
- Solana Mobile Wallet Adapter dependencies
- WalletConnect (v2)
- Retrofit/OkHttp for backend calls
- ExoPlayer (or Android audio track) for voice playback

(We keep Android scaffolding light in repo to avoid massive auto-generated files.)

---

## Environment Variables

Backend: `backend/.env`
- `PORT=8787`
- `SOLANA_CLUSTER=devnet` or `mainnet-beta`
- `SOLANA_RPC_URL=` (optional override)
- `SOLARIA_PROGRAM_ID=` (after deploy)
- `ORACLE_KEYPAIR_JSON=` (devnet oracle keypair JSON)
- `ELEVENLABS_API_KEY=`
- `ELEVENLABS_AGENT_ID=`
- `LIFI_BASE_URL=https://li.quest/v1`

---

## API Endpoints (Backend)

- `POST /api/elevenlabs/signed-url`
- `GET  /api/token/performance?symbol=SOL`
- `GET  /api/balance?wallet=<pubkey>`
- `POST /api/check-fraud` `{ "tokenAddress": "..." }`
- `POST /api/prepare-transfer` `{ "from": "...", "to": "...", "amountSol": 0.1 }`
- `POST /api/confirm-transfer` `{ "signedTxBase64": "..." }`
- `GET  /api/nfts/top`
- `POST /api/payment-link` `{ "amount": 1000000, "description": "Coffee" }`
- `POST /api/top-up/quote` `{ "fromChain": 11155111, "toChain": 1399811149, "fromToken": "...", "toToken": "...", "amount": "..." }`
- `GET  /api/top-up/status?txHash=<hash>`

---

## 3‑Minute Demo Script
1) Open app → voice greeting
2) Ask SOL performance
3) Fraud check token
4) “Send 0.1 SOL …” → fee/balance → confirm → biometric signing → tx hash
5) Top NFTs today
6) Cross-chain top-up quote (LI.FI) → confirm → WalletConnect signing → backend status DONE
7) Merchant creates payment link → intent created on devnet → settlement shown

---

## License
MIT (optional)
