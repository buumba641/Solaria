package com.solaria.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.solaria.app.data.api.SolariaApiService
import com.solaria.app.data.db.ChatDao
import com.solaria.app.data.db.ChatMessageEntity
import com.solaria.app.data.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
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
    private val api: SolariaApiService,
    private val chatDao: ChatDao
) : ViewModel() {

    private val conversationId = UUID.randomUUID().toString()
    private val gson = Gson()

    // Persisted message flow from Room
    val messages: StateFlow<List<UiChatMessage>> =
        chatDao.observeMessages(conversationId)
            .map { entities -> entities.map { it.toUi() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _approvalState = MutableStateFlow<ApprovalState>(ApprovalState.None)
    val approvalState: StateFlow<ApprovalState> = _approvalState.asStateFlow()

    // Currently connected wallet address (set from WalletScreen / MWA)
    private val _walletAddress = MutableStateFlow("")
    val walletAddress: StateFlow<String> = _walletAddress.asStateFlow()

    fun setWalletAddress(address: String) { _walletAddress.value = address }

    // ── Send a message ─────────────────────────────────────────
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            // Persist user message
            chatDao.insert(
                ChatMessageEntity(
                    conversationId = conversationId,
                    role = "user",
                    text = text
                )
            )
            _uiState.value = ChatUiState.Loading
            try {
                val response = api.chat(
                    ChatRequest(
                        message = text,
                        walletAddress = _walletAddress.value.ifBlank { "demo" },
                        conversationId = conversationId
                    )
                )
                // Persist bot reply
                chatDao.insert(
                    ChatMessageEntity(
                        conversationId = conversationId,
                        role = "bot",
                        text = response.reply,
                        audioUrl = response.audioUrl,
                        actionType = response.action?.type,
                        actionPayloadJson = response.action?.payload?.let { gson.toJson(it) }
                    )
                )
                // Trigger approval flow if action requires it
                response.action?.let { action ->
                    when (action.type) {
                        "TRANSFER" -> {
                            val p = action.payload ?: return@let
                            _approvalState.value = ApprovalState.Pending(
                                actionType = "TRANSFER",
                                description = "Send ${p.amountSol} SOL to ${p.to}",
                                payload = p
                            )
                        }
                        "CROSS_CHAIN" -> {
                            val lifi = action.lifiRequest ?: return@let
                            _approvalState.value = ApprovalState.Pending(
                                actionType = "CROSS_CHAIN",
                                description = "Cross-chain top-up of ${lifi.amount} ${lifi.fromToken} from ${lifi.fromChain}",
                                lifiRequest = lifi
                            )
                        }
                    }
                }
                _uiState.value = ChatUiState.Idle
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // ── Approve a pending transaction ─────────────────────────
    fun approvePending(signedTxBase64: String) {
        val pending = (_approvalState.value as? ApprovalState.Pending) ?: return
        _approvalState.value = ApprovalState.Processing
        viewModelScope.launch {
            try {
                if (pending.actionType == "TRANSFER") {
                    val result = api.confirmTransfer(
                        ConfirmTransferRequest(signedTxBase64 = signedTxBase64)
                    )
                    val confirmText = "✅ Transaction confirmed. Hash: ${result.txHash}"
                    chatDao.insert(
                        ChatMessageEntity(
                            conversationId = conversationId,
                            role = "bot",
                            text = confirmText
                        )
                    )
                }
                _approvalState.value = ApprovalState.None
            } catch (e: Exception) {
                _approvalState.value = ApprovalState.None
                chatDao.insert(
                    ChatMessageEntity(
                        conversationId = conversationId,
                        role = "bot",
                        text = "❌ Transaction failed: ${e.message}"
                    )
                )
            }
        }
    }

    fun rejectPending() {
        _approvalState.value = ApprovalState.None
        viewModelScope.launch {
            chatDao.insert(
                ChatMessageEntity(
                    conversationId = conversationId,
                    role = "bot",
                    text = "Transaction rejected by user."
                )
            )
        }
    }

    fun dismissError() { _uiState.value = ChatUiState.Idle }

    // ── Helper ────────────────────────────────────────────────
    private fun ChatMessageEntity.toUi() = UiChatMessage(
        id = id,
        role = role,
        text = text,
        audioUrl = audioUrl,
        action = if (actionType != null && actionPayloadJson != null)
            BotAction(
                type = actionType,
                payload = runCatching { gson.fromJson(actionPayloadJson, TransactionPayload::class.java) }.getOrNull()
            )
        else null,
        timestampMs = timestampMs
    )
}
