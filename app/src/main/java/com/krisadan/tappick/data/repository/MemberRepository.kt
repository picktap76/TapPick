package com.krisadan.tappick.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.krisadan.tappick.data.model.Member

class MemberRepository private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tappick_members_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    @Volatile
    private var cachedMembers: MutableList<Member>? = null

    companion object {
        @Volatile
        private var INSTANCE: MemberRepository? = null

        fun getInstance(context: Context): MemberRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MemberRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    @Synchronized
    fun getMembers(): List<Member> {
        return cachedMembers ?: run {
            val json = prefs.getString("members", null)
            val members: MutableList<Member> = if (json != null) {
                try {
                    val type = object : TypeToken<MutableList<Member>>() {}.type
                    gson.fromJson(json, type) ?: mutableListOf()
                } catch (e: Exception) {
                    mutableListOf()
                }
            } else {
                mutableListOf()
            }
            cachedMembers = members
            members
        }
    }

    @Synchronized
    fun saveMembers(members: List<Member>) {
        cachedMembers = members.toMutableList()
        try {
            val json = gson.toJson(members)
            prefs.edit().putString("members", json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addMember(member: Member) {
        val members = getMembers().toMutableList()
        members.add(member)
        saveMembers(members)
    }

    fun updateMember(updatedMember: Member) {
        val members = getMembers().toMutableList()
        val index = members.indexOfFirst { it.id == updatedMember.id }
        if (index != -1) {
            members[index] = updatedMember
            saveMembers(members)
        }
    }

    fun deleteMember(memberId: String) {
        val members = getMembers().toMutableList()
        members.removeAll { it.id == memberId }
        saveMembers(members)
    }
}
