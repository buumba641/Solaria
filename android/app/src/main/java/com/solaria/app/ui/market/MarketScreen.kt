package com.solaria.app.ui.market

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.solaria.app.data.models.NftCollection
import com.solaria.app.data.models.PricePoint
import com.solaria.app.data.models.TokenPerformanceResponse
import com.solaria.app.ui.theme.CryptoGreen
import com.solaria.app.ui.theme.CryptoPurple
import com.solaria.app.ui.theme.CryptoRed
import com.solaria.app.ui.theme.SolariaGreen
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(viewModel: MarketViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // ── Header bar ──────────────────────────────────────
        item {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Market Analysis",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Text(
                        "Token performance & top NFTs",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // ── Token search bar (Crypto-KMP search style) ──────
        item {
            var query by remember { mutableStateOf(state.searchQuery) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        viewModel.onSearchQuery(it)
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Symbol or mint address…", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.searchToken(query) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SolariaGreen,
                        cursorColor = SolariaGreen
                    )
                )
                IconButton(onClick = { viewModel.searchToken(query) }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = SolariaGreen)
                }
            }
        }

        // ── Token performance card (Crypto-KMP style) ───────
        item {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SolariaGreen)
                }
            } else {
                state.tokenData?.let { token ->
                    TokenPerformanceCard(token = token)
                }
            }
        }

        // ── Error message ────────────────────────────────────
        if (state.error != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(state.error ?: "", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        // ── Top NFT collections header ───────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🖼  Top NFT Collections",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // ── NFT grid cards ────────────────────────────────────
        if (state.nftCollections.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SolariaGreen)
                }
            }
        }

        items(state.nftCollections) { nft ->
            NftCollectionCard(nft = nft)
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Token performance card – Crypto-KMP inspired design:
// dark card, price + change chip, inline line chart
// ──────────────────────────────────────────────────────────────
@Composable
private fun TokenPerformanceCard(token: TokenPerformanceResponse) {
    val isPositive = token.change24h >= 0
    val changeColor = if (isPositive) CryptoGreen else CryptoRed
    val fmt = NumberFormat.getCurrencyInstance(Locale.US)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Symbol + price row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        token.symbol,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    token.mint?.let {
                        Text(
                            it.take(8) + "…",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        fmt.format(token.price),
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Change chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = changeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${if (isPositive) "▲" else "▼"} ${String.format("%.2f", kotlin.math.abs(token.change24h))}%",
                            color = changeColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 7-day price chart (Compose Canvas line chart)
            token.history?.takeIf { it.size >= 2 }?.let { history ->
                Text(
                    "7-Day Price",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                LineChart(
                    pricePoints = history,
                    lineColor = if (isPositive) CryptoGreen else CryptoRed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Stats row (volume + market cap)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                token.volume24h?.let {
                    StatChip(label = "Vol 24h", value = formatLargeNumber(it))
                }
                token.marketCap?.let {
                    StatChip(label = "Mkt Cap", value = formatLargeNumber(it))
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

// ──────────────────────────────────────────────────────────────
// Simple Compose Canvas line chart (Crypto-KMP inspired)
// ──────────────────────────────────────────────────────────────
@Composable
private fun LineChart(
    pricePoints: List<PricePoint>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val prices = pricePoints.map { it.price }
        val minP = prices.min()
        val maxP = prices.max()
        val range = if (maxP == minP) 1.0 else (maxP - minP)
        val step = size.width / (prices.size - 1).toFloat()

        val points = prices.mapIndexed { i, p ->
            Offset(
                x = i * step,
                y = size.height - ((p - minP) / range * size.height).toFloat()
            )
        }

        // Fill area under curve
        val path = Path().apply {
            moveTo(points.first().x, size.height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, size.height)
            close()
        }
        drawPath(path, color = lineColor.copy(alpha = 0.15f))

        // Draw line
        for (i in 0 until points.size - 1) {
            drawLine(
                color = lineColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
        // Data point dots
        points.forEach {
            drawCircle(color = lineColor, radius = 4f, center = it)
        }
    }
}

// ──────────────────────────────────────────────────────────────
// NFT Collection card – mifos-pay item card style
// ──────────────────────────────────────────────────────────────
@Composable
private fun NftCollectionCard(nft: NftCollection) {
    val isPositive = (nft.change24h ?: 0.0) >= 0
    val changeColor = if (isPositive) CryptoGreen else CryptoRed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Collection image
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CryptoPurple.copy(alpha = 0.15f),
                modifier = Modifier.size(56.dp)
            ) {
                if (nft.image != null) {
                    AsyncImage(
                        model = nft.image,
                        contentDescription = nft.name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Filled.Image,
                        contentDescription = null,
                        tint = CryptoPurple,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    nft.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    nft.symbol,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "◎ ${String.format("%.2f", nft.floorPrice)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                nft.change24h?.let {
                    Text(
                        text = "${if (isPositive) "▲" else "▼"} ${String.format("%.1f", kotlin.math.abs(it))}%",
                        fontSize = 12.sp,
                        color = changeColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                nft.volume24h?.let {
                    Text(
                        text = "Vol: ${formatLargeNumber(it)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatLargeNumber(value: Double): String = when {
    value >= 1_000_000_000 -> "${"%.1f".format(value / 1_000_000_000)}B"
    value >= 1_000_000     -> "${"%.1f".format(value / 1_000_000)}M"
    value >= 1_000         -> "${"%.1f".format(value / 1_000)}K"
    else                   -> "${"%.2f".format(value)}"
}
