package dev.victorbreno.diaadia.api

import dev.victorbreno.diaadia.data.Quote
import retrofit2.Call
import retrofit2.http.GET

interface QuoteApi {

    @GET("api/today")
    fun getQuoteOfTheDay(): Call<List<Quote>>

    @GET("api/random")
    fun getRandomQuote(): Call<List<Quote>>
}
