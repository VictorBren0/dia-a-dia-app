package dev.victorbreno.diaadia.data

import com.google.gson.annotations.SerializedName

data class Quote(
    @SerializedName("q") val text: String = "",
    @SerializedName("a") val author: String = ""
)
