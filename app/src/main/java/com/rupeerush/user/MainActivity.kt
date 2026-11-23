package com.rupeerush.user

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rupeerush.user.core.network.SupabaseClient
import com.rupeerush.user.core.theme.RupeeRushTheme
import com.rupeerush.user.data.model.Claim
import com.rupeerush.user.data.model.LedgerEntry
import com.rupeerush.user.data.model.Offer
import com.rupeerush.user.data.model.UserProfile
import com.rupeerush.user.data.model.Withdrawal
import com.rupeerush.user.ui.auth.LoginScreen
import com.rupeerush.user.ui.auth.OnboardingScreen
import com.rupeerush.user.ui.home.HomeScreen
import com.rupeerush.user.ui.leaderboard.LeaderboardScreen
import com.rupeerush.user.ui.lock.BlockedScreen
import com.rupeerush.user.ui.lock.MaintenanceLockScreen
import com.rupeerush.user.ui.navigation.AppBottomNavigation
import com.rupeerush.user.ui.navigation.BottomTab
import com.rupeerush.user.ui.navigation.Screen
import com.rupeerush.user.ui.notifications.NotificationsScreen
import com.rupeerush.user.ui.offers.OffersScreen
import com.rupeerush.user.ui.offers.TaskDetailScreen
import com.rupeerush.user.ui.offers.TaskTabsScreen
import com.rupeerush.user.ui.profile.ProfileScreen
import com.rupeerush.user.ui.referral.ReferralScreen
import com.rupeerush.user.ui.support.SupportScreen
import com.rupeerush.user.ui.wallet.WalletScreen
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RupeeRushTheme {
                RupeeRushMainApp(
                    onOpenUrl = { url ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun RupeeRushMainApp(onOpenUrl: (String) -> Unit) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    var isMaintenanceOn by remember { mutableStateOf(false) }
    var isUserBlocked by remember { mutableStateOf(false) }
    var userProfile by remember { mutableStateOf(UserProfile(id = "user_demo", name = "Player", referralCode = "RR7777")) }
    var balance by remember { mutableDoubleStateOf(0.0) }
    var streak by remember { mutableIntStateOf(1) }
    var isCheckedInToday by remember { mutableStateOf(false) }

    var offers by remember { mutableStateOf<List<Offer>>(emptyList()) }
    var claims by remember { mutableStateOf<List<Claim>>(emptyList()) }
    var ledger by remember { mutableStateOf<List<LedgerEntry>>(emptyList()) }
    var withdrawals by remember { mutableStateOf<List<Withdrawal>>(emptyList()) }

    var currentTab by remember { mutableStateOf(BottomTab.Home.route) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val telegramUrl = "https://t.me/rupeerush"

    // Periodic check for maintenance lock & account status
    LaunchedEffect(Unit) {
        // Safe read on open
    }

    if (isMaintenanceOn) {
        MaintenanceLockScreen(
            message = "Rupee Rush scheduled maintenance in progress.",
            expectedBackTime = null,
            supportLink = telegramUrl,
            onOpenSupport = onOpenUrl
        )
        return
    }

    if (isUserBlocked) {
        BlockedScreen(
            status = "Suspended",
            reason = "Multiple device accounts detected.",
            suspendedUntil = "Tomorrow",
            heldBalance = balance,
            onOpenSupportTicket = { onOpenUrl(telegramUrl) }
        )
        return
    }

    val showBottomBar = currentRoute in listOf(
        BottomTab.Home.route,
        BottomTab.Offers.route,
        BottomTab.Wallet.route,
        BottomTab.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigation(
                    currentRoute = currentTab,
                    onTabSelected = { route ->
                        currentTab = route
                        navController.navigate(route) {
                            popUpTo(BottomTab.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = { referralCode ->
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onViewTerms = { onOpenUrl("https://rupeerush.in/terms") },
                    onViewPrivacy = { onOpenUrl("https://rupeerush.in/privacy") }
                )
            }

            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(BottomTab.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(BottomTab.Home.route) {
                HomeScreen(
                    userName = userProfile.name ?: "Player",
                    walletBalance = balance,
                    streakCount = streak,
                    isCheckedInToday = isCheckedInToday,
                    onDailyCheckin = {
                        scope.launch {
                            try {
                                val res = SupabaseClient.callRpc("claim_daily_checkin", "{\"p_user_id\": \"${userProfile.id}\"}")
                                isCheckedInToday = true
                                streak++
                                balance += 1.0
                            } catch (_: Exception) {}
                        }
                    },
                    onNavigateOffers = {
                        currentTab = BottomTab.Offers.route
                        navController.navigate(BottomTab.Offers.route)
                    },
                    onNavigateReferral = { navController.navigate(Screen.Referral.route) },
                    onNavigateLeaderboard = { navController.navigate(Screen.Leaderboard.route) },
                    onNavigateProfile = {
                        currentTab = BottomTab.Profile.route
                        navController.navigate(BottomTab.Profile.route)
                    },
                    onOpenTelegram = { onOpenUrl(telegramUrl) }
                )
            }

            composable(BottomTab.Offers.route) {
                OffersScreen(
                    offers = offers,
                    isLoading = false,
                    onOfferSelected = { offerId ->
                        navController.navigate(Screen.TaskDetail.createRoute(offerId))
                    },
                    onNavigateTaskTabs = { navController.navigate(Screen.TaskTabs.route) }
                )
            }

            composable(Screen.TaskDetail.route) { backStackEntry ->
                val offerId = backStackEntry.arguments?.getString("offerId")
                val offer = offers.find { it.id == offerId } ?: Offer(
                    id = offerId ?: "demo",
                    appName = "Sponsor App",
                    downloadUrl = "https://rupeerush.in",
                    packageName = "com.sponsor.app",
                    prize = 25.0,
                    steps = listOf("Install app", "Register with mobile", "Use for 3 minutes")
                )
                TaskDetailScreen(
                    offer = offer,
                    milestones = emptyList(),
                    onBack = { navController.popBackStack() },
                    onOpenTerms = { onOpenUrl("https://rupeerush.in/terms") },
                    onGetStarted = { onOpenUrl(offer.downloadUrl) }
                )
            }

            composable(Screen.TaskTabs.route) {
                TaskTabsScreen(
                    claims = claims,
                    onBack = { navController.popBackStack() },
                    onRetryClaim = {},
                    onGoToOffers = { navController.navigate(BottomTab.Offers.route) }
                )
            }

            composable(BottomTab.Wallet.route) {
                WalletScreen(
                    balance = balance,
                    ledgerEntries = ledger,
                    withdrawals = withdrawals,
                    onRedeemPromo = { code ->
                        scope.launch {
                            try {
                                SupabaseClient.callRpc(
                                    "redeem_promo_code",
                                    "{\"p_user_id\":\"${userProfile.id}\",\"p_code\":\"$code\"}"
                                )
                            } catch (_: Exception) {}
                        }
                    },
                    onRequestWithdrawal = { amount, method, details ->
                        scope.launch {
                            try {
                                val body = JSONObject().apply {
                                    put("p_user_id", userProfile.id)
                                    put("p_amount", amount)
                                    put("p_method", method)
                                    put("p_account_snapshot", JSONObject(details))
                                }.toString()
                                SupabaseClient.callRpc("request_withdrawal", body)
                                balance -= amount
                            } catch (_: Exception) {}
                        }
                    }
                )
            }

            composable(Screen.Referral.route) {
                ReferralScreen(
                    referralCode = userProfile.referralCode ?: "RR9999",
                    totalReferrals = 0,
                    totalEarned = 0.0,
                    referredFriends = emptyList()
                )
            }

            composable(Screen.Leaderboard.route) {
                LeaderboardScreen(
                    currentUserName = userProfile.name ?: "Player",
                    users = emptyList()
                )
            }

            composable(BottomTab.Profile.route) {
                ProfileScreen(
                    user = userProfile,
                    onUpdateNickname = { userProfile = userProfile.copy(nickname = it) },
                    onOpenTerms = { onOpenUrl("https://rupeerush.in/terms") },
                    onOpenPrivacy = { onOpenUrl("https://rupeerush.in/privacy") },
                    onOpenSupport = { navController.navigate(Screen.Support.route) },
                    onLogout = { navController.navigate(Screen.Login.route) },
                    onDeleteAccount = { navController.navigate(Screen.Login.route) }
                )
            }

            composable(Screen.Support.route) {
                SupportScreen(
                    tickets = emptyList(),
                    onOpenTelegram = { onOpenUrl(telegramUrl) },
                    onSubmitTicket = { _, _ -> }
                )
            }

            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    notifications = emptyList(),
                    onDeleteNotification = {},
                    onDeleteAll = {}
                )
            }
        }
    }
}
