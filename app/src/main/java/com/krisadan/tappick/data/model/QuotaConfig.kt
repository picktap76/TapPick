package com.krisadan.tappick.data.model

data class QuotaConfig(
    var intervalValue: Int = 1,
    var intervalUnit: QuotaUnit = QuotaUnit.WEEK
)

enum class QuotaUnit {
    MINUTE, HOUR, DAY, WEEK, MONTH, YEAR;
    
    fun toThai(): String = when(this) {
        MINUTE -> "นาที"
        HOUR -> "ชั่วโมง"
        DAY -> "วัน"
        WEEK -> "สัปดาห์"
        MONTH -> "เดือน"
        YEAR -> "ปี"
    }
}
