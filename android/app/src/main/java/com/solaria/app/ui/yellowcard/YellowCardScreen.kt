package com.solaria.app.ui.yellowcard

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun YellowCardScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Buy & Sell Crypto",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(24.dp)
        )
        
        // Yellow Card Widget WebView
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    // Loading the Yellow Card widget URL (demo URL used here)
                    loadUrl("https://widget.yellowcard.io/?address=YOUR_WALLET_ADDRESS")
                }
            },
            modifier = Modifier.fillMaxSize().weight(1f)
        )
    }
}
