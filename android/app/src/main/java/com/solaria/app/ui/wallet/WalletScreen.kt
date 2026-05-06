package com.solaria.app.ui.wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.solaria.app.ui.theme.SolariaGreen
import com.solaria.app.ui.theme.SolariaGreenDark
import com.solaria.app.ui.theme.SolariaGreenLight

@Composable
fun WalletScreen(
    navController: NavController,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // ── Wallet Header ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(SolariaGreen, SolariaGreenDark)))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Text("Wallet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.weight(1f))
                    if (state.isConnected) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.2f)) {
                            Text("DevNet", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color.White, fontSize = 11.sp)
                        }
                    }
                }

                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Filled.AccountBalanceWallet, null, tint = Color.White, modifier = Modifier.padding(16.dp))
                }

                if (state.isConnected) {
                    // Address with copy button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val addr = state.walletAddress
                        Text(
                            "${addr.take(6)}…${addr.takeLast(6)}",
                            color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clip.setPrimaryClip(ClipData.newPlainText("address", addr))
                                Toast.makeText(context, "Address copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, "Copy", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        if (state.hasLocalKeypair) {
                            Spacer(Modifier.width(4.dp))
                            Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(alpha = 0.2f)) {
                                Text("🔑 Local Key", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }

                    if (state.isLoading || state.isAirdropping) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(
                                if (state.isAirdropping) "Requesting airdrop…" else "Refreshing…",
                                color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp
                            )
                        }
                    } else {
                        Text("${"%.4f".format(state.balanceSol)} SOL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                    }
                    Text("Last synced: ${state.lastSyncedAt}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                } else {
                    Text("Wallet not connected", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Connected Wallet Actions ───────────────────────────
        if (state.isConnected) {
            // Action buttons row
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Refresh Balance
                    OutlinedButton(
                        onClick = { viewModel.refreshBalance() },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isLoading
                    ) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Refresh", fontSize = 12.sp)
                    }
                    // Airdrop
                    Button(
                        onClick = { viewModel.requestAirdrop(1.0) },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isAirdropping && !state.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = SolariaGreen)
                    ) {
                        Icon(Icons.Filled.Download, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Airdrop", fontSize = 12.sp)
                    }
                }
            }

            // Send SOL Card
            if (state.hasLocalKeypair) {
                SendSolCard(
                    isSending = state.isSending,
                    onSend = { toAddr, amount -> viewModel.sendSol(toAddr, amount) }
                )
            }

            // SPL Token list
            if (state.tokens.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("SPL Tokens", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        state.tokens.forEach { token ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(token.symbol, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text(token.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${"%.4f".format(token.amount)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    token.usdValue?.let {
                                        Text("${"$%.2f".format(it)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Disconnect
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.onWalletDisconnected() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.LinkOff, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Disconnect Wallet")
                    }
                }
            }
        } else {
            // ── Not Connected ──────────────────────────────────
            // Generate New Wallet
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Create a Wallet", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "Generate a new Solana keypair on this device. Your private key is stored securely and never leaves the phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { viewModel.generateNewWallet() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SolariaGreen),
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Generate New Wallet")
                    }
                }
            }

            // Import Existing Wallet
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                ImportWalletSection(
                    isLoading = state.isLoading,
                    onImport = { viewModel.importWallet(it) },
                    onConnect = { viewModel.onWalletConnected(it) }
                )
            }
        }

        // Security
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Security", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, null, tint = SolariaGreen)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("PIN Approval", fontSize = 14.sp)
                            Text("Require PIN entry for transfers", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = state.pinEnabled,
                        onCheckedChange = { viewModel.togglePin(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = SolariaGreen, checkedTrackColor = SolariaGreenLight)
                    )
                }
            }
        }

        // About
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("About", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                InfoRow("App", "Solaria v1.0 (Real DevNet)")
                InfoRow("Network", "Solana DevNet")
                InfoRow("RPC", "api.devnet.solana.com")
                InfoRow("Key Storage", "EncryptedSharedPreferences")
            }
        }

        // Success message
        state.successMessage?.let { msg ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = { TextButton(onClick = { viewModel.dismissSuccess() }) { Text("OK") } },
                containerColor = SolariaGreen
            ) { Text(msg, color = Color.White) }
        }

        // Error message
        state.error?.let { err ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = { TextButton(onClick = { viewModel.dismissError() }) { Text("Dismiss") } }
            ) { Text("Error: $err") }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SendSolCard(isSending: Boolean, onSend: (String, Double) -> Unit) {
    var toAddress by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Send SOL", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Send real SOL on DevNet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = toAddress,
                onValueChange = { toAddress = it },
                label = { Text("Recipient address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount (SOL)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Button(
                onClick = {
                    val sol = amount.toDoubleOrNull() ?: return@Button
                    if (toAddress.isNotBlank() && sol > 0) onSend(toAddress, sol)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSending && toAddress.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = SolariaGreen)
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Sending…")
                } else {
                    Icon(Icons.Filled.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Send SOL")
                }
            }
        }
    }
}

@Composable
private fun ImportWalletSection(isLoading: Boolean, onImport: (String) -> Unit, onConnect: (String) -> Unit) {
    var mode by remember { mutableStateOf(0) } // 0 = address (watch), 1 = private key (full)
    var input by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Import or Watch", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == 0,
                onClick = { mode = 0; input = "" },
                label = { Text("Watch Address") }
            )
            FilterChip(
                selected = mode == 1,
                onClick = { mode = 1; input = "" },
                label = { Text("Import Key") }
            )
        }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text(if (mode == 0) "Wallet address" else "Private key (Base58)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Button(
            onClick = {
                if (input.isNotBlank()) {
                    if (mode == 0) onConnect(input) else onImport(input)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && input.isNotBlank()
        ) {
            Text(if (mode == 0) "Watch Wallet" else "Import & Connect")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
