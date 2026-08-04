package com.rakku.app.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserProfile(
    val id: String = "",
    val username: String? = null,
    val role: String? = "user",
    val level: Int? = 1,
    val exp: Int? = 0,
    val is_banned: Boolean? = false,
    val banned_reason: String? = null,
    val banned_until: String? = null,
    val has_unlimited: Boolean? = false,
    val avatar_url: String? = null,
    val rakku_coin: Int? = 0,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthUser(
    val id: String = "",
    val email: String? = null,
    val user_metadata: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val access_token: String? = null,
    val token_type: String? = null,
    val expires_in: Long? = null,
    val refresh_token: String? = null,
    val user: AuthUser? = null,
    val error: String? = null,
    val error_description: String? = null,
    // Supabase Auth (GoTrue) versi terbaru balikin error pakai field ini, BUKAN
    // "error"/"error_description" di atas (itu format lama/OAuth-style). Tanpa
    // field ini, pesan error asli dari server (mis. "User already registered",
    // "Password should be at least 6 characters") gak pernah kebaca, selalu
    // jatuh ke pesan generik.
    val msg: String? = null,
    val message: String? = null,
    val error_code: String? = null
) {
    val friendlyError: String?
        get() = error_description ?: msg ?: message ?: error
}

@JsonClass(generateAdapter = true)
data class BookmarkItem(
    val id: Long? = null,
    val user_id: String = "",
    val content_type: String = "", // 'anime' or 'manga'
    val ref_id: String = "",
    val title: String = "",
    val thumb: String? = null,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class HistoryItem(
    val id: Long? = null,
    val user_id: String = "",
    val content_type: String = "", // 'anime' or 'manga'
    val ref_id: String = "",
    val title: String = "",
    val thumb: String? = null,
    val progress_id: String? = null,
    val progress_name: String? = null,
    val updated_at: String? = null
)

@JsonClass(generateAdapter = true)
data class GlobalChatMessage(
    val id: Long? = null,
    val user_id: String = "",
    val message: String = "",
    val created_at: String? = null,
    var username: String? = null,
    var avatar_url: String? = null,
    var role: String? = null
)

@JsonClass(generateAdapter = true)
data class TopupRequest(
    val id: Long? = null,
    val user_id: String = "",
    val amount_coin: Int = 0,
    val price: String = "",
    val status: String = "pending", // 'pending' | 'approved' | 'rejected'
    val proof_note: String? = null,
    val created_at: String? = null,
    val approved_by: String? = null,
    val approved_at: String? = null,
    var username: String? = null
)

@JsonClass(generateAdapter = true)
data class EpisodeComment(
    val id: String? = null,
    val anime_slug: String = "",
    val episode_slug: String = "",
    val user_id: String = "",
    val message: String = "",
    val created_at: String? = null,
    var username: String? = null,
    var avatar_url: String? = null,
    var role: String? = null
)

@JsonClass(generateAdapter = true)
data class CommentReport(
    val id: String? = null,
    val comment_id: String = "",
    val reporter_id: String = "",
    val category: String = "", // 'spam' | 'promosi' | '18+' | 'lainnya'
    val description: String? = null,
    val status: String? = "pending",
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class FeedbackReport(
    val id: Long? = null,
    val user_id: String = "",
    val type: String = "saran", // 'saran' | 'laporan'
    val message: String = "",
    val status: String? = "open", // 'open' | 'in_progress' | 'closed'
    val created_at: String? = null,
    var username: String? = null
)

@JsonClass(generateAdapter = true)
data class Announcement(
    val id: Long? = null,
    val title: String = "",
    val content: String = "",
    val is_active: Boolean = true,
    val created_by: String? = null,
    val created_at: String? = null
)
