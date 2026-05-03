# Solaria Android App

A **Kotlin/Jetpack Compose** native Android app — the frontend for the Solaria Voice-First Solana Super App.

## UI Design Credits
- Overall finance/wallet layout inspired by **openMF/mifos-pay** (Material Design banking patterns, gradient header cards, bottom navigation, transaction list items).
- Market analysis UI and charting inspired by **KhubaibKhan4/Crypto-KMP** (token performance cards, inline line charts, NFT collection grid).

## Screens

| Screen | Route | Description |
|--------|-------|-------------|
| **Chat / Bot** | `chat` | Main screen. LLM-powered assistant with voice + text input, bot bubbles, inline transaction approval cards. |
| **Market** | `market` | Token performance search with 7-day price chart + Top NFT collections grid. |
| **Wallet** | `wallet` | Solana Mobile Wallet Adapter connect/disconnect, SOL & SPL balances, PIN toggle. |

## Tech Stack

| Layer | Technology |
|-------|------------|
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| Networking | Retrofit 2 + OkHttp 4 |
| Caching | Room (chat history + price cache) |
| Audio | Media3 / ExoPlayer (ElevenLabs TTS) |
| Voice input | Android `SpeechRecognizer` |
| PIN approval | In-app PIN dialog |
| Wallet signing | Solana Mobile Wallet Adapter |
| EVM cross-chain | WalletConnect Web3Modal |
| Images | Coil |

## Build

```bash
# From repo root
cd android
./gradlew :app:assembleDebug        # build APK
./gradlew :app:installDebug         # install on device/emulator
```

Requires Android Studio **Hedgehog or newer** and **JDK 17**.

## Configuration

In `app/build.gradle.kts` `defaultConfig`:
```kotlin
buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8788/\"")
buildConfigField("String", "API_KEY",  "\"<your-api-key>\"")
```
Replace `10.0.2.2` with your machine's LAN IP when using a physical device.
Port `8788` is the control server (mock/fallback API).

## Transaction Approval Flow

1. User sends a message via chat (voice or text).
2. Backend LLM calls `GET /api/balance`, `GET /api/token/performance`, or `POST /api/prepare-transfer` as needed.
3. If a transfer is required the response includes `action.type = "TRANSFER"` with an unsigned tx.
4. The app shows an **ApprovalCard** inside the bot bubble.
5. User taps **Approve** → in-app PIN dialog.
6. On success → `MobileWalletAdapter.signTransaction(unsignedTx)`.
7. Signed tx sent to `POST /api/confirm-transfer`.
8. Bot confirms with tx hash.

## Required modules
- Solana Mobile Wallet Adapter (signing)
- Retrofit/OkHttp (backend)
- WalletConnect v2 / Web3Modal (EVM signing for LI.FI cross-chain)
- Media3 / ExoPlayer (ElevenLabs voice playback)
- In-app PIN dialog (transaction approval)
- Room (chat + price caching)

