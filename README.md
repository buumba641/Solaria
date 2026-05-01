# Solaria — Voice‑First Solana Super App (Android)

Solaria is a **native Android** (Jetpack Compose) "voice-first" Solana super app that supports:
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

```
Android app ↔ Backend (Node/Express) ↔ Solana RPC + LI.FI + ElevenLabs + RugCheck + MagicEden
```

- **Voice**: ElevenLabs Conversational AI WebSocket uses **short-lived signed URLs** from backend
- **Transactions**: backend prepares unsigned tx → app signs via Mobile Wallet Adapter → backend broadcasts
- **Cross-chain**: LI.FI quote in backend → execution on device via WalletConnect → status monitored in backend → settlement on Solana program (devnet)

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
# Fill in your API keys in .env
npm install
npm run dev
```

Backend will run on `http://localhost:8787`.

### 2) Solana program (devnet)
```bash
cd program
solana config set --url devnet
solana-keygen new -o target/deploy/solaria_program-keypair.json
anchor build
anchor deploy --provider.cluster devnet
```

After deploy:
1. Run `solana address -k target/deploy/solaria_program-keypair.json`
2. Copy the **program ID** into:
   - `program/programs/solaria_program/src/lib.rs` → `declare_id!("...")`
   - `program/Anchor.toml` → `[programs.devnet]`
   - `backend/.env` → `SOLARIA_PROGRAM_ID=...`
   - This README (above, under Track 1)
3. Run `anchor build` again so the binary matches the declared ID.

### 3) Initialize on-chain config
After deploy, initialize the config PDA with your oracle pubkey:
```bash
# Using the Anchor CLI or a custom script
anchor run init-config -- --oracle <ORACLE_PUBKEY>
```

### 4) Android

The Android project lives in `android/`.  It is a standard Gradle project; open it directly in **Android Studio Hedgehog or newer**.

#### Prerequisites
- Android Studio Hedgehog+ (API 35 SDK installed)
- JDK 17
- Physical Android device or emulator with API 26+

#### Build & run
```bash
cd android
# debug build
./gradlew :app:assembleDebug

# install on connected device
./gradlew :app:installDebug
```

#### Configuration
Edit `android/app/build.gradle.kts` `defaultConfig` block (or add a `local.properties` override) to set:
```
BASE_URL  = "http://<your-backend-host>:8787/"
API_KEY   = "<your x-api-key value>"
```

For a physical device connecting to a local backend replace `10.0.2.2` with your machine's LAN IP.

#### Wallet Connect (EVM cross-chain)
Create a WalletConnect Cloud project at https://cloud.walletconnect.com and add your **Project ID** to
`android/app/src/main/res/values/strings.xml` as `walletconnect_project_id`.

#### App screens
| Screen | Description |
|--------|-------------|
| **Chat** (default) | LLM bot – voice/text input, bot bubbles, inline transfer-approval cards |
| **Market** | Token performance (7-day chart) + top NFT collections grid |
| **Wallet** | MWA connect/disconnect, SOL + SPL balances, biometric toggle |

#### Key dependencies
| Library | Purpose |
|---------|---------|
| Jetpack Compose + Material 3 | UI |
| Hilt | Dependency injection |
| Retrofit + OkHttp | REST API calls |
| Room | Local caching (chat history, price data) |
| Media3 / ExoPlayer | TTS audio playback |
| AndroidX BiometricPrompt | Fingerprint / PIN approval |
| Solana Mobile Wallet Adapter | Wallet signing |
| WalletConnect Web3Modal | EVM signing for LI.FI cross-chain |
| Coil | Image loading (NFT thumbnails) |

---

## Environment Variables

Backend: `backend/.env`
- `PORT=8787`
- `SOLANA_CLUSTER=devnet` or `mainnet-beta`
- `SOLANA_RPC_URL=` (optional override)
- `SOLARIA_PROGRAM_ID=` (after deploy)
- `ORACLE_KEYPAIR_JSON=` (devnet oracle keypair JSON array)
- `ELEVENLABS_API_KEY=`
- `ELEVENLABS_AGENT_ID=`
- `LIFI_BASE_URL=https://li.quest/v1`
- `ADMIN_API_KEY=` (required for protected endpoints; send as `x-api-key`)
- `CORS_ORIGINS=` (comma-separated allowed origins)
- `RATE_LIMIT_WINDOW_MS=900000`
- `RATE_LIMIT_MAX=100`
- `SENSITIVE_RATE_LIMIT_MAX=20`
- `PUBLIC_DEMO_MODE=false` (when `true`, `x-api-key` checks are bypassed)

