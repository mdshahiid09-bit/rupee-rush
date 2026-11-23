package com.rupeerush.user.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val email: String? = null,
    val name: String? = null,
    val nickname: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("referral_code") val referralCode: String? = null,
    val status: String = "active", // active, suspended, banned
    @SerialName("suspended_until") val suspendedUntil: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class WalletData(
    val id: String,
    @SerialName("user_id") val userId: String,
    val balance: Double = 0.0,
    @SerialName("total_earned") val totalEarned: Double = 0.0
)

@Serializable
data class LedgerEntry(
    val id: String,
    val type: String, // credit, debit
    val amount: Double,
    val reason: String,
    @SerialName("reference_id") val referenceId: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class Offer(
    val id: String,
    @SerialName("app_name") val appName: String,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("play_or_download_url") val downloadUrl: String,
    @SerialName("package_name") val packageName: String,
    val prize: Double,
    val steps: List<String> = emptyList(),
    @SerialName("terms_text") val termsText: String? = null,
    @SerialName("is_published") val isPublished: Boolean = true,
    @SerialName("daily_cap") val dailyCap: Int = 0,
    @SerialName("sort_order") val sortOrder: Int = 0
)

@Serializable
data class OfferMilestone(
    val id: String,
    @SerialName("offer_id") val offerId: String,
    val type: String, // step, daily
    val title: String,
    @SerialName("bonus_amount") val bonusAmount: Double,
    @SerialName("days_required") val daysRequired: Int = 0,
    @SerialName("is_enabled") val isEnabled: Boolean = true
)

@Serializable
data class Claim(
    val id: String,
    @SerialName("offer_id") val offerId: String,
    val status: String, // ongoing, pending, completed, rejected, flagged
    @SerialName("started_at") val startedAt: String,
    @SerialName("installed_at") val installedAt: String? = null,
    @SerialName("usage_seconds") val usageSeconds: Int = 0,
    @SerialName("retry_count") val retryCount: Int = 0,
    @SerialName("rejection_reason") val rejectionReason: String? = null
)

@Serializable
data class Withdrawal(
    val id: String,
    val amount: Double,
    val method: String, // upi, bank
    val status: String, // requested, processing, paid, rejected
    @SerialName("hold_until") val holdUntil: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class CheckinResult(
    val success: Boolean,
    val message: String? = null,
    @SerialName("streak_count") val streakCount: Int = 1,
    @SerialName("bonus_amount") val bonusAmount: Double = 0.0,
    @SerialName("new_balance") val newBalance: Double = 0.0
)

@Serializable
data class PromoRedeemResult(
    val success: Boolean,
    val message: String? = null,
    val amount: Double = 0.0,
    @SerialName("new_balance") val newBalance: Double = 0.0
)

@Serializable
data class WithdrawalResult(
    val success: Boolean,
    val message: String? = null,
    @SerialName("withdrawal_id") val withdrawalId: String? = null,
    val amount: Double = 0.0,
    @SerialName("hold_until") val holdUntil: String? = null,
    @SerialName("remaining_balance") val remainingBalance: Double = 0.0
)

@Serializable
data class LeaderboardUser(
    val rank: Int,
    val name: String,
    @SerialName("total_earned") val totalEarned: Double
)

@Serializable
data class NotificationItem(
    val id: String,
    val type: String,
    @SerialName("title_en") val titleEn: String,
    @SerialName("title_hi") val titleHi: String? = null,
    @SerialName("message_en") val messageEn: String,
    @SerialName("message_hi") val messageHi: String? = null,
    @SerialName("created_at") val createdAt: String
)
