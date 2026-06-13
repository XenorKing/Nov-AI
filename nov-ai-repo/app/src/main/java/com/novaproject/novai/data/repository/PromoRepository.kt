package com.novaproject.novai.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromoRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    /**
     * Attempts to redeem a promo code.
     * On success returns a human-readable premium duration string.
     * Firestore structure expected:
     *   /promo_codes/{docId}
     *     - code: String (UPPERCASE)
     *     - hours: Long   (total premium hours, e.g. 720 = 30 days)
     *     - isActive: Boolean
     *     - usedBy: String? (null = not yet used)
     *     - usedAt: Timestamp?
     */
    suspend fun redeemCode(code: String): Result<String> = runCatching {
        val userId = auth.currentUser?.uid ?: error("Не авторизован")
        val codeUpper = code.trim().uppercase()

        val snap = firestore.collection("promo_codes")
            .whereEqualTo("code", codeUpper)
            .whereEqualTo("isActive", true)
            .get().await()

        if (snap.isEmpty) error("Промокод не найден или уже недействителен")

        val doc = snap.documents.first()
        val data = doc.data ?: error("Ошибка данных")

        if ((data["usedBy"] as? String) != null) error("Промокод уже использован")

        val totalHours = (data["hours"] as? Long)?.toInt() ?: 0
        val totalMinutes = (data["minutes"] as? Long)?.toInt() ?: 0
        val totalMs = (totalHours * 60L + totalMinutes) * 60_000L
        if (totalMs <= 0L) error("Промокод с нулевой длительностью")

        val currentPremiumMs = getPremiumRemainingMs(userId)
        val newExpiresAt = maxOf(System.currentTimeMillis(), currentPremiumMs) + totalMs

        doc.reference.update(mapOf(
            "usedBy" to userId,
            "usedAt" to com.google.firebase.Timestamp.now()
        )).await()

        firestore.collection("users").document(userId)
            .set(mapOf("premiumExpiresAt" to newExpiresAt, "redeemedCode" to codeUpper), SetOptions.merge())
            .await()

        formatDuration(totalHours, totalMinutes)
    }

    fun premiumFlow(): Flow<Long> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run { trySend(0L); close(); return@callbackFlow }
        val reg = firestore.collection("users").document(userId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.getLong("premiumExpiresAt") ?: 0L)
            }
        awaitClose { reg.remove() }
    }

    private suspend fun getPremiumRemainingMs(userId: String): Long {
        return try {
            val snap = firestore.collection("users").document(userId).get().await()
            snap.getLong("premiumExpiresAt") ?: 0L
        } catch (_: Exception) { 0L }
    }

    private fun formatDuration(hours: Int, minutes: Int): String = when {
        hours >= 24 * 30 -> "${hours / (24 * 30)} мес."
        hours >= 24 -> "${hours / 24} дн."
        hours > 0 -> "$hours ч."
        else -> "$minutes мин."
    }
}
