package com.rakku.app.data.remote

import android.content.Context
import android.net.ConnectivityManager
import com.rakku.app.data.model.AnimeDetailResponse
import com.rakku.app.data.model.AnimeEpisodeDetailResponse
import com.rakku.app.data.model.AnimeHomeResponse
import com.rakku.app.data.model.AnimePagination
import com.rakku.app.data.model.AnimeinwebEpisodeItem
import com.rakku.app.data.model.AnimeinwebItem
import com.rakku.app.data.model.GenreItem
import com.rakku.app.data.model.MangaDetailResponse
import com.rakku.app.data.model.MangaDownloadResponse
import com.rakku.app.data.model.MangaHomeResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.Calendar
import java.util.concurrent.TimeUnit

class RakkuApiRepository(private val context: Context? = null) {

    private val moshi = Moshi.Builder()
        .add(LenientStatusAdapter())
        .add(LenientNameListAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val client = OkHttpClient.Builder().build()

    private val api: RakkuApiService = Retrofit.Builder()
        .baseUrl(RakkuApiService.BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(RakkuApiService::class.java)

    // ANIME: ganti total dari Sanka ke animeinweb-api - sumber yang sama persis
    // kayak "Dayynime-v5" di Aniku. Dikasih cache disk (50MB): kalau ONLINE,
    // respons yang sama dipakai ulang dari cache selama 1 jam (ngirit request +
    // bikin transisi antar-tab kerasa instan). Kalau device OFFLINE beneran,
    // fallback ke cache sampai 7 hari daripada blank/error total.
    private val animeCache: Cache? = context?.let {
        Cache(File(it.cacheDir, "animeinweb_http_cache"), 50L * 1024 * 1024)
    }

    private fun isNetworkAvailable(): Boolean {
        val ctx = context ?: return true // gak ada context = anggap online, gak override apa-apa
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private val animeClient = OkHttpClient.Builder()
        .apply { animeCache?.let { cache(it) } }
        .addInterceptor { chain ->
            var request = chain.request()
            if (!isNetworkAvailable()) {
                request = request.newBuilder()
                    .cacheControl(CacheControl.Builder().maxStale(7, TimeUnit.DAYS).build())
                    .build()
            }
            chain.proceed(request)
        }
        .addNetworkInterceptor { chain ->
            val response = chain.proceed(chain.request())
            val cacheControl = CacheControl.Builder().maxAge(1, TimeUnit.HOURS).build()
            response.newBuilder()
                .header("Cache-Control", cacheControl.toString())
                .removeHeader("Pragma")
                .build()
        }
        .build()

    private val animeinwebApi: AnimeinwebApiService = Retrofit.Builder()
        .baseUrl(AnimeinwebApiService.BASE_URL)
        .client(animeClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(AnimeinwebApiService::class.java)

    // MANGA (tidak diubah)
    suspend fun getMangaHome(): MangaHomeResponse = api.getMangaHome()

    suspend fun getMangaDetail(rawUrl: String): MangaDetailResponse {
        return api.getMangaInfo(rawUrl).data ?: MangaDetailResponse()
    }

    suspend fun getMangaChapter(rawUrl: String): MangaDownloadResponse {
        return api.getMangaChapter(rawUrl)
    }

    suspend fun searchManga(query: String): MangaHomeResponse = api.searchManga(query)

    // ANIME - dipetain balik ke AnimeHomeResponse/AnimeDetailResponse/dst biar UI
    // (AnimeViewModel.kt, AnimeScreen.kt) sama sekali gak perlu diubah.

    // PENTING: parameter "page" di sini itu RAW page upstream (0-indexed),
    // BUKAN nomor halaman UI biasa (1,2,3..). Backend /api/search bisa
    // "ngelompatin" beberapa halaman upstream sekaligus dalam 1 request (biar
    // filter status/type gak gampang mentok), jadi request berikutnya WAJIB
    // pakai next_page yang dibalikin server - bukan page+1 asal. pagination.currentPage
    // di response ini dipakai buat NAMPUNG next_page itu, dikirim balik apa
    // adanya sama caller (AnimeViewModel) pas manggil loadMore.
    suspend fun getAnimeHome(type: String? = null, page: Int = 0): AnimeHomeResponse {
        var nextPage: Int? = null
        val list: List<AnimeinwebItem> = when (type) {
            "ongoing" -> {
                val res = animeinwebApi.search(status = "ONGOING", page = page)
                nextPage = res.next_page
                res.results ?: emptyList()
            }
            "completed" -> {
                val res = animeinwebApi.search(status = "FINISHED", page = page)
                nextPage = res.next_page
                res.results ?: emptyList()
            }
            "movies" -> {
                val res = animeinwebApi.search(type = "MOVIE", page = page)
                nextPage = res.next_page
                res.results ?: emptyList()
            }
            "latest" -> {
                val res = animeinwebApi.search(sort = "latest", page = page)
                nextPage = res.next_page
                res.results ?: emptyList()
            }
            "schedule" -> animeinwebApi.getSchedule(day = todayIndonesianDay()) // gak ada next page, 1 hari doang
            else -> {
                // "home" (default/null): gabungin beberapa section homepage jadi 1
                // feed, dedup by id, biar gak cuma nunjukin 1 kategori doang. Gak
                // ada next page karena ini emang cuma preview tetap, bukan list panjang.
                val home = animeinwebApi.getHome()
                val combined = LinkedHashMap<String, AnimeinwebItem>()
                for (item in (home.hot ?: emptyList()) + (home.new ?: emptyList()) +
                    (home.popular ?: emptyList()) + (home.waiting ?: emptyList())) {
                    combined[item.id] = item
                }
                combined.values.toList()
            }
        }
        return AnimeHomeResponse(
            status = "success",
            animes = list.map { it.toAnimeItem() },
            pagination = AnimePagination(hasNext = nextPage != null, currentPage = nextPage ?: 0)
        )
    }

    suspend fun searchAnime(query: String, page: Int = 0): AnimeHomeResponse {
        val res = animeinwebApi.search(keyword = query, page = page)
        return AnimeHomeResponse(
            status = "success",
            animes = (res.results ?: emptyList()).map { it.toAnimeItem() },
            pagination = AnimePagination(hasNext = res.next_page != null, currentPage = res.next_page ?: 0)
        )
    }

    suspend fun getAnimeDetail(slug: String): AnimeDetailResponse {
        val detail = animeinwebApi.getDetail(slug)
        // Episode dipaginasi upstream (30/halaman) - loop semua halaman biar anime
        // yang episode-nya banyak (One Piece dkk) gak keptong 30 doang. Batch
        // pertama itu TANPA page param (bukan page=1 - itu udah batch kedua).
        val allEpisodes = mutableListOf<AnimeinwebEpisodeItem>()
        val firstBatch = animeinwebApi.getEpisodes(slug, page = null)
        allEpisodes.addAll(firstBatch)
        if (firstBatch.isNotEmpty()) {
            var epPage = 1
            val maxEpisodePages = 60 // ~1800 episode, jauh di atas anime terpanjang yang ada
            while (epPage <= maxEpisodePages) {
                val pageResult = animeinwebApi.getEpisodes(slug, page = epPage)
                if (pageResult.isEmpty()) break
                allEpisodes.addAll(pageResult)
                epPage++
            }
        }
        return detail.toAnimeDetailResponse(allEpisodes)
    }

    suspend fun getAnimeEpisode(slug: String): AnimeEpisodeDetailResponse =
        animeinwebApi.getEpisodeStream(slug).toAnimeEpisodeDetailResponse()

    suspend fun getAnimeGenres(): List<GenreItem> {
        val res = animeinwebApi.getGenres()
        val rawList = res.map { it.toGenreItem() }
        // FILTER OUT ECCHI (dipertahankan sama seperti sebelumnya)
        return rawList.filter { it.name.isNotBlank() && !it.name.equals("ecchi", ignoreCase = true) && !it.slug.equals("ecchi", ignoreCase = true) }
    }

    suspend fun getAnimeByGenre(slug: String, page: Int = 0): AnimeHomeResponse {
        val res = animeinwebApi.search(genreIn = slug, page = page)
        return AnimeHomeResponse(
            status = "success",
            animes = (res.results ?: emptyList()).map { it.toAnimeItem() },
            pagination = AnimePagination(hasNext = res.next_page != null, currentPage = res.next_page ?: 0)
        )
    }

    // Dipisah dari getAnimeHome(type="schedule") karena ScheduleScreen butuh
    // milih hari sendiri (Senin..Minggu), bukan cuma "hari ini" kayak tab
    // Jadwal yang lama. Return raw AnimeinwebItem (bukan AnimeItem) soalnya
    // ScheduleScreen butuh field key_time/genre/views yang gak ada di AnimeItem.
    suspend fun getScheduleForDay(day: String): List<AnimeinwebItem> {
        return animeinwebApi.getSchedule(day = day)
    }

    private fun todayIndonesianDay(): String {
        val days = arrayOf("MINGGU", "SENIN", "SELASA", "RABU", "KAMIS", "JUMAT", "SABTU")
        val idx = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 // Calendar: Sunday=1
        return days.getOrElse(idx) { "MINGGU" }
    }
}
