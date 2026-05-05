package com.krisadan.tappick.data.repository

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

class SettingsRepository private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tappick_settings_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_RESET_DAY = "reset_day"
        private const val KEY_RESET_HOUR = "reset_hour"
        private const val KEY_RESET_MINUTE = "reset_minute"

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context).also { INSTANCE = it }
            }
        }
    }

    fun getResetDay(): Int {
        return prefs.getInt(KEY_RESET_DAY, Calendar.MONDAY)
    }

    fun setResetDay(day: Int) {
        prefs.edit().putInt(KEY_RESET_DAY, day).apply()
    }

    fun getResetHour(): Int {
        return prefs.getInt(KEY_RESET_HOUR, 0)
    }

    fun setResetHour(hour: Int) {
        prefs.edit().putInt(KEY_RESET_HOUR, hour).apply()
    }

    fun getResetMinute(): Int {
        return prefs.getInt(KEY_RESET_MINUTE, 0)
    }

    fun setResetMinute(minute: Int) {
        prefs.edit().putInt(KEY_RESET_MINUTE, minute).apply()
    }

    fun getResetDayName(): String {
        return when (getResetDay()) {
            Calendar.SUNDAY -> "วันอาทิตย์"
            Calendar.MONDAY -> "วันจันทร์"
            Calendar.TUESDAY -> "วันอังคาร"
            Calendar.WEDNESDAY -> "วันพุธ"
            Calendar.THURSDAY -> "วันพฤหัสบดี"
            Calendar.FRIDAY -> "วันศุกร์"
            Calendar.SATURDAY -> "วันเสาร์"
            else -> "วันจันทร์"
        }
    }
}
