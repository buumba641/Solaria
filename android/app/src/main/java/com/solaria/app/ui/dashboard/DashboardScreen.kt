package com.solaria.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.solaria.app.data.db.TransactionEntity
import com.solaria.app.ui.components.AiChatEntryBar
import com.solaria.app.ui.components.PendingSyncBanner
import com.solaria.app.ui.components.SyncStatusIndicator
import com.solaria.app.ui.theme.SolariaGreen

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val isScrolling by remember { derivedStateOf { listState.isScrollInProgress } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Sync status row
            item {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SyncStatusIndicator(
                        label = "Wallet",
                        syncAge = state.lastWalletSync
                    )
                    state.solPrice?.let {
                        SyncStatusIndicator(
                            label = "Prices",
                            syncAge = formatAge(it.updatedAtMs)
                        )
                    }
                }
            }

            // Pending sync banner
            if (state.pendingSyncCount > 0) {
                item {
                    PendingSyncBanner(
                        count = state.pendingSyncCount,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }

            item {
                BalanceCard(
                    balanceSol = state.wallet?.balanceSol ?: 0.0,
                    portfolioUsd = state.portfolioValueUsd,
                    lastSynced = state.lastWalletSync
                )
            }

            item {
                state.solPrice?.let { sol ->
                    TokenPerformanceCard(
                        symbol = "SOL",
                        name = "Solana",
                        price = "${"$%.2f".format(sol.price)}",
                        change = "${if (sol.change24h >= 0) "+" else ""}${"%.2f".format(sol.change24h)}%",
                        isPositive = sol.change24h >= 0
                    )
                }
            }

            if (state.recentTransactions.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Recent activity",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(state.recentTransactions.take(5)) { tx ->
                    TransactionRow(tx)
                }
            }
        }

        AiChatEntryBar(
            navController = navController,
            isCollapsed = isScrolling,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
private fun BalanceCard(balanceSol: Double, portfolioUsd: Double, lastSynced: String) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Last updated: $lastSynced",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            Text(
                text = "Main balance",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Text(
                text = "${"%.4f".format(balanceSol)} SOL",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "≈ ${"$%.2f".format(portfolioUsd)} USD portfolio",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AssistChip(
                    onClick = { },
                    label = { Text("Portfolio insights") },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = null,
                    leadingIcon = { Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp)) }
                )
            }
        }
    }
}

@Composable
private fun TokenPerformanceCard(
    symbol: String, name: String, price: String, change: String, isPositive: Boolean
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = symbol.take(1),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column {
                    Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = symbol, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = price, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = change,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isPositive) SolariaGreen else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: TransactionEntity) {
    val isReceive = tx.type.contains("RECEIVE")
    val amount = "${if (isReceive) "+" else "-"}${"%.4f".format(tx.amountSol)} SOL"
    val statusBadge = when (tx.status) {
        "PENDING_SYNC" -> " 🔄"
        "PENDING" -> " ⏳"
        "FAILED" -> " ❌"
        else -> ""
    }

    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = (tx.description ?: tx.type) + statusBadge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = tx.paymentMethod + " • " + formatAge(tx.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                amount,
                fontWeight = FontWeight.Bold,
                color = if (isReceive) SolariaGreen else MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun formatAge(timestampMs: Long): String {
    val diff = System.currentTimeMillis() - timestampMs
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> "${diff / 86_400_000}d ago"
    }
}
