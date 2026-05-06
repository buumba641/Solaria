package com.solaria.app.data.repository

import com.solaria.app.data.db.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline AI engine that handles chat responses using local database data.
 * Uses pattern matching against user messages to provide contextual responses.
 * All data comes from Room DB — works fully offline.
 *
 * When online, balance/price data is refreshed from DevNet + CoinGecko
 * and flows into the same Room tables that this engine reads from.
 */
@Singleton
class OfflineAiEngine @Inject constructor(
    private val repository: SolariaRepository,
    private val gemini: com.solaria.app.domain.usecases.AskGeminiUseCase,
    private val authManager: com.solaria.app.data.auth.FirebaseAuthManager
) {
    suspend fun generateResponse(userMessage: String): AiResponse {
        val msg = userMessage.lowercase().trim()
        
        // 1. Try local pattern matching
        if (msg.contains("airdrop")) {
            val amount = msg.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 1.0
            val userId = authManager.getCurrentUser() ?: ""
            return try {
                val signature = repository.airdropAndSync(userId, amount)
                AiResponse("✅ Airdropped $amount SOL from DevNet faucet!\n\nTX: ${signature.take(20)}…\nBalance synced to cloud.")
            } catch (e: Exception) {
                AiResponse("❌ Airdrop failed: ${e.message}\n\nDevNet faucet limits: ~2 SOL per request, ~5 SOL per 8 hours.")
            }
        }

        return when {
            msg.containsAny("balance", "wallet", "how much") -> handleBalance()
            msg.containsAny("sol price", "sol performance", "solana price") -> handleSolPrice()
            msg.containsAny("send", "transfer") -> handleTransfer(msg)
            msg.containsAny("nft", "collection", "top nft") -> handleNfts()
            msg.containsAny("token", "bonk", "jup", "price") -> handleTokenPrice(msg)
            msg.containsAny("transaction", "history", "recent") -> handleHistory()
            msg.containsAny("payment", "pay", "mobile money", "bank") -> handlePayments()

            msg.containsAny("sync", "update", "offline", "last update") -> handleSyncStatus()
            msg.containsAny("help", "what can") -> handleHelp()
            msg.containsAny("airdrop", "fund", "faucet") -> handleAirdrop()
            msg.containsAny("developer", "setup", "start building", "anchor", "cli") -> handleDevSetup()
            msg.containsAny("hello", "hi ", "hey", "good") -> handleGreeting()
            else -> gemini.ask(userMessage).let { AiResponse(it) }
        }
    }

    private suspend fun handleBalance(): AiResponse {
        val wallet = repository.observeConnectedWallet().first()
        if (wallet == null) return AiResponse("No wallet connected. Go to your Account screen to connect or generate a wallet.")

        // Try to refresh from DevNet
        try {
            repository.refreshBalance(wallet.address)
        } catch (_: Exception) { /* Use cached */ }

        val updatedWallet = repository.observeConnectedWallet().first() ?: wallet
        val tokens = repository.observeTokenBalances(updatedWallet.address).first()
        val tokenList = tokens.joinToString("\n") { "  • ${it.symbol}: ${it.amount} ($${it.usdValue ?: 0.0})" }
        val solPrice = repository.getPrice("SOL")?.price ?: 0.0
        val solUsd = updatedWallet.balanceSol * solPrice
        val hasKeypair = repository.keypairManager.hasKeypair(updatedWallet.address)
        return AiResponse(
            "💰 Your wallet balance:\n\n" +
            "SOL: ${"%.4f".format(updatedWallet.balanceSol)} (${"$%.2f".format(solUsd)})\n" +
            "Address: ${updatedWallet.address.take(8)}…${updatedWallet.address.takeLast(4)}\n" +
            "Mode: ${if (hasKeypair) "Full access (local keypair)" else "Watch-only"}\n" +
            if (tokenList.isNotBlank()) "\nSPL Tokens:\n$tokenList" else "\nNo SPL tokens found.",
            dataType = "balance"
        )
    }

    private suspend fun handleSolPrice(): AiResponse {
        // Try to refresh from CoinGecko
        try { repository.refreshSolPrice() } catch (_: Exception) { /* Use cached */ }

        val sol = repository.getPrice("SOL")
        if (sol == null) return AiResponse("SOL price data is not available. Connect to the internet to fetch live prices.")
        val dir = if (sol.change24h >= 0) "📈" else "📉"
        return AiResponse(
            "$dir SOL Performance:\n\n" +
            "Price: ${"$%.2f".format(sol.price)}\n" +
            "24h Change: ${if (sol.change24h >= 0) "+" else ""}${"%.2f".format(sol.change24h)}%\n" +
            (if (sol.volume24h != null) "Volume: ${"$%.0f".format(sol.volume24h)}\n" else "") +
            "\nData updated: ${repository.formatSyncAge(sol.updatedAtMs)}"
        )
    }

    private suspend fun handleTransfer(msg: String): AiResponse {
        val amountRegex = Regex("""(\d+\.?\d*)\s*sol""", RegexOption.IGNORE_CASE)
        val amount = amountRegex.find(msg)?.groupValues?.get(1)?.toDoubleOrNull()
        val toRegex = Regex("""to\s+([A-Za-z0-9]{32,44})""")
        val toAddr = toRegex.find(msg)?.groupValues?.get(1)
        return if (amount != null && toAddr != null) {
            val wallet = repository.observeConnectedWallet().first()
            val hasKeypair = wallet?.let { repository.keypairManager.hasKeypair(it.address) } ?: false
            AiResponse(
                "I'll prepare a transfer of $amount SOL to ${toAddr.take(6)}…${toAddr.takeLast(4)}.\n\n" +
                if (hasKeypair) {
                    "🔑 Your local keypair will sign this transaction.\n" +
                    "⚡ It will be broadcast to Solana DevNet immediately.\n" +
                    "Please review and approve the transaction."
                } else {
                    "⚠️ Watch-only wallet — transaction will be queued.\n" +
                    "Import your private key to sign and broadcast."
                },
                actionType = "TRANSFER", actionAmount = amount, actionTo = toAddr
            )
        } else {
            AiResponse("To send SOL, say something like:\n\"Send 0.1 SOL to <wallet address>\"")
        }
    }

    private suspend fun handleAirdrop(): AiResponse {
        val wallet = repository.observeConnectedWallet().first()
        if (wallet == null) return AiResponse("No wallet connected. Generate one first in the Wallet screen.")

        return try {
            val sig = repository.requestAirdrop(wallet.address, 1.0)
            AiResponse(
                "✅ Airdrop successful!\n\n" +
                "Amount: 1.0 SOL\n" +
                "TX: ${sig.take(20)}…\n" +
                "New balance: ${"%.4f".format(repository.refreshBalance(wallet.address))} SOL\n\n" +
                "DevNet faucet limit: ~2 SOL per request, ~5 SOL per 8 hours."
            )
        } catch (e: Exception) {
            AiResponse("❌ Airdrop failed: ${e.message}\n\nTry again later or visit faucet.solana.com.")
        }
    }

    private suspend fun handleNfts(): AiResponse {
        val nfts = repository.observeNftCollections().first()
        if (nfts.isEmpty()) return AiResponse("No NFT collection data cached. Connect to update.")
        val list = nfts.take(5).joinToString("\n") {
            "  • ${it.name}: Floor ${it.floorPrice} SOL" +
            (if (it.change24h != null) " (${if (it.change24h >= 0) "+" else ""}${"%.1f".format(it.change24h)}%)" else "")
        }
        return AiResponse("🖼️ Top NFT Collections:\n\n$list\n\nView the Market tab for more details.")
    }

    private suspend fun handleTokenPrice(msg: String): AiResponse {
        val symbol = when {
            msg.contains("bonk") -> "BONK"
            msg.contains("jup") -> "JUP"
            else -> "SOL"
        }
        val price = repository.getPrice(symbol)
        if (price == null) return AiResponse("$symbol price data is not available offline.")
        return AiResponse(
            "💱 $symbol: ${"$%.6f".format(price.price)} " +
            "(${if (price.change24h >= 0) "+" else ""}${"%.2f".format(price.change24h)}%)\n" +
            "Updated: ${repository.formatSyncAge(price.updatedAtMs)}"
        )
    }

    private suspend fun handleHistory(): AiResponse {
        val txs = repository.observeRecentTransactions(5).first()
        if (txs.isEmpty()) return AiResponse("No transaction history yet.")
        val list = txs.joinToString("\n") {
            val dir = if (it.type.contains("RECEIVE")) "⬇️" else "⬆️"
            val amt = "${"%.4f".format(it.amountSol)} SOL"
            "$dir ${it.description ?: it.type} — $amt [${it.status}]"
        }
        return AiResponse("📜 Recent Transactions:\n\n$list")
    }

    private fun handlePayments(): AiResponse = AiResponse(
        "💳 Payment Methods:\n\n" +
        "  • Solana Devnet — Active (real on-chain transactions)\n" +
        "  • MTN Mobile Money — Placeholder\n" +
        "  • Airtel Money — Placeholder\n" +
        "  • Standard Bank — Placeholder\n\n" +
        "DevNet transactions are now signed locally and broadcast to the real Solana DevNet cluster."
    )



    private suspend fun handleSyncStatus(): AiResponse {
        val meta = repository.observeAllSyncMetadata().first()
        if (meta.isEmpty()) return AiResponse("No sync data available.")
        val list = meta.joinToString("\n") {
            "  • ${it.dataType}: ${repository.formatSyncAge(it.lastSyncedAt)} (${it.status})"
        }
        return AiResponse("🔄 Sync Status:\n\n$list\n\nAll data is stored offline and updated when connected.")
    }

    private fun handleHelp(): AiResponse = AiResponse(
        "I can help you with:\n\n" +
        "💰 \"What's my balance?\"\n" +
        "📈 \"SOL price\" or \"BONK price\"\n" +
        "⬆️ \"Send 0.5 SOL to <address>\"\n" +
        "🖼️ \"Top NFT collections\"\n" +
        "📜 \"Transaction history\"\n" +
        "💳 \"Payment methods\"\n" +

        "💧 \"Airdrop\" — Get free DevNet SOL\n" +
        "🛠️ \"Developer setup guide\"\n" +
        "🔄 \"Sync status\"\n\n" +
        "All data works offline!"
    )

    private fun handleDevSetup(): AiResponse = AiResponse(
        "🛠️ **Solana Developer Setup Guide**\n\n" +
        "1️⃣ **Solana CLI**: `sh -c \"\$(curl -sSfL https://release.solana.com/stable/install)\"`\n" +
        "2️⃣ **Anchor**: `cargo install --git https://github.com/coral-xyz/anchor anchor-cli --locked`\n" +
        "3️⃣ **Wallet**: `solana-keygen new` (then `solana config set --url devnet`)\n" +
        "4️⃣ **Airdrop**: `solana airdrop 2` (or visit faucet.solana.com)\n" +
        "5️⃣ **Template**: Use the **Next.js + Anchor** template to start building.\n\n" +
        "Type \"help\" for more commands or visit docs.solana.com."
    )

    private fun handleGreeting(): AiResponse = AiResponse(
        "Hello! 👋 I'm Solaria, your Solana assistant.\n" +
        "I now connect to the real Solana DevNet! Ask me about your balance, prices, or say \"airdrop\" to get free SOL.\n" +
        "I also work offline using your local data."
    )

    private fun String.containsAny(vararg keywords: String) = keywords.any { this.contains(it) }
}

data class AiResponse(
    val text: String,
    val actionType: String? = null,
    val actionAmount: Double? = null,
    val actionTo: String? = null,
    val dataType: String? = null
)
