package com.solaria.app.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import java.util.Locale

import androidx.navigation.NavController

@Composable
fun AiChatEntryBar(
    navController: NavController,
    isCollapsed: Boolean,
    onSendMessage: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var isManuallyExpanded by remember { mutableStateOf(true) }
    var isChatWindowOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Once scrolling starts, force shrink and don't auto-expand
    LaunchedEffect(isCollapsed) {
        if (isCollapsed) {
            isManuallyExpanded = false
        }
    }

    // Speech Recognizer
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    DisposableEffect(Unit) { onDispose { speechRecognizer.destroy() } }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isListening = true
            startListening(speechRecognizer) { recognized ->
                if (recognized.isNotBlank()) {
                    inputText = recognized
                    isChatWindowOpen = true
                }
                isListening = false
            }
        }
    }

    val actualCollapsed = !isManuallyExpanded && !isChatWindowOpen

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Chat Window Overlay ───────────────────────────────
        AnimatedVisibility(
            visible = isChatWindowOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isChatWindowOpen = false }
            ) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.75f)
                        .clickable(enabled = false) { }, // Prevent clicks through to background
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header
                        Row(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Solaria AI",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { isChatWindowOpen = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        }

                        // Fake chat content for demo purposes
                        Box(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (inputText.isNotBlank()) {
                                    ChatBubble(text = inputText, isUser = true)
                                    ChatBubble(text = "I'm analyzing that for you. It looks like your SOL portfolio is performing well with a 5.2% increase today.", isUser = false)
                                } else {
                                    Text(
                                        "How can I help you today?",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Input field inside window
                        Box(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth()
                        ) {
                            AiInputRow(
                                inputText = inputText,
                                onValueChange = { inputText = it },
                                isListening = isListening,
                                onMicClick = { 
                                    // mic logic
                                },
                                onSendClick = {
                                    if (inputText.isNotBlank()) {
                                        if (onSendMessage != null) {
                                            onSendMessage(inputText)
                                        } else {
                                            navController.navigate("ai_chat?initialMessage=$inputText")
                                            isChatWindowOpen = false
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // ── Bottom Bar / Shrunk Icon ───────────────────────────
        if (!isChatWindowOpen) {
            Box(
                modifier = modifier
                    .align(Alignment.BottomCenter)
            ) {
                AnimatedContent(
                    targetState = actualCollapsed,
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                    }
                ) { collapsed ->
                    if (collapsed) {
                        // Shrunk Icon
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 8.dp,
                            onClick = { isManuallyExpanded = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = "Expand", tint = Color.White)
                            }
                        }
                    } else {
                        // Expanded Bar
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            shape = RoundedCornerShape(32.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            shadowElevation = 12.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            AiInputRow(
                                inputText = inputText,
                                onValueChange = { inputText = it },
                                isListening = isListening,
                                onMicClick = {
                                    val hasMic = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasMic) {
                                        if (isListening) {
                                            speechRecognizer.stopListening()
                                            isListening = false
                                        } else {
                                            isListening = true
                                            startListening(speechRecognizer) { recognized ->
                                                if (recognized.isNotBlank()) {
                                                    inputText = recognized
                                                    isChatWindowOpen = true
                                                }
                                                isListening = false
                                            }
                                        }
                                    } else {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                onSendClick = {
                                    if (inputText.isNotBlank()) {
                                        isChatWindowOpen = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (isChatWindowOpen) {
        BackHandler { isChatWindowOpen = false }
    }
}

@Composable
private fun AiInputRow(
    inputText: String,
    onValueChange: (String) -> Unit,
    isListening: Boolean,
    onMicClick: () -> Unit,
    onSendClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onMicClick) {
            Icon(
                imageVector = if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = "Voice",
                tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }

        OutlinedTextField(
            value = inputText,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Ask Solaria AI...", style = MaterialTheme.typography.bodyMedium) },
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            )
        )

        IconButton(
            onClick = onSendClick,
            enabled = inputText.isNotBlank() || isListening,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        ) {
            Icon(Icons.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ChatBubble(text: String, isUser: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

private fun startListening(
    recognizer: SpeechRecognizer,
    onResult: (String) -> Unit
) {
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
