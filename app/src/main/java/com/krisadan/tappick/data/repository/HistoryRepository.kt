package com.krisadan.tappick.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.krisadan.tappick.data.model.HistoryEntry
import java.util.Calendar

class HistoryRepository private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tappick_history_prefs", Context.MODE_PRIVATE)
    private val settingsRepository = SettingsRepository.getInstance(context)
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

    fun getWeeklyUsageCount(memberId: String, productId: String): Int {
        val startOfWeek = getResetTimestamp()
        return getHistory()
            .filter { it.memberId == memberId && it.timestamp >= startOfWeek }
            .flatMap { it.items }
            .filter { it.productId == productId }
            .sumOf { it.quantity }
    }

    private fun getResetTimestamp(): Long {
        val resetDay = settingsRepository.getResetDay()
        val resetHour = settingsRepository.getResetHour()
        val resetMinute = settingsRepository.getResetMinute()

        val now = Calendar.getInstance()
        val resetTime = Calendar.getInstance()
        
        resetTime.set(Calendar.HOUR_OF_DAY, resetHour)
        resetTime.set(Calendar.MINUTE, resetMinute)
        resetTime.set(Calendar.SECOND, 0)
        resetTime.set(Calendar.MILLISECOND, 0)
        
        val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)

        var daysToSubtract = (currentDayOfWeek - resetDay + 7) % 7

        if (daysToSubtract == 0) {
            if (now.before(resetTime)) {
                daysToSubtract = 7
            }
        }
        
        resetTime.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        
        return resetTime.timeInMillis
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
