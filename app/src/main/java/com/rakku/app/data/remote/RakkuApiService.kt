package com.rakku.app.data.remote

import com.rakku.app.data.model.MangaDetailApiEnvelope
import com.rakku.app.data.model.MangaDownloadResponse
import com.rakku.app.data.model.MangaHomeResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface RakkuApiService {

    // MANGA (masih lewat proxy Vercel karena butuh apikey yang disembunyikan di server)
    @GET("api/manga/home")
    suspend fun getMangaHome(): MangaHomeResponse

    @GET("api/manga/info")
    suspend fun getMangaInfo(@Query("url") url: String): MangaDetailApiEnvelope

    @GET("api/manga/download")
    suspend fun getMangaChapter(@Query("url") url: String): MangaDownloadResponse

    @GET("api/manga/search")
    suspend fun searchManga(@Query("q") query: String): MangaHomeResponse

    companion object {
        const val BASE_URL = "https://wakafive.vercel.app/"
    }
}
