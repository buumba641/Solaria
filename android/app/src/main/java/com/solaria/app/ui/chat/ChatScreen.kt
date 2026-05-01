package com.solaria.app.ui.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import com.solaria.app.ui.components.ApprovalCard
import com.solaria.app.ui.components.showBiometricPrompt
import com.solaria.app.ui.theme.SolariaGreen
import com.solaria.app.ui.theme.SolariaGreenLight
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val messages by viewModel.messages.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val approvalState by viewModel.approvalState.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // ExoPlayer for TTS audio playback
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    // Speech Recognizer
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    DisposableEffect(Unit) { onDispose { speechRecognizer.destroy() } }

    // Mic permission launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening(speechRecognizer, isListening = true) { recognized ->
            inputText = recognized
            isListening = false
        }
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    // Play audio when a bot message with audioUrl arrives
    LaunchedEffect(messages) {
        val lastBot = messages.lastOrNull { it.role == "bot" && it.audioUrl != null }
        lastBot?.audioUrl?.let { url ->
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    Scaffold(
        topBar = {
            // mifos-pay-style gradient top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(SolariaGreen, SolariaGreenLight))
                    )
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Solaria",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Text(
                        text = "Your Solana AI assistant",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Message list ──────────────────────────────────
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item { WelcomePlaceholder() }
                }

                items(messages, key = { it.id }) { msg ->
                    MessageBubble(
                        message = msg,
                        onApprove = { actionType, description ->
                            if (activity != null) {
                                showBiometricPrompt(
                                    activity = activity,
                                    title = "Confirm Transaction",
                                    subtitle = description,
                                    onSuccess = {
                                        // TODO: Replace with real MWA signing:
                                        //   val scenario = transact(activity) { it.signTransactions(...) }
                                        //   val signedTxBase64 = Base64.encode(scenario.signedPayloads[0])
                                        viewModel.approvePending("SIGNED_TX_PLACEHOLDER")
                                    },
                                    onFailure = { viewModel.rejectPending() }
                                )
                            }
                        },
                        onReject = { viewModel.rejectPending() }
                    )
                }

                // Pending approval card (floating at bottom of list)
                if (approvalState is ApprovalState.Pending) {
                    val pending = approvalState as ApprovalState.Pending
                    item {
                        ApprovalCard(
                            actionType = pending.actionType,
                            description = pending.description,
                            onApprove = {
                                if (activity != null) {
                                    showBiometricPrompt(
                                        activity = activity,
                                        title = "Confirm Transaction",
                                        subtitle = pending.description,
                                        onSuccess = {
                                            // TODO: Replace with real MWA signing:
                                            //   transact(activity) { it.signTransactions(...) }
                                            viewModel.approvePending("SIGNED_TX_PLACEHOLDER")
                                        },
                                        onFailure = { viewModel.rejectPending() }
                                    )
                                }
                            },
                            onReject = { viewModel.rejectPending() },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                // Loading indicator
                if (uiState is ChatUiState.Loading) {
                    item { TypingIndicator() }
                }
            }

            // ── Error snackbar ────────────────────────────────
            if (uiState is ChatUiState.Error) {
                val errMsg = (uiState as ChatUiState.Error).message
                LaunchedEffect(errMsg) {
                    scope.launch {
                        // Brief display then dismiss
                        kotlinx.coroutines.delay(3000)
                        viewModel.dismissError()
                    }
                }
                Snackbar(
                    modifier = Modifier.padding(8.dp),
                    action = {
                        TextButton(onClick = { viewModel.dismissError() }) { Text("Dismiss") }
                    }
                ) { Text("Error: $errMsg") }
            }

            HorizontalDivider()

            // ── Input row (mifos-pay bottom compose bar style) ─
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask about your portfolio…", fontSize = 14.sp) },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SolariaGreen,
                        cursorColor = SolariaGreen
                    )
                )

                // Mic button
                IconButton(
                    onClick = {
                        val hasMic = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasMic) {
                            if (isListening) {
                                speechRecognizer.stopListening()
                                isListening = false
                            } else {
                                isListening = true
                                startListening(speechRecognizer, isListening = true) { recognized ->
                                    inputText = recognized
                                    isListening = false
                                }
                            }
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isListening) MaterialTheme.colorScheme.error else SolariaGreen,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                            contentDescription = "Voice input",
                            tint = Color.White,
                            modifier = Modifier.padding(9.dp)
                        )
                    }
                }

                // Send button
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
                        color = SolariaGreen,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.padding(9.dp)
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Message bubble – user (right, green) / bot (left, surface)
// Mirrors mifos-pay chat/transaction item design
// ──────────────────────────────────────────────────────────────
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
            // Bot avatar
            Surface(
                shape = CircleShape,
                color = SolariaGreen,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.Bottom)
            ) {
                Text(
                    text = "S",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.wrapContentSize(Alignment.Center)
                )
            }
            Spacer(Modifier.width(6.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) SolariaGreen else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            // Inline approval card for bot action messages
            if (!isUser && message.action != null &&
                (message.action.type == "TRANSFER" || message.action.type == "CROSS_CHAIN")
            ) {
                Spacer(Modifier.height(4.dp))
                val actionType = message.action.type
                val desc = message.action.payload?.let { "Send ${it.amountSol} SOL to ${it.to}" }
                    ?: "Approve cross-chain transfer"
                ApprovalCard(
                    actionType = actionType,
                    description = desc,
                    onApprove = { onApprove(actionType, desc) },
                    onReject = onReject
                )
            }
        }

        if (isUser) {
            Spacer(Modifier.width(6.dp))
            Surface(
                shape = CircleShape,
                color = SolariaGreen,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.Bottom)
            ) {
                Text(
                    text = "U",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.wrapContentSize(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun WelcomePlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("👋", fontSize = 40.sp)
        Text(
            text = "Hi! I'm your Solana assistant.",
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Ask me about your balance, SOL performance, top NFTs, or say \"Send 0.1 SOL to…\"",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(start = 40.dp, top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Solaria is thinking", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
    }
}

// ── Speech helper ─────────────────────────────────────────────
private fun startListening(
    recognizer: SpeechRecognizer,
    isListening: Boolean,
    onResult: (String) -> Unit
) {
    if (!isListening) return
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
    }
    recognizer.setRecognitionListener(object : RecognitionListener {
        override fun onResults(bundle: android.os.Bundle?) {
            val results = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            onResult(results?.firstOrNull() ?: "")
        }
        override fun onError(error: Int) { onResult("") }
        override fun onReadyForSpeech(params: android.os.Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: android.os.Bundle?) {}
        override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
    })
    recognizer.startListening(intent)
}
