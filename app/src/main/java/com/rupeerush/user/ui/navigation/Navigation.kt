package com.rupeerush.user.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.rupeerush.user.core.theme.EmeraldPrimary

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Onboarding : Screen("onboarding")
    object Permissions : Screen("permissions")
    object Main : Screen("main")
    object TaskDetail : Screen("task_detail/{offerId}") {
        fun createRoute(offerId: String) = "task_detail/$offerId"
    }
    object TaskTerms : Screen("task_terms/{offerId}") {
        fun createRoute(offerId: String) = "task_terms/$offerId"
    }
    object TaskTabs : Screen("task_tabs")
    object Referral : Screen("referral")
    object Leaderboard : Screen("leaderboard")
    object Notifications : Screen("notifications")
    object Support : Screen("support")
    object RaiseTicket : Screen("raise_ticket")
    object Maintenance : Screen("maintenance")
    object Blocked : Screen("blocked")
    object Offline : Screen("offline")
}

sealed class BottomTab(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomTab("tab_home", "Home", Icons.Default.Home)
    object Offers : BottomTab("tab_offers", "Offers", Icons.Default.LocalOffer)
    object Wallet : BottomTab("tab_wallet", "Wallet", Icons.Default.AccountBalanceWallet)
    object Profile : BottomTab("tab_profile", "Profile", Icons.Default.Person)
}

@Composable
fun AppBottomNavigation(
    currentRoute: String,
    onTabSelected: (route: String) -> Unit
) {
    val tabs = listOf(
        BottomTab.Home,
        BottomTab.Offers,
        BottomTab.Wallet,
        BottomTab.Profile
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        tabs.forEach { tab ->
            val isSelected = currentRoute == tab.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab.route) },
                icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                label = { Text(text = tab.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = EmeraldPrimary,
                    selectedTextColor = EmeraldPrimary,
                    indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                )
            )
        }
    }
}
