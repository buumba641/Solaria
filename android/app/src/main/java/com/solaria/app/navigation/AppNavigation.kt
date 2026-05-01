package com.solaria.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.solaria.app.ui.chat.ChatScreen
import com.solaria.app.ui.market.MarketScreen
import com.solaria.app.ui.wallet.WalletScreen

sealed class Screen(val route: String, val label: String) {
    object Chat   : Screen("chat",   "Chat")
    object Market : Screen("market", "Market")
    object Wallet : Screen("wallet", "Wallet")
}

private val bottomTabs = listOf(Screen.Chat, Screen.Market, Screen.Wallet)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDest = navBackStackEntry?.destination

                bottomTabs.forEach { screen ->
                    val icon = when (screen) {
                        Screen.Chat   -> Icons.Filled.Chat
                        Screen.Market -> Icons.Filled.BarChart
                        Screen.Wallet -> Icons.Filled.AccountBalanceWallet
                    }
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDest?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chat.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Chat.route)   { ChatScreen(navController) }
            composable(Screen.Market.route) { MarketScreen() }
            composable(Screen.Wallet.route) { WalletScreen() }
        }
    }
}
