# Solaria — Solana Super App (Android)

Solaria is a **native Android** (Jetpack Compose) Solana super app featuring:
- **AI-powered chat** — Gemini 1.5 Flash financial advisor + offline pattern matching
- **Real Solana DevNet integration** — Ed25519 keypair generation, balance checks, airdrops, signed transfers
- **Offline-first architecture** — Room DB with realistic seed data, works without internet
- **Firebase Auth + Firestore** — Secure user sessions and cross-device data sync
- **Token & NFT tracking** — CoinGecko prices, NFT collection data
- **On-chain PaymentIntent escrow** — Anchor program on Solana DevNet

## Architecture

```
┌─────────────────────────────────────────────────────┐
│  Android App (Jetpack Compose + Hilt)               │
│  ┌─────────────┐  ┌───────────────┐  ┌───────────┐ │
│  │ Room DB      │  │ SolanaRpcClient│  │ Gemini AI │ │
│  │ (offline     │  │ (DevNet RPC)   │  │ (1.5 Flash)│ │
│  │  seed data)  │  │               │  │           │ │
│  └──────┬──────┘  └───────┬───────┘  └─────┬─────┘ │
│         └────────┬────────┘                │       │
│           SolariaRepository ◄──────────────┘       │
│                  │                                  │
│  ┌───────────────┴───────────────┐                 │
│  │ Firebase Auth + Firestore     │                 │
│  └───────────────────────────────┘                 │
└─────────────────────────────────────────────────────┘

┌──────────────────────────────────┐  ┌──────────────────────────┐
│  Node.js Backend (Express)       │  │  Anchor Program (Rust)   │
│  - SOL balance / transfer API    │  │  - PaymentIntent escrow  │
│  - LI.FI cross-chain quotes      │  │  - Oracle-gated settle   │
│  - CoinGecko / MagicEden proxy   │  │  - Config PDA (admin +   │
│  - ElevenLabs signed URLs        │  │    oracle keys)          │
│  - RugCheck fraud checks         │  └──────────────────────────┘
└──────────────────────────────────┘
```

**Primary data flow:** The Android app talks directly to Solana DevNet RPC and CoinGecko. The backend is available for extended features (cross-chain, voice, fraud checks) but is not required for core functionality.

---

## Setup

### Prerequisites
- Android Studio Hedgehog+ (API 35 SDK, JDK 17)
- Node.js 18+ (for backend)
- Solana CLI + Anchor (for program deployment)

### 1) Android App

```bash
cd android
```

**Configure API keys** — add your keys to `android/local.properties` (never committed to git):

```properties
GEMINI_API_KEY=your_gemini_api_key_here
BITREFILL_API_KEY=your_bitrefill_key_here
BASE_URL=http://10.0.2.2:8788/
API_KEY=your_backend_api_key_here
```

