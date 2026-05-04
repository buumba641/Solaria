package com.solaria.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solaria.app.data.db.ChatMessageEntity
import com.solaria.app.data.models.*
import com.solaria.app.data.repository.OfflineAiEngine
import com.solaria.app.data.repository.SolariaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UiChatMessage(
    val id: Long = 0,
    val role: String,          // "user" | "bot"
    val text: String,
    val audioUrl: String? = null,
    val action: BotAction? = null,
    val timestampMs: Long = System.currentTimeMillis()
)

sealed class ChatUiState {
    object Idle    : ChatUiState()
    object Loading : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}

sealed class ApprovalState {
    object None : ApprovalState()
    data class Pending(
        val actionType: String,
        val description: String,
        val payload: TransactionPayload? = null,
        val lifiRequest: LifiRequest? = null
    ) : ApprovalState()
    object Processing : ApprovalState()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repo: SolariaRepository,
    private val aiEngine: OfflineAiEngine
) : ViewModel() {

    private val conversationId = "solaria_main"

    // Persisted message flow from Room
    val messages: StateFlow<List<UiChatMessage>> =
        repo.observeChatMessages(conversationId)
            .map { entities -> entities.map { it.toUi() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _approvalState = MutableStateFlow<ApprovalState>(ApprovalState.None)
    val approvalState: StateFlow<ApprovalState> = _approvalState.asStateFlow()

    // ── Send a message (offline AI) ───────────────────────
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            // Persist user message
            repo.insertChatMessage(
                ChatMessageEntity(conversationId = conversationId, role = "user", text = text)
            )
            _uiState.value = ChatUiState.Loading

            try {
                // Small delay for natural feel
                kotlinx.coroutines.delay(600)

                val response = aiEngine.generateResponse(text)

                // Build action if the AI detected a transfer intent
                val action = if (response.actionType == "TRANSFER" && response.actionAmount != null && response.actionTo != null) {
                    BotAction(
                        type = "TRANSFER",
                        payload = TransactionPayload(
                            unsignedTxBase64 = "offline_placeholder",
                            to = response.actionTo,
                            amountSol = response.actionAmount,
                            feeSol = 0.000005
                        )
                    )
                } else null

                // Persist bot reply
                repo.insertChatMessage(
                    ChatMessageEntity(
                        conversationId = conversationId,
                        role = "bot",
                        text = response.text,
                        actionType = action?.type
                    )
                )

                // Trigger approval flow if action requires it
                if (action?.type == "TRANSFER") {
                    val p = action.payload!!
                    _approvalState.value = ApprovalState.Pending(
                        actionType = "TRANSFER",
                        description = "Send ${p.amountSol} SOL to ${p.to}",
                        payload = p
                    )
                }

                _uiState.value = ChatUiState.Idle
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // ── Approve a pending transaction ─────────────────────
    fun approvePending(signedTxBase64: String) {
        val pending = (_approvalState.value as? ApprovalState.Pending) ?: return
        _approvalState.value = ApprovalState.Processing
        viewModelScope.launch {
            try {
                // Create a transaction in the DB
                val wallet = repo.observeConnectedWallet().first()
                if (wallet != null && pending.payload != null) {
                    val tx = repo.createTransaction(
                        fromAddress = wallet.address,
                        toAddress = pending.payload.to,
                        amountSol = pending.payload.amountSol,
                        type = "SEND", paymentMethod = "DEVNET",
                        description = "Send ${pending.payload.amountSol} SOL",
                        isOffline = true // Will need network to actually broadcast
                    )
                    repo.insertChatMessage(
                        ChatMessageEntity(
                            conversationId = conversationId, role = "bot",
                            text = "✅ Transaction queued. ID: ${tx.id.take(8)}…\nWill broadcast to Solana devnet when online."
                        )
                    )
                }
                _approvalState.value = ApprovalState.None
            } catch (e: Exception) {
                _approvalState.value = ApprovalState.None
                repo.insertChatMessage(
                    ChatMessageEntity(
                        conversationId = conversationId, role = "bot",
                        text = "❌ Transaction failed: ${e.message}"
                    )
                )
            }
        }
    }

    fun rejectPending() {
        _approvalState.value = ApprovalState.None
        viewModelScope.launch {
            repo.insertChatMessage(
                ChatMessageEntity(
                    conversationId = conversationId, role = "bot",
                    text = "Transaction rejected by user."
                )
            )
        }
    }

    fun dismissError() { _uiState.value = ChatUiState.Idle }

    // ── Helper ────────────────────────────────────────────
    private fun ChatMessageEntity.toUi() = UiChatMessage(
        id = id, role = role, text = text, audioUrl = audioUrl,
        action = if (actionType != null)
            BotAction(type = actionType, payload = null)
        else null,
        timestampMs = timestampMs
    )
}
