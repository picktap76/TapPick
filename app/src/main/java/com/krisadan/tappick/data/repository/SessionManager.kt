package com.krisadan.tappick.data.repository

import android.content.Context
import android.content.SharedPreferences

class SessionManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tappick_session_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CURRENT_MEMBER_NFC_ID = "current_member_nfc_id"

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context).also { INSTANCE = it }
            }
        }
    }

    fun login(nfcId: String) {
        prefs.edit().putString(KEY_CURRENT_MEMBER_NFC_ID, nfcId).commit()
    }

    fun logout() {
        prefs.edit().remove(KEY_CURRENT_MEMBER_NFC_ID).commit()
    }

    fun getCurrentMemberNfcId(): String? {
        return prefs.getString(KEY_CURRENT_MEMBER_NFC_ID, null)
    }

    fun isLoggedIn(): Boolean {
        return getCurrentMemberNfcId() != null
    }
}
