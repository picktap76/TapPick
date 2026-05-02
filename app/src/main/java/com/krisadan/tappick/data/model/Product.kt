package com.krisadan.tappick.data.model

import java.util.UUID

data class Product(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var imageUri: String? = null,
    var status: String = "ไม่จำกัด"
)
