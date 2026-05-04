package com.solaria.app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.*
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.solaria.app.ui.account.AccountScreen
import com.solaria.app.ui.chat.ChatScreen
import com.solaria.app.ui.dashboard.DashboardScreen
import com.solaria.app.ui.market.MarketScreen
import com.solaria.app.ui.payments.PaymentsScreen
import com.solaria.app.ui.wallet.WalletScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Filled.Home)
    object Payments  : Screen("payments", "Pay", Icons.Filled.Payments)
    object Market    : Screen("market", "Market", Icons.Filled.ShowChart)
    object Account   : Screen("account", "Account", Icons.Filled.AccountCircle)
}

private val navigationItems = listOf(
    Screen.Dashboard,
    Screen.Payments,
    Screen.Market,
    Screen.Account
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""
    val showBars = currentRoute != "ai_chat" && !currentRoute.startsWith("ai_chat?") && currentRoute != "wallet"

    Scaffold(
        topBar = {
            if (showBars) {
                TopAppBar(
                    title = { 
                        Text(
                            "Solaria", 
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        ) 
                    },
                    actions = {
                        IconButton(onClick = onToggleTheme) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "Toggle Theme"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (showBars) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val currentDestination = navBackStackEntry?.destination
                    
                    navigationItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen(navController) }
            composable(Screen.Payments.route)  { PaymentsScreen(navController) }
            composable(Screen.Market.route)    { MarketScreen(navController) }
            composable(Screen.Account.route)   { AccountScreen(navController) }

            // Wallet management screen
            composable("wallet") { WalletScreen(navController) }

            // Full screen AI chat route
            composable("ai_chat?initialMessage={initialMessage}", 
                arguments = listOf(navArgument("initialMessage") { 
                    defaultValue = ""; type = NavType.StringType 
                })
            ) { backStackEntry ->
                val initialMessage = backStackEntry.arguments?.getString("initialMessage")
                ChatScreen(navController = navController, initialMessage = initialMessage)
            }
            
            // Legacy route support
            composable("ai_chat") {
                ChatScreen(navController = navController, initialMessage = null)
            }
        }
    }
}

