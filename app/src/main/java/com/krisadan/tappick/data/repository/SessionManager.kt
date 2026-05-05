package com.krisadan.tappick.data.repository

import android.content.Context
import android.content.SharedPreferences

class SessionManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tappick_session_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MEMBER_ID = "member_id"

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context).also { INSTANCE = it }
            }
        }
    }

    fun login(memberId: String) {
        prefs.edit().putString(KEY_MEMBER_ID, memberId).apply()
    }

    fun logout() {
        prefs.edit().remove(KEY_MEMBER_ID).apply()
    }

    fun getMemberId(): String? {
        return prefs.getString(KEY_MEMBER_ID, null)
    }

    fun isLoggedIn(): Boolean {
        return getMemberId() != null
    }

    // Compatibility for old code if needed
    fun getCurrentMemberNfcId(): String? {
        // This is now slightly misleading but I'll update usages
        return getMemberId()
    }
}
