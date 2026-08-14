package com.rakku.app.data.remote

import com.rakku.app.data.model.MangaChapterItem
import com.rakku.app.data.model.MangaDetailResponse
import com.rakku.app.data.model.MangaDownloadResponse
import com.rakku.app.data.model.MangaItem
import com.rakku.app.data.model.SankaComicChapterResponse
import com.rakku.app.data.model.SankaComicDetailResponse
import com.rakku.app.data.model.SankaComicListItem
import com.rakku.app.data.model.SankaComicSearchItem

/**
 * Endpoint /comic/terbaru, /comic/populer, dan /comic/genre/{slug} cuma
 * ngasih "link" (mis. "/manga/naruto/" atau "https://komiku.org/manga/naruto/"),
 * BUKAN slug polos. Field "url" di MangaItem dipakai sebagai identifier unik
 * ke seluruh app (bookmark, history, buka detail) - jadi di sini WAJIB
 * diekstrak jadi slug polos ("naruto") supaya bisa langsung dipakai manggil
 * GET /comic/comic/{slug}.
 */
fun extractComicSlug(link: String?): String {
    if (link.isNullOrBlank()) return ""
    val match = Regex("/manga/([^/?]+)/?").find(link)
    if (match != null) return match.groupValues[1]
    // fallback: bukan format "/manga/xxx/" (misal udah slug polos duluan)
    return link.trim('/').substringAfterLast('/')
}

fun SankaComicListItem.toMangaItem(): MangaItem = MangaItem(
    title = title,
    url = extractComicSlug(link),
    thumb = image,
    chapter = chapter,
    rating = rating,
    type = null
)

// Hasil /comic/search udah nyediain "slug" langsung, gak perlu diekstrak lagi
fun SankaComicSearchItem.toMangaItem(): MangaItem = MangaItem(
    title = title,
    url = slug,
    thumb = thumbnail,
    chapter = null,
    rating = null,
    type = type
)

fun SankaComicDetailResponse.toMangaDetailResponse(): MangaDetailResponse = MangaDetailResponse(
    title = title,
    thumb = image,
    synopsis = synopsis,
    author = metadata?.author,
    type = metadata?.type,
    rating = null,
    genres = genres?.map { it.name },
    // "slug" chapter di sini udah lengkap dgn nomor chapter (siap dipakai
    // langsung ke GET /comic/chapter/{slug}), disimpan di MangaChapterItem.url
    chapters = chapters?.map { MangaChapterItem(title = it.chapter, url = it.slug, date = it.date) },
    totalChapters = chapters?.size,
    status = metadata?.status
)

fun SankaComicChapterResponse.toMangaDownloadResponse(): MangaDownloadResponse = MangaDownloadResponse(
    status = "success",
    title = chapter_title ?: manga_title,
    // Utamain imagesproxy (dibungkus lewat server Sanka) buat ngehindarin
    // kemungkinan hotlink/CORS block dari img.komiku.org kalau diakses
    // langsung dari app. Fallback ke "images" kalau imagesproxy kosong.
    images = imagesproxy?.takeIf { it.isNotEmpty() } ?: images,
    nextUrl = navigation?.nextChapter,
    prevUrl = navigation?.previousChapter
)
