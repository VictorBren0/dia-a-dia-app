package dev.victorbreno.diaadia.data

data class DiaryProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val coverPhotoBase64: String = "",
    val todayFocus: String = "",
    val dailyReflection: String = "",
    val reflectionDate: String = ""
)