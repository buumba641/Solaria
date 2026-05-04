package com.solaria.app.data.repository

import com.solaria.app.data.db.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline AI engine that handles chat responses using local database data.
 * Uses pattern matching against user messages to provide contextual responses.
 * All data comes from Room DB — works fully offline.
 */
@Singleton
class OfflineAiEngine @Inject constructor(
    private val repository: SolariaRepository
) {
    suspend fun generateResponse(userMessage: String): AiResponse {
        val msg = userMessage.lowercase().trim()
        return when {
            msg.containsAny("balance", "wallet", "how much") -> handleBalance()
            msg.containsAny("sol price", "sol performance", "solana price") -> handleSolPrice()
            msg.containsAny("send", "transfer") -> handleTransfer(msg)
            msg.containsAny("nft", "collection", "top nft") -> handleNfts()
            msg.containsAny("token", "bonk", "jup", "price") -> handleTokenPrice(msg)
            msg.containsAny("transaction", "history", "recent") -> handleHistory()
            msg.containsAny("payment", "pay", "mobile money", "bank") -> handlePayments()
            msg.containsAny("p2p", "nearby", "peer") -> handleP2P()
            msg.containsAny("sync", "update", "offline", "last update") -> handleSyncStatus()
            msg.containsAny("help", "what can") -> handleHelp()
            msg.containsAny("hello", "hi ", "hey", "good") -> handleGreeting()
            else -> handleGeneral()
        }
    }

    private suspend fun handleBalance(): AiResponse {
        val wallet = repository.observeConnectedWallet().first()
        if (wallet == null) return AiResponse("No wallet connected. Go to your Account screen to connect a wallet.")
        val tokens = repository.observeTokenBalances(wallet.address).first()
        val tokenList = tokens.joinToString("\n") { "  • ${it.symbol}: ${it.amount} ($${it.usdValue ?: 0.0})" }
        val solPrice = repository.getPrice("SOL")?.price ?: 0.0
        val solUsd = wallet.balanceSol * solPrice
        return AiResponse(
            "💰 Your wallet balance:\n\n" +
            "SOL: ${"%.4f".format(wallet.balanceSol)} (${"$%.2f".format(solUsd)})\n" +
            if (tokenList.isNotBlank()) "\nSPL Tokens:\n$tokenList" else "\nNo SPL tokens found.",
            dataType = "balance"
        )
    }

    private suspend fun handleSolPrice(): AiResponse {
        val sol = repository.getPrice("SOL")
        if (sol == null) return AiResponse("SOL price data is not available offline. Connect to update.")
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
            AiResponse(
                "I'll prepare a transfer of $amount SOL to ${toAddr.take(6)}…${toAddr.takeLast(4)}.\n\n" +
                "⚠️ This will require network access to broadcast to Solana devnet.\n" +
                "Please review and approve the transaction.",
                actionType = "TRANSFER", actionAmount = amount, actionTo = toAddr
            )
        } else {
            AiResponse("To send SOL, say something like:\n\"Send 0.1 SOL to <wallet address>\"")
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
        "  • Solana Devnet — Active (real transactions)\n" +
        "  • MTN Mobile Money — Placeholder\n" +
        "  • Airtel Money — Placeholder\n" +
        "  • Standard Bank — Placeholder\n\n" +
        "Only Devnet transactions are processed on-chain. Bank and mobile money integrations are coming soon."
    )

    private fun handleP2P(): AiResponse = AiResponse(
        "📡 P2P Transactions:\n\n" +
        "If another Solaria device is nearby, you can send/receive SOL without internet.\n" +
        "The transaction is queued locally and synced to the blockchain when either device goes online.\n\n" +
        "Go to Payments → P2P to start."
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
        "📡 \"P2P transactions\"\n" +
        "🔄 \"Sync status\"\n\n" +
        "All data works offline!"
    )

    private fun handleGreeting(): AiResponse = AiResponse(
        "Hello! 👋 I'm Solaria, your offline-first Solana assistant.\n" +
        "Ask me about your balance, prices, or transactions. I work even without internet!"
    )

    private fun handleGeneral(): AiResponse = AiResponse(
        "I'm not sure I understand. Try asking about:\n" +
        "• Your wallet balance\n• Token prices\n• Sending SOL\n• NFT collections\n• Transaction history\n\n" +
        "Type \"help\" for a full list of commands."
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
