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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rupeerush.user.core.components.GlassCard
import com.rupeerush.user.core.components.ShimmerBox
import com.rupeerush.user.core.theme.EmeraldPrimary
import com.rupeerush.user.data.model.Offer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersScreen(
    offers: List<Offer>,
    isLoading: Boolean,
    onOfferSelected: (offerId: String) -> Unit,
    onNavigateTaskTabs: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Available Offers",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            actions = {
                IconButton(onClick = { showMenu = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("My Tasks (Ongoing, Pending, etc.)") },
                        onClick = {
                            showMenu = false
                            onNavigateTaskTabs()
                        }
                    )
                }
            }
        )

        if (isLoading) {
            Column(modifier = Modifier.padding(16.dp)) {
                repeat(4) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .padding(bottom = 12.dp)
                    )
                }
            }
        } else if (offers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No offers available right now. Check back tomorrow!",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(offers) { offer ->
                    OfferCard(
                        offer = offer,
                        onClick = { onOfferSelected(offer.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun OfferCard(
    offer: Offer,
    onClick: () -> Unit
) {
    val isCapReached = offer.dailyCap > 0 && false // Connected to live cap count

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (isCapReached) null else onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Logo
            AsyncImage(
                model = offer.logoUrl ?: "https://placehold.co/100x100/png",
                contentDescription = offer.appName,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(14.dp))

            // App Name & Status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = offer.appName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCapReached) Color.Gray else MaterialTheme.colorScheme.onBackground
                )

                if (isCapReached) {
                    Text(
                        text = "Kal aayega",
                        fontSize = 12.sp,
                        color = Color(0xFFF59E0B),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Bold Highlighted Prize
            Box(
                modifier = Modifier
                    .background(
                        if (isCapReached) Color.Gray.copy(alpha = 0.2f) else EmeraldPrimary.copy(alpha = 0.15f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "₹${offer.prize.toInt()}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isCapReached) Color.Gray else EmeraldPrimary
                )
            }
        }
    }
}
