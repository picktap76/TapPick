package com.krisadan.tappick.data.model

import java.util.UUID

data class Member(
    val id: String = UUID.randomUUID().toString(),
    val nfcId: String,
    var name: String,
    var roleId: String // ID from Role
)
