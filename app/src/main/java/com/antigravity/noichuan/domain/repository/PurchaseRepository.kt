package com.antigravity.noichuan.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface PurchaseRepository {
    val isPremium: StateFlow<Boolean>
    suspend fun purchasePremium(planId: String): Boolean
    suspend fun restorePurchases(): Boolean
}
