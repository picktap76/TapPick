package com.krisadan.tappick.data.model

import java.util.UUID

data class Member(
    val id: String = UUID.randomUUID().toString(),
    val nfcId: String? = null,
    var name: String,
    var roleId: String, 
    var pin: String = ""
)
