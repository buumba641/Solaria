package com.solaria.app.ui.bitrefill

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.solaria.app.ui.theme.SolariaGreen

data class GiftCardOption(val name: String, val slug: String, val color: Color)

val giftCards = listOf(
    GiftCardOption("Amazon", "amazon-usa", Color(0xFFFF9900)),
    GiftCardOption("Netflix", "netflix-usa", Color(0xFFE50914)),
    GiftCardOption("Spotify", "spotify-usa", Color(0xFF1DB954)),
    GiftCardOption("Apple", "apple-usa", Color(0xFF000000)),
    GiftCardOption("Google Play", "google-play-usa", Color(0xFF34A853)),
    GiftCardOption("Uber", "uber-usa", Color(0xFF000000))
)

@Composable
fun BitrefillScreen(
    viewModel: BitrefillViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Gift Cards",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        if (uiState is BitrefillUiState.Success) {
            SuccessDialog(code = (uiState as BitrefillUiState.Success).code)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(giftCards) { card ->
                GiftCardItem(card) {
                    viewModel.purchaseGiftCard(card.slug, 25.0)
                }
            }
        }
        
        if (uiState is BitrefillUiState.Processing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun GiftCardItem(card: GiftCardOption, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = card.color.copy(alpha = 0.1f))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(card.name, fontWeight = FontWeight.Bold, color = card.color)
        }
    }
}

@Composable
fun SuccessDialog(code: String) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Purchase Successful!") },
        text = { 
            Column {
                Text("Your gift card code is:")
                Text(code, fontWeight = FontWeight.Black, fontSize = 20.sp, color = SolariaGreen)
            }
        },
        confirmButton = {
            Button(onClick = { /* Refresh state */ }) { Text("Done") }
        }
    )
}
