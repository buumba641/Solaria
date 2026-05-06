package com.solaria.app.ui.market

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.solaria.app.data.db.NftCollectionEntity
import com.solaria.app.data.db.PriceCacheEntity
import com.solaria.app.ui.components.AiChatEntryBar
import com.solaria.app.ui.components.SyncStatusIndicator
import com.solaria.app.ui.theme.SolariaGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    navController: NavController,
    viewModel: MarketViewModel = hiltViewModel()
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Market", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Live prices and demand insights", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Sync status
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SyncStatusIndicator(label = "Prices", syncAge = state.lastPriceSync)
                    SyncStatusIndicator(label = "NFTs", syncAge = state.lastNftSync)
                }
            }

            item { MarketInsightsCard() }
            item { InventoryTrackerCard() }

            // Search
            item {
                var query by remember { mutableStateOf(state.searchQuery) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it; viewModel.onSearchQuery(it) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search token (SOL, BONK, JUP)", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.searchToken(query) }),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SolariaGreen, cursorColor = SolariaGreen)
                    )
                }
            }

            // Token data
            item {
                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SolariaGreen)
                    }
                } else {
                    state.tokenData?.let { TokenPerformanceCard(token = it) }
                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp), fontSize = 13.sp)
                    }
                }
            }

            // NFT Collections
            item {
                Text("Top NFT collections", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp))
            }
            items(state.nftCollections) { nft -> NftCollectionCard(nft = nft) }
        }

        AiChatEntryBar(
            navController = navController,
            isCollapsed = isScrolling,
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
private fun MarketInsightsCard() {
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("AI Market Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
            Text("Rice demand up 18% today. Restock suggested soon.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InventoryTrackerCard() {
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Live Inventory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            InventoryRow("Tomatoes", "Low", 0.2f)
            InventoryRow("Palm oil", "Healthy", 0.72f)
        }
    }
}

@Composable
private fun InventoryRow(name: String, status: String, fill: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, fontWeight = FontWeight.SemiBold)
            Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { fill },
            color = SolariaGreen,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TokenPerformanceCard(token: PriceCacheEntity) {
    val isPositive = token.change24h >= 0
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(token.symbol, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("${"$%.2f".format(token.price)}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Text(
                text = "${if (isPositive) "+" else ""}${"%.2f".format(token.change24h)}%",
                color = if (isPositive) SolariaGreen else MaterialTheme.colorScheme.error,
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Updated: ${formatAge(token.updatedAtMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NftCollectionCard(nft: NftCollectionEntity) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(nft.name.take(1), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(nft.name, fontWeight = FontWeight.SemiBold)
                Text(nft.symbol, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("SOL ${nft.floorPrice}", fontWeight = FontWeight.Bold)
                nft.change24h?.let {
                    Text(
                        "${if (it >= 0) "+" else ""}${"%.1f".format(it)}%",
                        fontSize = 12.sp,
                        color = if (it >= 0) SolariaGreen else MaterialTheme.colorScheme.error
                    )
                }
            }
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
