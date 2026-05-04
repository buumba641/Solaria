package com.solaria.app.ui.wallet

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
import androidx.compose.ui.text.font.FontWeight
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Wallet header card
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
                }

                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Filled.AccountBalanceWallet, null, tint = Color.White, modifier = Modifier.padding(16.dp))
                }

                if (state.isConnected) {
                    val addr = state.walletAddress
                    Text(
                        "${addr.take(6)}…${addr.takeLast(6)}",
                        color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp
                    )
                    if (state.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("${"%.4f".format(state.balanceSol)} SOL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                    }
                    Text("Last synced: ${state.lastSyncedAt}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                } else {
                    Text("Wallet not connected", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                }
            }
        }

        // SPL Token list
        if (state.tokens.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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

        Spacer(Modifier.height(8.dp))

        // Connect / Disconnect
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Solana Wallet", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                if (state.isConnected) {
                    OutlinedButton(onClick = { viewModel.refreshBalance() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Refresh Balance")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.onWalletDisconnected() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.LinkOff, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Disconnect Wallet")
                    }
                } else {
                    var demoAddress by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = demoAddress,
                        onValueChange = { demoAddress = it },
                        label = { Text("Wallet address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (demoAddress.isNotBlank()) viewModel.onWalletConnected(demoAddress)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SolariaGreen)
                    ) {
                        Icon(Icons.Filled.Link, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Connect via MWA")
                    }
                }
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
                InfoRow("App", "Solaria v1.0 (Offline-First)")
                InfoRow("Network", "Solana Devnet")
                InfoRow("Storage", "Local Room Database")
            }
        }

        if (state.error != null) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = { TextButton(onClick = { viewModel.dismissError() }) { Text("Dismiss") } }
            ) { Text("Error: ${state.error}") }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
