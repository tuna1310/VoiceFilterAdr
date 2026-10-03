package com.antigravity.noichuan.data.repository

import com.antigravity.noichuan.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakePurchaseRepository : PurchaseRepository {
    private val _isPremium = MutableStateFlow(false)
    override val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    override suspend fun purchasePremium(planId: String): Boolean {
        _isPremium.value = true
        return true
    }

    override suspend fun restorePurchases(): Boolean {
        _isPremium.value = true
        return true
    }
}
