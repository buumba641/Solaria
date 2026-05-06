package com.solaria.app.ui.buysell

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.solaria.app.ui.theme.*

@Composable
fun BuySellScreen(
    viewModel: BuySellViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showSlippageDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        SolariaPurple.copy(alpha = 0.12f),
                        SolariaBlue.copy(alpha = 0.06f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Swap",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Trade tokens instantly",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Slippage settings
                OutlinedButton(
                    onClick = { showSlippageDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SolariaGreen.copy(alpha = 0.3f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Tune, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${state.slippagePct}%", fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── FROM Token Card ──
            TokenInputCard(
                label = "You pay",
                tokenSymbol = state.fromToken,
                amount = state.fromAmount,
                balance = if (state.fromToken == "SOL") state.solBalance else state.usdcBalance,
                onAmountChange = { viewModel.updateFromAmount(it) },
                onMaxClick = {
                    val max = if (state.fromToken == "SOL")
                        (state.solBalance - state.estimatedFee).coerceAtLeast(0.0)
                    else state.usdcBalance
                    viewModel.updateFromAmount("%.6f".format(max).trimEnd('0').trimEnd('.'))
                }
            )

            // ── Flip Button ──
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val rotation by animateFloatAsState(
                    targetValue = if (state.fromToken == "SOL") 0f else 180f,
                    animationSpec = tween(300),
                    label = "flipRotation"
                )
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.flipTokens() },
                    shape = CircleShape,
                    color = SolariaPurple,
                    shadowElevation = 8.dp,
                    border = BorderStroke(2.dp, SolariaGreen.copy(alpha = 0.6f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.SwapVert,
                            contentDescription = "Flip tokens",
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(rotation)
                        )
                    }
                }
            }

            // ── TO Token Card ──
            TokenInputCard(
                label = "You receive",
                tokenSymbol = state.toToken,
                amount = state.toAmount,
                balance = if (state.toToken == "SOL") state.solBalance else state.usdcBalance,
                readOnly = true,
                onAmountChange = {},
                onMaxClick = {}
            )

            // ── Rate Info ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(0.5.dp, SolariaGreen.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RateRow("Rate", "1 SOL = ${"$%.2f".format(state.solPrice)} USDC")
                    RateRow("Network fee", "~${state.estimatedFee} SOL")
                    RateRow("Slippage tolerance", "${state.slippagePct}%")
                    RateRow("Route", "Jupiter (Simulated on DevNet)")
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Swap Button ──
            val canSwap = state.fromAmount.toDoubleOrNull()?.let { it > 0 } ?: false
            Button(
                onClick = { viewModel.executeSwap() },
                enabled = canSwap && !state.isSwapping,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SolariaPurple,
                    disabledContainerColor = SolariaPurple.copy(alpha = 0.3f)
                )
            ) {
                if (state.isSwapping) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Swapping…", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.SwapHoriz, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (canSwap) "Swap ${state.fromToken} → ${state.toToken}"
                        else "Enter an amount",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // ── Success Snackbar ──
        state.swapSuccess?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = SolariaGreen.copy(alpha = 0.9f),
                action = {
                    TextButton(onClick = { viewModel.dismissMessage() }) {
                        Text("OK", color = Color.White)
                    }
                }
            ) {
                Text("✅ $msg", color = Color.White)
            }
        }

        // ── Error Snackbar ──
        state.swapError?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = SolariaError.copy(alpha = 0.9f),
                action = {
                    TextButton(onClick = { viewModel.dismissMessage() }) {
                        Text("OK", color = Color.White)
                    }
                }
            ) {
                Text("❌ $msg", color = Color.White)
            }
        }

        // ── Slippage Dialog ──
        if (showSlippageDialog) {
            SlippageDialog(
                currentSlippage = state.slippagePct,
                onSelect = { viewModel.setSlippage(it); showSlippageDialog = false },
                onDismiss = { showSlippageDialog = false }
            )
        }
    }
}

@Composable
private fun TokenInputCard(
    label: String,
    tokenSymbol: String,
    amount: String,
    balance: Double,
    readOnly: Boolean = false,
    onAmountChange: (String) -> Unit,
    onMaxClick: () -> Unit
) {
    val tokenColor = if (tokenSymbol == "SOL") SolariaPurple else SolariaBlue
    val tokenIcon = if (tokenSymbol == "SOL") "◎" else "$"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, SolariaGreen.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Balance: ${"%.4f".format(balance)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!readOnly) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = tokenColor.copy(alpha = 0.1f),
                            modifier = Modifier.clickable { onMaxClick() }
                        ) {
                            Text(
                                "MAX",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = tokenColor
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Token badge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = tokenColor.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, tokenColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            tokenIcon,
                            fontSize = 18.sp,
                            color = tokenColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            tokenSymbol,
                            fontWeight = FontWeight.Bold,
                            color = tokenColor
                        )
                    }
                }

                // Amount input
                if (readOnly) {
                    Text(
                        text = amount.ifEmpty { "0.00" },
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (amount.isNotEmpty())
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                } else {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = onAmountChange,
                        modifier = Modifier.weight(1f),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.End
                        ),
                        placeholder = {
                            Text(
                                "0.00",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = SolariaPurple
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RateRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SlippageDialog(
    currentSlippage: Double,
    onSelect: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(0.1, 0.5, 1.0, 2.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Slippage Tolerance", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Maximum price difference you're willing to accept.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { pct ->
                        val isSelected = pct == currentSlippage
                        OutlinedButton(
                            onClick = { onSelect(pct) },
                            colors = if (isSelected) ButtonDefaults.buttonColors(
                                containerColor = SolariaPurple
                            ) else ButtonDefaults.outlinedButtonColors(),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) SolariaGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "$pct%",
                                fontSize = 13.sp,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
