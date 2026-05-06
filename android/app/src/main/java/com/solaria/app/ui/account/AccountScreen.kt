package com.solaria.app.ui.account

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
import com.solaria.app.ui.components.AiChatEntryBar
import com.solaria.app.ui.wallet.WalletViewModel

@Composable
fun AccountScreen(
    navController: NavController,
    walletViewModel: WalletViewModel = hiltViewModel()
) {
    val walletState by walletViewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Profile header
            Surface(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
                                )
                            )
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(80.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.AccountCircle, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Solaria Merchant", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        if (walletState.isConnected) {
                            val addr = walletState.walletAddress
                            Text(
                                "${addr.take(6)}…${addr.takeLast(4)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text("No wallet connected", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(
                        onClick = { navController.navigate("wallet") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Text(if (walletState.isConnected) "Manage Wallet" else "Connect Wallet")
                    }
                }
            }

            // Wallet info card
            if (walletState.isConnected) {
                Surface(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Wallet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        InfoRow("Balance", "${"%.4f".format(walletState.balanceSol)} SOL")
                        InfoRow("Tokens", "${walletState.tokens.size} SPL tokens")
                        InfoRow("Last synced", walletState.lastSyncedAt)
                    }
                }
            }

            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            AccountSection("Security", "Biometrics and recovery", Icons.Filled.Lock, MaterialTheme.colorScheme.primary)
            AccountSection("Language", "English, Swahili, Yoruba", Icons.Filled.Language, MaterialTheme.colorScheme.secondary)
            AccountSection("Support", "24/7 AI and human help", Icons.Filled.SupportAgent, MaterialTheme.colorScheme.tertiary)
            AccountSection("Developer Setup", "CLI, Anchor, and environment tools", Icons.Filled.Terminal, MaterialTheme.colorScheme.primary)

            // Offline info
            Surface(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.CloudDone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text("Offline-First", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "All data is stored locally and works without internet. " +
                        "Connect to sync the latest prices, NFTs, and broadcast pending transactions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    InfoRow("App Version", "Solaria v1.0")
                    InfoRow("Network", "Solana Devnet")
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        AiChatEntryBar(
            navController = navController,
            isCollapsed = scrollState.value > 0,
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
private fun AccountSection(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        onClick = { }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.1f)) {
                Icon(icon, null, modifier = Modifier.padding(12.dp), tint = color)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
