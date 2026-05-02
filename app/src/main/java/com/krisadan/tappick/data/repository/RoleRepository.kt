package com.krisadan.tappick.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.krisadan.tappick.data.model.Role

class RoleRepository private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tappick_roles_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private var cachedRoles: MutableList<Role>? = null

    companion object {
        @Volatile
        private var INSTANCE: RoleRepository? = null

        fun getInstance(context: Context): RoleRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RoleRepository(context).also { INSTANCE = it }
            }
        }
    }

    fun getRoles(): List<Role> {
        if (cachedRoles == null) {
            val json = prefs.getString("roles", null)
            if (json == null) {
                val defaultRoles = listOf(
                    Role(id = "admin_role", name = "ผู้ดูแล", isDeletable = false, color = "#0055D4")
                )
                saveRoles(defaultRoles)
                return defaultRoles
            }
            val type = object : TypeToken<MutableList<Role>>() {}.type
            cachedRoles = gson.fromJson(json, type)
        }
        return cachedRoles ?: emptyList()
    }

    fun saveRoles(roles: List<Role>) {
        cachedRoles = roles.toMutableList()
        val json = gson.toJson(roles)
        prefs.edit().putString("roles", json).apply()
    }

    fun addRole(role: Role) {
        val roles = getRoles().toMutableList()
        roles.add(role)
        saveRoles(roles)
    }

    fun updateRole(updatedRole: Role) {
        val roles = getRoles().toMutableList()
        val index = roles.indexOfFirst { it.id == updatedRole.id }
        if (index != -1) {
            roles[index] = updatedRole
            saveRoles(roles)
        }
    }

    fun deleteRole(roleId: String) {
        val roles = getRoles().toMutableList()
        val roleToDelete = roles.find { it.id == roleId }
        if (roleToDelete != null && roleToDelete.isDeletable) {
            roles.remove(roleToDelete)
            saveRoles(roles)
        }
    }
}
