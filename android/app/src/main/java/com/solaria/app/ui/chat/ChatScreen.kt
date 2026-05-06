package com.solaria.app.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.solaria.app.ui.components.ApprovalCard
import com.solaria.app.ui.components.PinApprovalDialog
import com.solaria.app.ui.theme.SolariaGreen
import com.solaria.app.ui.theme.SolariaPurple
import com.solaria.app.ui.theme.SolariaBlue
import kotlinx.coroutines.launch

// TODO: Voice integration point — add SpeechRecognizer, ExoPlayer (TTS playback),
//  and mic permission launcher here when implementing voice-first features.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    initialMessage: String? = null,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val approvalState by viewModel.approvalState.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var pinDialogSubtitle by remember { mutableStateOf<String?>(null) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val approvalPin = "1234"
    var didSendInitial by remember(initialMessage) { mutableStateOf(false) }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    LaunchedEffect(initialMessage) {
        val message = initialMessage?.trim().orEmpty()
        if (message.isNotBlank() && !didSendInitial) {
            didSendInitial = true
            viewModel.sendMessage(message)
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(SolariaPurple, SolariaBlue))
                    )
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Column {
                        Text(
                            text = "Solaria AI",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                        Text(
                            text = "Connected to Solana DevNet",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        listOf(SolariaPurple.copy(alpha = 0.05f), MaterialTheme.colorScheme.background)
                    )
                )
        ) {
            if (pinDialogSubtitle != null) {
                PinApprovalDialog(
                    title = "Confirm Transaction",
                    subtitle = pinDialogSubtitle ?: "",
                    pin = pinInput,
                    error = pinError,
                    onPinChange = { next ->
                        pinInput = next.filter { it.isDigit() }.take(6)
                        pinError = null
                    },
                    onConfirm = {
                        if (pinInput.length < 4) {
                            pinError = "Enter 4-digit PIN"
                            return@PinApprovalDialog
                        }
                        if (pinInput != approvalPin) {
                            pinError = "Incorrect PIN"
                            return@PinApprovalDialog
                        }
                        pinDialogSubtitle = null
                        pinInput = ""
                        pinError = null
                        viewModel.approvePending("SIGNED_TX_PLACEHOLDER")
                    },
                    onDismiss = {
                        pinDialogSubtitle = null
                        pinInput = ""
                        pinError = null
                        viewModel.rejectPending()
                    }
                )
            }

            // ── Message list ──────────────────────────────────
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (messages.isEmpty()) {
                    item { WelcomePlaceholder() }
                }

                items(messages, key = { it.id }) { msg ->
                    MessageBubble(
                        message = msg,
                        onApprove = { _, description ->
                            pinDialogSubtitle = description
                            pinInput = ""
                            pinError = null
                        },
                        onReject = { viewModel.rejectPending() }
                    )
                }

                if (approvalState is ApprovalState.Pending) {
                    val pending = approvalState as ApprovalState.Pending
                    item {
                        ApprovalCard(
                            actionType = pending.actionType,
                            description = pending.description,
                            onApprove = {
                                pinDialogSubtitle = pending.description
                                pinInput = ""
                                pinError = null
                            },
                            onReject = { viewModel.rejectPending() },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                if (uiState is ChatUiState.Loading) {
                    item { TypingIndicator() }
                }
            }

            HorizontalDivider(color = SolariaGreen.copy(alpha = 0.2f))

            // ── Input row ─────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask about DevNet transactions…", fontSize = 14.sp) },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SolariaGreen,
                        cursorColor = SolariaPurple,
                        unfocusedBorderColor = SolariaBlue.copy(alpha = 0.3f)
                    )
                )

                IconButton(
                    onClick = {
                        val text = inputText.trim()
                        if (text.isNotBlank()) {
                            viewModel.sendMessage(text)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && uiState !is ChatUiState.Loading
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SolariaPurple,
                        modifier = Modifier.size(44.dp),
                        border = BorderStroke(1.dp, SolariaGreen)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: UiChatMessage,
    onApprove: (String, String) -> Unit,
    onReject: () -> Unit
) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = SolariaPurple,
                modifier = Modifier.size(32.dp).align(Alignment.Bottom),
                border = BorderStroke(1.dp, SolariaGreen)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("S", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp, topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                color = if (isUser) SolariaPurple else MaterialTheme.colorScheme.surface,
                border = if (!isUser) BorderStroke(0.5.dp, SolariaGreen.copy(alpha = 0.4f)) else null,
                tonalElevation = if (isUser) 4.dp else 1.dp
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            if (!isUser && message.action != null &&
                (message.action.type == "TRANSFER" || message.action.type == "CROSS_CHAIN")
            ) {
                Spacer(Modifier.height(8.dp))
                val desc = message.action.payload?.let { "Send ${it.amountSol} SOL to ${it.to}" }
                    ?: "Approve cross-chain transfer"
                ApprovalCard(
                    actionType = message.action.type,
                    description = desc,
                    onApprove = { onApprove(message.action.type, desc) },
                    onReject = onReject
                )
            }
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = SolariaBlue,
                modifier = Modifier.size(32.dp).align(Alignment.Bottom),
                border = BorderStroke(1.dp, SolariaGreen)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("U", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun WelcomePlaceholder() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = SolariaPurple.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, SolariaGreen)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("☀️", fontSize = 32.sp)
            }
        }
        Text(
            text = "Welcome to Solaria",
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Your sleek assistant for Solana DevNet. Ask me anything or start a transaction with voice or text.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(start = 40.dp, top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = SolariaPurple)
        Text("Solaria is thinking", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
    }
}
