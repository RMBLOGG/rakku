package com.rakku.app.data.model

import com.squareup.moshi.JsonClass

/**
 * Model mentah buat response JSON dari Sanka Comic API
 * (https://www.sankavollerei.web.id/comic/...), sumber data aslinya scrape
 * dari Komiku.org. File ini KHUSUS buat bentuk JSON asli si API - dipetain
 * ke MangaItem/MangaDetailResponse/dst (di MangaModels.kt) lewat
 * SankaComicMappers.kt supaya UI manga (MangaScreen, MangaDetailScreen,
 * MangaReaderScreen) sama sekali gak perlu diubah.
 */

@JsonClass(generateAdapter = true)
data class SankaComicListItem(
    val title: String = "",
    val link: String? = null,
    val image: String? = null,
    val chapter: String? = null,
    val time_ago: String? = null,
    val status: String? = null,
    val rating: String? = null,
    val genre: String? = null
)

@JsonClass(generateAdapter = true)
data class SankaComicPagination(
    val current_page: Int? = null,
    val per_page: Int? = null,
    val total: Int? = null,
    val total_on_page: Int? = null,
    val has_more: Boolean? = null
)

// Dipakai buat /comic/terbaru dan /comic/populer (bentuk JSON-nya sama)
@JsonClass(generateAdapter = true)
data class SankaComicListResponse(
    val creator: String? = null,
    val comics: List<SankaComicListItem>? = null,
    val pagination: SankaComicPagination? = null
)

// Dipakai buat /comic/genre/{slug}
@JsonClass(generateAdapter = true)
data class SankaComicGenreListResponse(
    val creator: String? = null,
    val genre: String? = null,
    val comics: List<SankaComicListItem>? = null,
    val pagination: SankaComicPagination? = null
)

@JsonClass(generateAdapter = true)
data class SankaComicSearchItem(
    val title: String = "",
    val altTitle: String? = null,
    val slug: String = "",
    val href: String? = null,
    val thumbnail: String? = null,
    val type: String? = null,
    val genre: String? = null,
    val description: String? = null
)

// Dipakai buat /comic/search?q=...
@JsonClass(generateAdapter = true)
data class SankaComicSearchResponse(
    val status: Boolean? = null,
    val creator: String? = null,
    val message: String? = null,
    val q: String? = null,
    val total: Int? = null,
    val data: List<SankaComicSearchItem>? = null
)

@JsonClass(generateAdapter = true)
data class SankaComicMetadata(
    val type: String? = null,
    val author: String? = null,
    val status: String? = null,
    val concept: String? = null,
    val age_rating: String? = null,
    val reading_direction: String? = null
)

@JsonClass(generateAdapter = true)
data class SankaComicGenreRef(
    val name: String = "",
    val slug: String? = null,
    val link: String? = null
)

@JsonClass(generateAdapter = true)
data class SankaComicChapterRef(
    val chapter: String = "",
    val slug: String = "",
    val link: String? = null,
    val date: String? = null
)

// Dipakai buat /comic/comic/{slug} (detail komik)
@JsonClass(generateAdapter = true)
data class SankaComicDetailResponse(
    val creator: String? = null,
    val slug: String? = null,
    val title: String? = null,
    val title_indonesian: String? = null,
    val image: String? = null,
    val synopsis: String? = null,
    val metadata: SankaComicMetadata? = null,
    val genres: List<SankaComicGenreRef>? = null,
    val chapters: List<SankaComicChapterRef>? = null
)

@JsonClass(generateAdapter = true)
data class SankaComicNavigation(
    val previousChapter: String? = null,
    val nextChapter: String? = null,
    val chapterList: String? = null
)

// Dipakai buat /comic/chapter/{chapterSlug} (baca gambar per chapter)
@JsonClass(generateAdapter = true)
data class SankaComicChapterResponse(
    val creator: String? = null,
    val manga_title: String? = null,
    val chapter_title: String? = null,
    val navigation: SankaComicNavigation? = null,
    val images: List<String>? = null,
    // Sama kayak "images" tapi link-nya udah dibungkus lewat proxy
    // (.../comic/proxy?url=...) - dipakai kalau load gambar langsung
    // ke img.komiku.org gagal (hotlink/CORS diblok).
    val imagesproxy: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class SankaComicGenreItem(
    val value: String = "",
    val name: String = ""
)

// Dipakai buat /comic/genres (list semua genre)
@JsonClass(generateAdapter = true)
data class SankaComicGenreListWrapper(
    val status: Boolean? = null,
    val creator: String? = null,
    val message: String? = null,
    val data: List<SankaComicGenreItem>? = null
)
