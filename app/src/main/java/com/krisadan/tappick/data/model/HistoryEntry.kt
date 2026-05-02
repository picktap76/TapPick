package com.krisadan.tappick.data.model

import java.util.UUID

data class HistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val memberId: String,
    val memberName: String,
    val items: List<TransactionItem>
)

data class TransactionItem(
    val productId: String,
    val productName: String,
    val quantity: Int
)
