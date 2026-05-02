package com.krisadan.tappick.data.model

import java.util.UUID

data class Role(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var permissions: MutableMap<String, Int> = mutableMapOf(), // productId -> maxQuantity
    val isDeletable: Boolean = true,
    var color: String = "#0055D4" // Default Primary Blue
) {
    // Ensure permissions is not null after Gson deserialization
    fun getPermissionsMap(): MutableMap<String, Int> {
        if (permissions == null) {
            permissions = mutableMapOf()
        }
        return permissions
    }
}
