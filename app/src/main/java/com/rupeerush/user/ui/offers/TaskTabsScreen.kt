package com.rupeerush.user.ui.offers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rupeerush.user.core.components.GlassCard
import com.rupeerush.user.core.theme.EmeraldPrimary
import com.rupeerush.user.core.theme.ErrorRed
import com.rupeerush.user.data.model.Claim

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTabsScreen(
    claims: List<Claim>,
    onBack: () -> Unit,
    onRetryClaim: (claimId: String) -> Unit,
    onGoToOffers: () -> Unit
) {
    val tabs = listOf("Ongoing", "Pending", "Completed", "Rejected")
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val filteredClaims = when (selectedTabIndex) {
        0 -> claims.filter { it.status == "ongoing" }
        1 -> claims.filter { it.status == "pending" }
        2 -> claims.filter { it.status == "completed" }
        3 -> claims.filter { it.status == "rejected" }
        else -> emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("My Tasks", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        if (filteredClaims.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No ${tabs[selectedTabIndex].lowercase()} tasks found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onGoToOffers,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Browse Offers", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredClaims) { claim ->
                    TaskClaimCard(
                        claim = claim,
                        onRetry = { onRetryClaim(claim.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskClaimCard(
    claim: Claim,
    onRetry: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Task ID: ${claim.id.take(8)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = claim.status.uppercase(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = when (claim.status) {
                        "completed" -> EmeraldPrimary
                        "rejected" -> ErrorRed
                        else -> Color(0xFF3B82F6)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (claim.status == "pending") {
                Text(
                    text = "Installation is not payment. Waiting for admin approval.",
                    fontSize = 12.sp,
                    color = Color(0xFFF59E0B),
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (claim.status == "rejected") {
                Text(
                    text = "Reason: ${claim.rejectionReason ?: "Requirements not fulfilled"}",
                    fontSize = 12.sp,
                    color = ErrorRed
                )
                if (claim.retryCount < 3) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Retry (${3 - claim.retryCount} left)", color = Color.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
