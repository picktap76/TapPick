package com.krisadan.tappick.data.model

import java.util.UUID

data class Role(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var permissions: MutableMap<String, Int> = mutableMapOf(), 
    var quotaConfigs: MutableMap<String, QuotaConfig> = mutableMapOf(),
    val isDeletable: Boolean = true,
    var color: String = "#0055D4" 
) {
    
    fun getPermissionsMap(): MutableMap<String, Int> {
        if (permissions == null) {
            permissions = mutableMapOf()
        }
        return permissions
    }

    fun getQuotaConfigsMap(): MutableMap<String, QuotaConfig> {
        if (quotaConfigs == null) {
            quotaConfigs = mutableMapOf()
        }
        return quotaConfigs
    }
}
