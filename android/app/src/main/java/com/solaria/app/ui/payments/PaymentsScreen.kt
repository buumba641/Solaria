package com.solaria.app.ui.payments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.solaria.app.data.db.PaymentMethodEntity
import com.solaria.app.data.db.TransactionEntity
import com.solaria.app.ui.components.AiChatEntryBar
import com.solaria.app.ui.components.PendingSyncBanner
import com.solaria.app.ui.components.SyncStatusIndicator
import com.solaria.app.ui.theme.SolariaGreen

@Composable
fun PaymentsScreen(
    navController: NavController,
    viewModel: PaymentsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val isScrolling by remember { derivedStateOf { listState.isScrollInProgress } }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Send", "Receive", "P2P")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Payments", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Send, receive, or P2P transfer", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Pending sync
            if (state.pendingSyncCount > 0) {
                item {
                    PendingSyncBanner(count = state.pendingSyncCount, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            // Tabs
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    tabs.forEachIndexed { i, label ->
                        val icon = when (i) {
                            0 -> Icons.Filled.CallMade
                            1 -> Icons.Filled.CallReceived
                            else -> Icons.Filled.Wifi
                        }
                        OutlinedButton(
                            onClick = { selectedTab = i },
                            modifier = Modifier.weight(1f),
                            colors = if (selectedTab == i) ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White
                            ) else ButtonDefaults.outlinedButtonColors(),
                            border = if (selectedTab == i) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(icon, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(label)
                        }
                    }
                }
            }

            // P2P Section
            if (selectedTab == 2) {
                item {
                    P2PSection()
                }
            }

            // Payment Methods
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Payment methods", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            items(state.paymentMethods) { method ->
                PaymentMethodRow(method)
            }

            // QR Section
            item {
                Surface(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                            Text("QR code payments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Text("Generate or scan a QR for fast in-person payments.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { }) { Text("Generate") }
                            Button(onClick = { }) { Text("Scan QR") }
                        }
                    }
                }
            }

            // Transaction history
            item {
                Text("Recent transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp))
            }

            val filteredTxs = when (selectedTab) {
                0 -> state.transactions.filter { it.type.contains("SEND") }
                1 -> state.transactions.filter { it.type.contains("RECEIVE") }
                2 -> state.transactions.filter { it.type.startsWith("P2P") }
                else -> state.transactions
            }

            items(filteredTxs.take(10)) { tx ->
                TransactionHistoryRow(tx)
            }

            if (filteredTxs.isEmpty()) {
                item {
                    Text(
                        "No ${tabs[selectedTab].lowercase()} transactions yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Success/Error snackbar
        state.successMessage?.let { msg ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).padding(bottom = 80.dp),
                action = { TextButton(onClick = { viewModel.dismissMessage() }) { Text("OK") } }
            ) { Text(msg) }
        }

        AiChatEntryBar(
            navController = navController,
            isCollapsed = isScrolling,
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
private fun P2PSection() {
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Wifi, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("P2P Nearby Transfer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                "Send or receive SOL with nearby Solaria devices — no internet needed. " +
                "Transactions are queued locally and synced to the blockchain when either device goes online.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { /* P2P discovery placeholder */ },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Find nearby devices")
            }
        }
    }
}

@Composable
private fun PaymentMethodRow(method: PaymentMethodEntity) {
    val icon = when (method.iconType) {
        "solana" -> Icons.Filled.CurrencyBitcoin
        "mobile_money" -> Icons.Filled.PhoneAndroid
        "bank" -> Icons.Filled.AccountBalance
        else -> Icons.Filled.Payment
    }
    val statusColor = when {
        !method.isActive -> MaterialTheme.colorScheme.outline
        method.isPlaceholder -> MaterialTheme.colorScheme.tertiary
        else -> SolariaGreen
    }

    Surface(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = statusColor.copy(alpha = 0.1f)) {
                Icon(icon, null, modifier = Modifier.padding(10.dp), tint = statusColor)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(method.name, fontWeight = FontWeight.SemiBold)
                Text(
                    if (method.isPlaceholder) "Placeholder — coming soon"
                    else method.accountInfo ?: method.provider,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (method.isPlaceholder) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text("Demo", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Surface(shape = RoundedCornerShape(8.dp), color = SolariaGreen.copy(alpha = 0.1f)) {
                    Text("Active", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, color = SolariaGreen)
                }
            }
        }
    }
}

@Composable
private fun TransactionHistoryRow(tx: TransactionEntity) {
    val isReceive = tx.type.contains("RECEIVE")
    val amount = "${if (isReceive) "+" else "-"}${"%.4f".format(tx.amountSol)} SOL"
    val statusIcon = when (tx.status) {
        "PENDING_SYNC" -> "🔄"
        "PENDING" -> "⏳"
        "FAILED" -> "❌"
        "CONFIRMED" -> "✅"
        else -> ""
    }

    Surface(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text("$statusIcon ${tx.description ?: tx.type}", fontWeight = FontWeight.SemiBold)
                Text(
                    "${tx.paymentMethod} • ${tx.status}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(amount, fontWeight = FontWeight.Bold, color = if (isReceive) SolariaGreen else MaterialTheme.colorScheme.error)
        }
    }
}
