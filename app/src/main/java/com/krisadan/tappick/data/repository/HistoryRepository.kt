package com.krisadan.tappick.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.krisadan.tappick.data.model.HistoryEntry

class HistoryRepository private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tappick_history_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private var cachedHistory: MutableList<HistoryEntry>? = null

    companion object {
        @Volatile
        private var INSTANCE: HistoryRepository? = null

        fun getInstance(context: Context): HistoryRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HistoryRepository(context).also { INSTANCE = it }
            }
        }
    }

    fun getHistory(): List<HistoryEntry> {
        if (cachedHistory == null) {
            val json = prefs.getString("history", null)
            cachedHistory = if (json != null) {
                val type = object : TypeToken<MutableList<HistoryEntry>>() {}.type
                gson.fromJson(json, type)
            } else {
                mutableListOf()
            }
        }
        return cachedHistory ?: emptyList()
    }

    fun getUsageCountSince(memberId: String, productId: String, sinceTimestamp: Long): Int {
        return getHistory()
            .filter { it.memberId == memberId && it.timestamp >= sinceTimestamp }
            .flatMap { it.items }
            .filter { it.productId == productId }
            .sumOf { it.quantity }
    }

    fun getStartTimeForConfig(config: com.krisadan.tappick.data.model.QuotaConfig): Long {
        val intervalMillis = when (config.intervalUnit) {
            com.krisadan.tappick.data.model.QuotaUnit.MINUTE -> config.intervalValue * 60 * 1000L
            com.krisadan.tappick.data.model.QuotaUnit.HOUR -> config.intervalValue * 60 * 60 * 1000L
            com.krisadan.tappick.data.model.QuotaUnit.DAY -> config.intervalValue * 24 * 60 * 60 * 1000L
            com.krisadan.tappick.data.model.QuotaUnit.WEEK -> config.intervalValue * 7 * 24 * 60 * 60 * 1000L
            com.krisadan.tappick.data.model.QuotaUnit.MONTH -> config.intervalValue * 30 * 24 * 60 * 60 * 1000L
            com.krisadan.tappick.data.model.QuotaUnit.YEAR -> config.intervalValue * 365 * 24 * 60 * 60 * 1000L
        }
        return System.currentTimeMillis() - intervalMillis
    }

    fun addEntry(entry: HistoryEntry) {
        val history = getHistory().toMutableList()
        history.add(0, entry)
        saveHistory(history)
    }

    private fun saveHistory(history: List<HistoryEntry>) {
        cachedHistory = history.toMutableList()
        val json = gson.toJson(history)
        prefs.edit().putString("history", json).apply()
    }

    fun updateMemberName(memberId: String, newName: String) {
        val history = getHistory().toMutableList()
        var changed = false
        history.forEachIndexed { index, entry ->
            if (entry.memberId == memberId && entry.memberName != newName) {
                history[index] = entry.copy(memberName = newName)
                changed = true
            }
        }
        if (changed) saveHistory(history)
    }

    fun updateProductName(productId: String, newName: String) {
        val history = getHistory().toMutableList()
        var changed = false
        history.forEachIndexed { index, entry ->
            var rowChanged = false
            val updatedItems = entry.items.map { item ->
                if (item.productId == productId && item.productName != newName) {
                    rowChanged = true
                    changed = true
                    item.copy(productName = newName)
                } else {
                    item
                }
            }
            if (rowChanged) {
                history[index] = entry.copy(items = updatedItems)
            }
        }
        if (changed) saveHistory(history)
    }
}
