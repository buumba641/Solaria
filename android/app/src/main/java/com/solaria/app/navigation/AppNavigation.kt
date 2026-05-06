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
    object BuySell   : Screen("buy_sell", "Buy/Sell", Icons.Filled.CurrencyExchange)
    object Payments  : Screen("payments", "Pay", Icons.Filled.Payments)
    object Market    : Screen("market", "Market", Icons.Filled.ShowChart)
    object Card      : Screen("solcard", "Card", Icons.Filled.CreditCard)
    object Account   : Screen("account", "Account", Icons.Filled.AccountCircle)
}

private val navigationItems = listOf(
    Screen.Dashboard,
    Screen.BuySell,
    Screen.Payments,
    Screen.Market,
    Screen.Card
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    // Use a dedicated ViewModel to access Hilt-managed auth state
    val authViewModel: com.solaria.app.ui.auth.AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val isLoggedIn = authViewModel.isLoggedIn()
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""
    val isAuthScreen = currentRoute == "auth"
    val showBars = !isAuthScreen && currentRoute != "ai_chat" && !currentRoute.startsWith("ai_chat?") && currentRoute != "wallet"

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
                        IconButton(onClick = { navController.navigate(Screen.Account.route) }) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = "Account")
                        }
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
            startDestination = if (isLoggedIn) Screen.Dashboard.route else "auth",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("auth") {
                com.solaria.app.ui.auth.AuthScreen(
                    onAuthSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) { DashboardScreen(navController) }
            composable(Screen.BuySell.route)   { com.solaria.app.ui.yellowcard.YellowCardScreen() }
            composable(Screen.Payments.route)  { PaymentsScreen(navController) }
            composable(Screen.Market.route)    { MarketScreen(navController) }
            composable(Screen.Card.route)      { com.solaria.app.ui.solcard.SolCardScreen() }
            composable(Screen.Account.route)   { AccountScreen(navController) }

            // Other routes...
            composable("bitrefill") { com.solaria.app.ui.bitrefill.BitrefillScreen() }
            composable("wallet") { WalletScreen(navController) }
            composable("ai_chat?initialMessage={initialMessage}", 
                arguments = listOf(navArgument("initialMessage") { defaultValue = ""; type = NavType.StringType })
            ) { backStackEntry ->
                val initialMessage = backStackEntry.arguments?.getString("initialMessage")
                ChatScreen(navController = navController, initialMessage = initialMessage)
            }
            composable("ai_chat") { ChatScreen(navController = navController, initialMessage = null) }
        }
    }
}

