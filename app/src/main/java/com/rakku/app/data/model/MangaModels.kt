package com.rakku.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MangaItem(
    val title: String = "",
    // key asli JSON adalah "href" (link ke halaman detail manga), bukan "url"
    @Json(name = "href") val url: String = "",
    val thumb: String? = null,
    // key asli JSON adalah "lastChapter", bukan "chapter"
    @Json(name = "lastChapter") val chapter: String? = null,
    val rating: String? = null,
    val type: String? = null
)

@JsonClass(generateAdapter = true)
data class MangaHomeResponse(
    val status: String? = null,
    val latest: List<MangaItem>? = null,
    val popular: List<MangaItem>? = null,
    // "data" adalah key asli yang dipakai website untuk home & search manga
    val data: List<MangaItem>? = null
)

@JsonClass(generateAdapter = true)
data class MangaChapterItem(
    // key asli JSON adalah "name", bukan "title"
    @Json(name = "name") val title: String = "",
    // key asli JSON adalah "link", bukan "url"
    @Json(name = "link") val url: String = "",
    val date: String? = null
)

/**
 * Ini merepresentasikan objek "data" di dalam response asli:
 * { "status": ..., "data": { title, thumb, genre, chapters, synopsis, status, ... } }
 * Nama class & field SENGAJA dipertahankan sama seperti sebelumnya supaya
 * layar (MangaDetailScreen.kt) tidak perlu diubah sama sekali.
 */
@JsonClass(generateAdapter = true)
data class MangaDetailResponse(
    val title: String? = null,
    val thumb: String? = null,
    val synopsis: String? = null,
    val author: String? = null,
    val type: String? = null,
    val rating: String? = null,
    // key asli JSON adalah "genre" (tunggal), bukan "genres", dan bisa berupa
    // array of string ATAU array of object {name}
    @Json(name = "genre") val genres: List<String>? = null,
    val chapters: List<MangaChapterItem>? = null,
    val totalChapters: Int? = null,
    // status publikasi manga (mis. "Ongoing"/"Tamat"), field ini ADA di dalam
    // objek "data", beda dengan "status" di level atas yang cuma wrapper API
    val status: String? = null
)

/**
 * Wrapper mentah dari /api/manga/info: { status, data: {...} }.
 * Cuma dipakai internal di RakkuApiService/Repository untuk unwrap ".data".
 */
@JsonClass(generateAdapter = true)
data class MangaDetailApiEnvelope(
    val status: String? = null,
    val data: MangaDetailResponse? = null
)

@JsonClass(generateAdapter = true)
data class MangaDownloadResponse(
    val status: String? = null,
    val title: String? = null,
    val images: List<String>? = null,
    val nextUrl: String? = null,
    val prevUrl: String? = null
)
