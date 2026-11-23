package com.rupeerush.user.ui.wallet

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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.rupeerush.user.data.model.LedgerEntry
import com.rupeerush.user.data.model.Withdrawal

@Composable
fun WalletScreen(
    balance: Double,
    ledgerEntries: List<LedgerEntry>,
    withdrawals: List<Withdrawal>,
    onRedeemPromo: (code: String) -> Unit,
    onRequestWithdrawal: (amount: Double, method: String, details: Map<String, String>) -> Unit
) {
    var promoCode by remember { mutableStateOf("") }
    var showWithdrawDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Wallet",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Glass Balance Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Text(
                        text = "Available Balance",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${String.format("%.2f", balance)}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldPrimary
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = { showWithdrawDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text(
                            text = "Withdraw Funds",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Promo Code Section
            Text(
                text = "Telegram Promo Code",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = promoCode,
                        onValueChange = { promoCode = it.uppercase() },
                        placeholder = { Text("Enter Promo Code") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (promoCode.isNotBlank()) {
                                onRedeemPromo(promoCode.trim())
                                promoCode = ""
                            }
                        },
                        enabled = promoCode.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Redeem", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Withdrawals Section
            if (withdrawals.isNotEmpty()) {
                Text(
                    text = "Withdrawal Requests",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        items(withdrawals) { withdrawal ->
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "₹${withdrawal.amount.toInt()} via ${withdrawal.method.uppercase()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = withdrawal.createdAt.take(10),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = withdrawal.status.uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = when (withdrawal.status) {
                            "paid" -> EmeraldPrimary
                            "rejected" -> ErrorRed
                            else -> Color(0xFFF59E0B)
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Transaction History",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(ledgerEntries) { entry ->
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (entry.type == "credit") EmeraldPrimary.copy(alpha = 0.2f) else ErrorRed.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (entry.type == "credit") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (entry.type == "credit") EmeraldPrimary else ErrorRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.reason.replace("_", " ").replaceFirstChar { it.uppercase() },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = entry.createdAt.take(16).replace("T", " "),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "${if (entry.type == "credit") "+" else "-"} ₹${String.format("%.2f", entry.amount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (entry.type == "credit") EmeraldPrimary else ErrorRed
                    )
                }
            }
        }
    }

    if (showWithdrawDialog) {
        WithdrawDialog(
            balance = balance,
            onDismiss = { showWithdrawDialog = false },
            onConfirm = { amount, method, details ->
                showWithdrawDialog = false
                onRequestWithdrawal(amount, method, details)
            }
        )
    }
}

@Composable
fun WithdrawDialog(
    balance: Double,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, method: String, details: Map<String, String>) -> Unit
) {
    var method by remember { mutableStateOf("upi") }
    var amountText by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var accountHolder by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request Withdrawal", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = method == "upi",
                        onClick = { method = "upi" },
                        colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                    )
                    Text("UPI")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = method == "bank",
                        onClick = { method = "bank" },
                        colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                    )
                    Text("Bank Transfer")
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (Min ₹50 - Max ₹1000)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (method == "upi") {
                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it.trim() },
                        label = { Text("UPI ID (e.g. mobile@upi)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = accountHolder,
                        onValueChange = { accountHolder = it },
                        label = { Text("Account Holder Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it.trim() },
                        label = { Text("Account Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = ifscCode,
                        onValueChange = { ifscCode = it.trim().uppercase() },
                        label = { Text("IFSC Code") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                errorText?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it, color = ErrorRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount < 50.0 || amount > 1000.0) {
                        errorText = "Enter valid amount between ₹50 and ₹1,000"
                        return@Button
                    }
                    if (amount > balance) {
                        errorText = "Insufficient wallet balance"
                        return@Button
                    }
                    if (method == "upi" && !upiId.contains("@")) {
                        errorText = "Enter valid UPI ID with @"
                        return@Button
                    }
                    if (method == "bank" && (accountNumber.length < 8 || ifscCode.length != 11)) {
                        errorText = "Enter valid Account Number & 11-digit IFSC"
                        return@Button
                    }

                    errorText = null
                    showConfirmDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Proceed", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showConfirmDialog) {
        val amount = amountText.toDouble()
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Deduction", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "₹$amount will be deducted from your wallet immediately. Your withdrawal request will be processed within 24 hours.\n\nMethod: ${method.uppercase()}"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        val details = if (method == "upi") {
                            mapOf("upi_id" to upiId)
                        } else {
                            mapOf(
                                "account_holder" to accountHolder,
                                "account_number" to accountNumber,
                                "ifsc" to ifscCode
                            )
                        }
                        onConfirm(amount, method, details)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Confirm & Deduct", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Back") }
            }
        )
    }
}
