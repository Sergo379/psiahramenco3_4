package com.sergo379.gamedex.data.remote

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.sergo379.gamedex.data.remote.dto.GameDto
import com.sergo379.gamedex.data.remote.dto.GamePageDto
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface RawgApi {
    @GET("games")
    suspend fun games(
        @Query("key") key: String,
        @Query("search") search: String? = null,
        @Query("genres") genres: String? = null,
        @Query("platforms") platforms: Int? = null,
        @Query("dates") dates: String? = null,
        @Query("ordering") ordering: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): GamePageDto

    @GET("games/{id}")
    suspend fun game(@Path("id") id: Int, @Query("key") key: String): GameDto
}

object RawgServiceFactory {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun create(): RawgApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl("https://api.rawg.io/api/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RawgApi::class.java)
    }
}