---

## API Endpoints (Backend)

| Method | Endpoint | Body / Params | Description |
|--------|----------|---------------|-------------|
| `GET` | `/health` | — | Health check |
| `POST` | `/api/elevenlabs/signed-url` | — | Get ElevenLabs WebSocket signed URL (requires `x-api-key`) |
| `POST` | `/api/chat` | `{ message, walletAddress, conversationId? }` | LLM bot – returns `{ reply, audioUrl?, action? }` where `action.type` is `TRANSFER`, `CROSS_CHAIN`, or absent (requires `x-api-key`) |
| `GET` | `/api/token/performance` | `?symbol=SOL&days=7` | Token price + 24h change + optional 7-day history array (cached, requires `x-api-key`) |
| `GET` | `/api/balance` | `?wallet=<pubkey>` | SOL balance + SPL token list for a wallet |
| `POST` | `/api/prepare-transfer` | `{ from, to, amountSol }` | Build unsigned SOL transfer tx (requires `x-api-key`) |
| `POST` | `/api/confirm-transfer` | `{ signedTxBase64, commitment? }` + `?wait=true` | Broadcast a signed transaction; optionally wait for confirmation (requires `x-api-key`) |
| `GET` | `/api/nfts/top` | — | Top NFT collections (requires `x-api-key`) |
| `POST` | `/api/lifi/quote` | `{ fromChain, toChain, fromToken, toToken, amount, fromAddress }` | LI.FI cross-chain quote → returns `{ requestId, walletConnectUri, estimate }` (requires `x-api-key`) |
| `GET` | `/api/lifi/status` | `?txHash=<hash>` | LI.FI cross-chain tx status – `PENDING \| DONE \| FAILED` (requires `x-api-key`) |
| `POST` | `/api/lifi/settle` | `{ requestId }` | Trigger Solana devnet settlement after EVM tx completes (requires `x-api-key`) |
| `POST` | `/api/payment-link` | `{ merchant, amount, description? }` | Create PaymentIntent on-chain (requires `x-api-key`) |
| `POST` | `/api/settle-intent` | `{ intentId, merchantAddress, sourceChainTx, proofHash? }` | Oracle settles a PaymentIntent (requires `x-api-key`) |
| `POST` | `/api/top-up/quote` | `{ fromChain, toChain, fromToken, toToken, amount, ... }` | Legacy LI.FI cross-chain quote alias (requires `x-api-key`) |
| `GET` | `/api/top-up/status` | `?txHash=<hash>` | Legacy LI.FI status alias (requires `x-api-key`) |

### `/api/chat` action payloads

When the LLM decides to initiate a transaction it attaches an `action` object:

**TRANSFER**
```json
{
  "action": {
    "type": "TRANSFER",
    "payload": {
      "unsignedTxBase64": "...",
      "to": "<recipient pubkey>",
      "amountSol": 0.5,
      "feeSol": 0.000005
    }
  }
}
```

**CROSS_CHAIN**
```json
{
  "action": {
    "type": "CROSS_CHAIN",
    "lifiRequest": {
      "requestId": "abc123",
      "walletConnectUri": "wc:...",
      "fromChain": "ETH",
      "fromToken": "ETH",
      "amount": "0.01"
    }
  }
}
```

---

## 3‑Minute Demo Script
1. Open app → voice greeting
2. Ask SOL performance
3. Fraud check token
4. "Send 0.1 SOL …" → fee/balance → confirm → biometric signing → tx hash
5. Top NFTs today
6. Cross-chain top-up quote (LI.FI) → confirm → WalletConnect signing → backend status DONE
7. Merchant creates payment link → intent created on devnet → settlement shown

---

## Important Note (LI.FI + WalletConnect Flow)
The cross-chain execution **cannot** be done by the backend from the user's wallet. The correct model is:
- **Backend**: quote + route data + status polling
- **App**: WalletConnect signs/executes the EVM transaction on testnet
- **Backend**: polls LI.FI status and triggers `settle_payment` on Solana devnet using oracle key

---

## License
MIT (optional)
