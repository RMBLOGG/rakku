package com.rakku.app.data.remote

import com.rakku.app.data.model.SankaComicChapterResponse
import com.rakku.app.data.model.SankaComicDetailResponse
import com.rakku.app.data.model.SankaComicGenreListResponse
import com.rakku.app.data.model.SankaComicGenreListWrapper
import com.rakku.app.data.model.SankaComicListResponse
import com.rakku.app.data.model.SankaComicSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Manggil LANGSUNG ke API komik Sanka (sumber data: Komiku.org), gantiin
 * proxy Vercel lama ("api/manga/..."). Endpoint publik, tanpa apikey, jadi
 * aman dipanggil langsung dari app - sama kayak pola SankaAnimeApiService.
 *
 * Endpoint kayak /comic/unlimited, /comic/scroll, /comic/realtime,
 * /comic/comparison, /comic/docs, /comic/fullstats, /comic/trending,
 * /comic/browse, /comic/type/{type} SENGAJA gak dipakai - itu cuma fitur
 * showcase/benchmark performa API, bukan endpoint buat kebutuhan baca komik.
 */
interface SankaComicApiService {

    @GET("terbaru")
    suspend fun getLatest(@Query("page") page: Int? = null): SankaComicListResponse

    @GET("populer")
    suspend fun getPopular(@Query("page") page: Int? = null): SankaComicListResponse

    @GET("search")
    suspend fun searchComic(@Query("q") query: String): SankaComicSearchResponse

    // path "slug" di sini bisa berupa slug polos ("naruto") ATAU sudah
    // sekalian slug hasil ekstraksi dari link /manga/{slug}/
    @GET("comic/{slug}")
    suspend fun getComicDetail(@Path("slug") slug: String): SankaComicDetailResponse

    // "chapterSlug" WAJIB slug lengkap dengan nomor chapter-nya, mis.
    // "naruto-chapter-700" (didapat dari field "slug" di dalam
    // SankaComicDetailResponse.chapters, bukan slug manga polos)
    @GET("chapter/{chapterSlug}")
    suspend fun getComicChapter(@Path("chapterSlug") chapterSlug: String): SankaComicChapterResponse

    @GET("genres")
    suspend fun getGenres(): SankaComicGenreListWrapper

    @GET("genre/{slug}")
    suspend fun getComicByGenre(
        @Path("slug") slug: String,
        @Query("page") page: Int? = null
    ): SankaComicGenreListResponse

    companion object {
        const val BASE_URL = "https://www.sankavollerei.web.id/comic/"
    }
}