**Firebase setup** — place your `google-services.json` from the [Firebase Console](https://console.firebase.google.com/) into `android/app/`.

**Build & run:**
```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

### 2) Backend

```bash
cd backend
cp .env.example .env
# Fill in your API keys in .env (especially ADMIN_API_KEY)
npm install
npm run dev
```

Backend runs on `http://localhost:8787`.

### 3) Solana Program (DevNet)

```bash
cd program
solana config set --url devnet
solana-keygen new -o target/deploy/solaria_program-keypair.json
anchor build
anchor deploy --provider.cluster devnet
```

After deploy:
1. Get the program ID: `solana address -k target/deploy/solaria_program-keypair.json`
2. Update it in:
   - `program/programs/solaria_program/src/lib.rs` → `declare_id!("...")`
   - `program/Anchor.toml` → `[programs.devnet]`
   - `backend/.env` → `SOLARIA_PROGRAM_ID=...`
3. Rebuild: `anchor build`
4. Initialize config: `anchor run init-config -- --oracle <ORACLE_PUBKEY>`

---

## App Screens

| Screen | Description |
|--------|-------------|
| **Auth** | Firebase email/password sign-in and sign-up |
| **Dashboard** (Home) | Portfolio value, SOL price, recent transactions |
| **Buy/Sell** | Yellow Card crypto on/off-ramp widget |
| **Payments** | Payment methods, transaction history, send SOL |
| **Market** | Token prices (7-day charts), NFT collections |
| **Card** | SolCard virtual card (placeholder) |
| **Account** | Wallet management, settings, sign out |
| **AI Chat** | Gemini-powered financial assistant |
| **Wallet** | Keypair management, balance, airdrop, send SOL |

---

## Key Dependencies

| Library | Purpose |
|---------|---------|
| Jetpack Compose + Material 3 | UI framework |
| Hilt | Dependency injection |
| Room | Local database (offline-first) |
| Retrofit + OkHttp | REST API (backend integration) |
| Firebase Auth + Firestore | Authentication + cloud sync |
| Google Generative AI (Gemini) | AI chat fallback |
| net.i2p.crypto.eddsa | Ed25519 keypair signing |
| AndroidX Security Crypto | Encrypted key storage |
| Coil | Image loading |
| WorkManager | Background sync tasks |

---

## Backend API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/health` | Health check |
| `GET` | `/api/balance?wallet=<pubkey>` | SOL balance for a wallet |
| `GET` | `/api/token/performance?symbol=SOL` | Token price + 24h change + history |
| `GET` | `/api/nfts/top` | Top NFT collections (Magic Eden + fallback) |
| `POST` | `/api/prepare-transfer` | Build unsigned SOL transfer tx |
| `POST` | `/api/confirm-transfer` | Broadcast signed transaction |
| `POST` | `/api/check-fraud` | RugCheck token risk analysis |
| `POST` | `/api/lifi/quote` | LI.FI cross-chain quote |
| `GET` | `/api/lifi/status?txHash=<hash>` | LI.FI tx status |
| `POST` | `/api/payment-link` | Create on-chain PaymentIntent |
| `POST` | `/api/settle-intent` | Oracle settles a PaymentIntent |
| `POST` | `/api/elevenlabs/signed-url` | ElevenLabs WebSocket URL |
| `POST` | `/api/chat` | Chat echo (stub — AI runs on-device) |

All endpoints except `/health` and `/api/balance` require `x-api-key` header (or `PUBLIC_DEMO_MODE=true`).

---

## Environment Variables

### Backend (`backend/.env`)

| Variable | Description |
|----------|-------------|
| `PORT` | Server port (default: 8787) |
| `SOLANA_CLUSTER` | `devnet` or `mainnet-beta` |
| `SOLANA_RPC_URL` | Optional RPC override |
| `SOLARIA_PROGRAM_ID` | Deployed Anchor program ID |
| `ORACLE_KEYPAIR_JSON` | Oracle keypair JSON array |
| `ELEVENLABS_API_KEY` | ElevenLabs API key |
| `ELEVENLABS_AGENT_ID` | ElevenLabs agent ID |
| `LIFI_BASE_URL` | LI.FI API base URL |
| `ADMIN_API_KEY` | Required for protected endpoints |
| `CORS_ORIGINS` | Comma-separated allowed origins |
| `RATE_LIMIT_WINDOW_MS` | Rate limit window (default: 900000) |
| `RATE_LIMIT_MAX` | Max requests per window (default: 100) |
| `SENSITIVE_RATE_LIMIT_MAX` | Max for sensitive endpoints (default: 20) |
| `PUBLIC_DEMO_MODE` | Bypass API key checks (dev only) |

### Android (`android/local.properties`)

| Property | Description |
|----------|-------------|
| `GEMINI_API_KEY` | Google Generative AI key |
| `BITREFILL_API_KEY` | Bitrefill API key |
| `BASE_URL` | Backend URL (default: `http://10.0.2.2:8788/`) |
| `API_KEY` | Backend `x-api-key` value |

---

## License

MIT
