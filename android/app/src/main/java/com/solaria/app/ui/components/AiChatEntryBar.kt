package com.solaria.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.navigation.NavController

// TODO: Voice integration point — when implementing voice-first features,
//  add SpeechRecognizer, mic permission launcher, and startListening() helper here.
//  The mic button should be added to AiInputRow alongside the text field.

@Composable
fun AiChatEntryBar(
    navController: NavController,
    isCollapsed: Boolean,
    onSendMessage: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var isManuallyExpanded by remember { mutableStateOf(true) }
    var isChatWindowOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Once scrolling starts, force shrink and don't auto-expand
    LaunchedEffect(isCollapsed) {
        if (isCollapsed) {
            isManuallyExpanded = false
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
    onSendClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // TODO: Voice integration point — add mic IconButton here
        //  before the text field for voice input activation.

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
            enabled = inputText.isNotBlank(),
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
