package dev.victorbreno.diaadia.data

data class ReflectionEntry(
    val id: String = "",
    val text: String = "",
    val photoBase64: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationName: String = "",
    val createdAt: Long = 0L,
    val formattedDate: String = ""
)