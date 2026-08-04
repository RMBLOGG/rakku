package com.rakku.app.data.remote

import android.content.Context
import android.net.Uri
import com.rakku.app.data.local.SessionManager
import com.rakku.app.data.model.Announcement
import com.rakku.app.data.model.AuthResponse
import com.rakku.app.data.model.BookmarkItem
import com.rakku.app.data.model.CommentReport
import com.rakku.app.data.model.EpisodeComment
import com.rakku.app.data.model.FeedbackReport
import com.rakku.app.data.model.GlobalChatMessage
import com.rakku.app.data.model.HistoryItem
import com.rakku.app.data.model.TopupRequest
import com.rakku.app.data.model.UserProfile
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream

class SupabaseRepository(
    private val sessionManager: SessionManager
) {
    companion object {
        const val SUPABASE_URL = "https://lqixsabpmyflguisblrb.supabase.co"
        const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxxaXhzYWJwbXlmbGd1aXNibHJiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODMyNjI4NDcsImV4cCI6MjA5ODgzODg0N30.QVdxWkMguIbJ0T5uqomBKwN7PBAYeb_xNjRfh67W1-E"
    }

    private val client = OkHttpClient.Builder().build()
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getAuthToken(): String {
        return sessionManager.getAccessToken() ?: SUPABASE_ANON_KEY
    }

    private fun newRequestBuilder(url: String): Request.Builder {
        val token = getAuthToken()
        return Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Prefer", "return=representation")
    }

    // EXP: manggil RPC "award_exp_once" (sudah ada di database, sama seperti yang
    // dipakai website di anime.js). eventKey harus unik per kejadian (mis.
    // "anime_open:{slug}:{episodeSlug}") supaya EXP gak dobel kalau dipanggil
    // berkali-kali untuk kejadian yang sama - RPC ini sendiri yang jaga idempotensi
    // di sisi server, return true kalau baru pertama kali (EXP ditambahkan),
    // false kalau sudah pernah (EXP tidak ditambahkan lagi).
    suspend fun awardExp(eventKey: String, amount: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val bodyJson = moshi.adapter(Map::class.java).toJson(
                mapOf("p_event_key" to eventKey, "p_amount" to amount)
            )
            val request = newRequestBuilder("$SUPABASE_URL/rest/v1/rpc/award_exp_once")
                .post(bodyJson.toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()?.trim() == "true"
            } else false
        } catch (e: Exception) {
            false
        }
    }

    // AUTH
    suspend fun signUp(email: String, password: String, username: String): AuthResponse = withContext(Dispatchers.IO) {
        val bodyJson = moshi.adapter(Map::class.java).toJson(
            mapOf(
                "email" to email,
                "password" to password,
                "data" to mapOf("username" to username)
            )
        )
        val request = Request.Builder()
            // redirect_to = deep link app, biar pas user klik link konfirmasi di
            // email, dia balik ke app Rakku langsung (bukan ke website). URL ini
            // WAJIB juga didaftarin di Supabase Dashboard -> Authentication ->
            // URL Configuration -> Redirect URLs, kalau enggak, Supabase bakal
            // nolak/abaikan redirect_to ini dan tetap fallback ke Site URL.
            .url("$SUPABASE_URL/auth/v1/signup?redirect_to=rakku://login-callback")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .post(bodyJson.toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        val authRes = moshi.adapter(AuthResponse::class.java).fromJson(responseBody) ?: AuthResponse(error = "Unknown error")
        
        if (response.isSuccessful && authRes.access_token != null && authRes.user != null) {
            sessionManager.saveSession(authRes.access_token, authRes.user.id)
            // ensure profile entry exists if needed
        }
        authRes
    }

    suspend fun signIn(email: String, password: String): AuthResponse = withContext(Dispatchers.IO) {
        val bodyJson = moshi.adapter(Map::class.java).toJson(
            mapOf("email" to email, "password" to password)
        )
        val request = Request.Builder()
            .url("$SUPABASE_URL/auth/v1/token?grant_type=password")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .post(bodyJson.toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        val authRes = moshi.adapter(AuthResponse::class.java).fromJson(responseBody) ?: AuthResponse(error = "Gagal login")

        if (response.isSuccessful && authRes.access_token != null && authRes.user != null) {
            sessionManager.saveSession(authRes.access_token, authRes.user.id)
        }
        authRes
    }

    suspend fun fetchUserProfile(userId: String): UserProfile? = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/profiles?id=eq.$userId")
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val responseBody = response.body?.string() ?: ""
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, UserProfile::class.java)
            val list = moshi.adapter<List<UserProfile>>(type).fromJson(responseBody)
            val profile = list?.firstOrNull()
            sessionManager.updateProfile(profile)
            profile
        } else null
    }

    suspend fun updateUserProfile(userId: String, username: String?, avatarUrl: String?): Boolean = withContext(Dispatchers.IO) {
        val map = mutableMapOf<String, Any>()
        if (username != null) map["username"] = username
        if (avatarUrl != null) map["avatar_url"] = avatarUrl

        val bodyJson = moshi.adapter(Map::class.java).toJson(map)
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/profiles?id=eq.$userId")
            .patch(bodyJson.toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        response.isSuccessful
    }

    // BAN LOGIC RPC
    suspend fun clearExpiredBan(userId: String): Boolean = withContext(Dispatchers.IO) {
        val bodyJson = moshi.adapter(Map::class.java).toJson(mapOf("target_id" to userId))
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/rpc/clear_expired_ban")
            .post(bodyJson.toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        response.isSuccessful
    }

    // UPLOAD AVATAR TO BUCKET 'avatars'
    suspend fun uploadAvatar(context: Context, userId: String, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: return@withContext null
            val fileName = "avatar_${System.currentTimeMillis()}.jpg"
            val path = "avatars/$userId/$fileName"
            val uploadUrl = "$SUPABASE_URL/storage/v1/object/$path"

            val imageMediaType = "image/jpeg".toMediaType()
            val request = Request.Builder()
                .url(uploadUrl)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${getAuthToken()}")
                .addHeader("x-upsert", "true")
                .post(bytes.toRequestBody(imageMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                "$SUPABASE_URL/storage/v1/object/public/$path"
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ANNOUNCEMENTS
    suspend fun getActiveAnnouncements(): List<Announcement> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/announcements?is_active=eq.true&order=created_at.desc")
            .get().build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, Announcement::class.java)
            moshi.adapter<List<Announcement>>(type).fromJson(response.body?.string() ?: "") ?: emptyList()
        } else emptyList()
    }

    suspend fun getAllAnnouncements(): List<Announcement> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/announcements?order=created_at.desc")
            .get().build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, Announcement::class.java)
            moshi.adapter<List<Announcement>>(type).fromJson(response.body?.string() ?: "") ?: emptyList()
        } else emptyList()
    }

    suspend fun createAnnouncement(title: String, content: String, active: Boolean): Boolean = withContext(Dispatchers.IO) {
        val map = mapOf(
            "title" to title,
            "content" to content,
            "is_active" to active,
            "created_by" to sessionManager.getUserId()
        )
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/announcements")
            .post(moshi.adapter(Map::class.java).toJson(map).toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().isSuccessful
    }

    suspend fun toggleAnnouncement(id: Long, active: Boolean): Boolean = withContext(Dispatchers.IO) {
        val map = mapOf("is_active" to active)
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/announcements?id=eq.$id")
            .patch(moshi.adapter(Map::class.java).toJson(map).toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().isSuccessful
    }

    // BOOKMARKS & HISTORY
    suspend fun getBookmarks(userId: String): List<BookmarkItem> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/bookmarks?user_id=eq.$userId&order=created_at.desc")
            .get().build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, BookmarkItem::class.java)
            moshi.adapter<List<BookmarkItem>>(type).fromJson(response.body?.string() ?: "") ?: emptyList()
        } else emptyList()
    }

    suspend fun addBookmark(userId: String, contentType: String, refId: String, title: String, thumb: String?): Boolean = withContext(Dispatchers.IO) {
        val map = mapOf(
            "user_id" to userId,
            "content_type" to contentType,
            "ref_id" to refId,
            "title" to title,
            "thumb" to thumb
        )
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/bookmarks")
            .post(moshi.adapter(Map::class.java).toJson(map).toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().isSuccessful
    }

    suspend fun removeBookmark(id: Long): Boolean = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/bookmarks?id=eq.$id")
            .delete().build()
        client.newCall(request).execute().isSuccessful
    }

    suspend fun clearAllBookmarks(userId: String): Boolean = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/bookmarks?user_id=eq.$userId")
            .delete().build()
        client.newCall(request).execute().isSuccessful
    }

    suspend fun getWatchHistory(userId: String): List<HistoryItem> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/history?user_id=eq.$userId&content_type=eq.anime&order=updated_at.desc")
            .get().build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, HistoryItem::class.java)
            moshi.adapter<List<HistoryItem>>(type).fromJson(response.body?.string() ?: "") ?: emptyList()
        } else emptyList()
    }

    suspend fun deleteHistoryItem(id: Long): Boolean = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/history?id=eq.$id")
            .delete().build()
        client.newCall(request).execute().isSuccessful
    }

    suspend fun clearAllHistory(userId: String): Boolean = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/history?user_id=eq.$userId&content_type=eq.anime")
            .delete().build()
        client.newCall(request).execute().isSuccessful
    }

    suspend fun saveWatchHistory(userId: String, refId: String, title: String, thumb: String?, progressId: String, progressName: String): Boolean = withContext(Dispatchers.IO) {
        val map = mapOf(
            "user_id" to userId,
            "content_type" to "anime",
            "ref_id" to refId,
            "title" to title,
            "thumb" to thumb,
            "progress_id" to progressId,
            "progress_name" to progressName
        )
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/history")
            .post(moshi.adapter(Map::class.java).toJson(map).toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().isSuccessful
    }

    // EPISODE COMMENTS & COMMENT REPORTS
    suspend fun getEpisodeComments(animeSlug: String, episodeSlug: String): List<EpisodeComment> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/episode_comments?anime_slug=eq.$animeSlug&episode_slug=eq.$episodeSlug&order=created_at.asc")
            .get().build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, EpisodeComment::class.java)
            val comments = moshi.adapter<List<EpisodeComment>>(type).fromJson(response.body?.string() ?: "") ?: emptyList()
            // Attach user profiles
            attachProfilesToComments(comments)
        } else emptyList()
    }

    suspend fun getUserComments(userId: String): List<EpisodeComment> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/episode_comments?user_id=eq.$userId&order=created_at.desc")
            .get().build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, EpisodeComment::class.java)
            moshi.adapter<List<EpisodeComment>>(type).fromJson(response.body?.string() ?: "") ?: emptyList()
        } else emptyList()
    }

    private suspend fun attachProfilesToComments(comments: List<EpisodeComment>): List<EpisodeComment> {
        val userIds = comments.map { it.user_id }.distinct()
        if (userIds.isEmpty()) return comments
        val profilesMap = fetchProfilesMap(userIds)
        comments.forEach { c ->
            val p = profilesMap[c.user_id]
            c.username = p?.username ?: "User"
            c.avatar_url = p?.avatar_url
            c.role = p?.role ?: "user"
        }
        return comments
    }

    suspend fun postEpisodeComment(animeSlug: String, episodeSlug: String, message: String): Boolean = withContext(Dispatchers.IO) {
        val userId = sessionManager.getUserId() ?: return@withContext false
        val map = mapOf(
            "anime_slug" to animeSlug,
            "episode_slug" to episodeSlug,
            "user_id" to userId,
            "message" to message
        )
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/episode_comments")
            .post(moshi.adapter(Map::class.java).toJson(map).toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().isSuccessful
    }

    suspend fun deleteEpisodeComment(id: String): Boolean = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/episode_comments?id=eq.$id")
            .delete().build()
        client.newCall(request).execute().isSuccessful
    }

    suspend fun reportComment(commentId: String, category: String, description: String?): Boolean = withContext(Dispatchers.IO) {
        val userId = sessionManager.getUserId() ?: return@withContext false
        val map = mapOf(
            "comment_id" to commentId,
            "reporter_id" to userId,
            "category" to category,
            "description" to description,
            "status" to "pending"
        )
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/comment_reports")
            .post(moshi.adapter(Map::class.java).toJson(map).toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().isSuccessful
    }

    suspend fun getCommentReports(): List<CommentReport> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/comment_reports?order=created_at.desc")
            .get().build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, CommentReport::class.java)
            moshi.adapter<List<CommentReport>>(type).fromJson(response.body?.string() ?: "") ?: emptyList()
        } else emptyList()
    }

    // GLOBAL CHAT
    suspend fun getGlobalChatMessages(): List<GlobalChatMessage> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/global_chat_messages?order=created_at.desc&limit=50")
            .get().build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, GlobalChatMessage::class.java)
            val list = moshi.adapter<List<GlobalChatMessage>>(type).fromJson(response.body?.string() ?: "") ?: emptyList()
            // PENTING: tabel global_chat_messages SUDAH punya kolom username/avatar_url/role
            // sendiri (diisi otomatis oleh trigger DB pas insert, sama seperti di website -
            // lihat chat.js yang cuma .select("*") tanpa join). Jangan ditimpa lagi dengan
            // hasil lookup manual ke tabel profiles, karena kalau lookup itu gagal/kosong
            // (mis. RLS profiles membatasi baca profil user lain), semua username malah
            // ke-reset jadi fallback "User" walau data aslinya sudah benar dari response ini.
            list.reversed().onEach { msg ->
                if (msg.username.isNullOrBlank()) msg.username = "User"
            }
        } else emptyList()
    }

    suspend fun sendGlobalChatMessage(message: String): Boolean = withContext(Dispatchers.IO) {
        val userId = sessionManager.getUserId() ?: return@withContext false
        val map = mapOf(
            "user_id" to userId,
            "message" to message
        )
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/global_chat_messages")
            .post(moshi.adapter(Map::class.java).toJson(map).toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().isSuccessful
    }

    // TOPUP REQUESTS
    suspend fun createTopupRequest(amountCoin: Int, price: String): Boolean = withContext(Dispatchers.IO) {
        val userId = sessionManager.getUserId() ?: return@withContext false
        val map = mapOf(
            "user_id" to userId,
            "amount_coin" to amountCoin,
            "price" to price,
            "status" to "pending"
        )
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/topup_requests")
            .post(moshi.adapter(Map::class.java).toJson(map).toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().isSuccessful
    }

    // FEEDBACK & REPORTS
    suspend fun submitFeedback(type: String, message: String): Boolean = withContext(Dispatchers.IO) {
        val userId = sessionManager.getUserId() ?: return@withContext false
        val map = mapOf(
            "user_id" to userId,
            "type" to type,
            "message" to message,
            "status" to "open"
        )
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/feedback_reports")
            .post(moshi.adapter(Map::class.java).toJson(map).toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().isSuccessful
    }

    suspend fun getFeedbackReports(): List<FeedbackReport> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/feedback_reports?order=created_at.desc")
            .get().build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, FeedbackReport::class.java)
            val list = moshi.adapter<List<FeedbackReport>>(type).fromJson(response.body?.string() ?: "") ?: emptyList()
            val userIds = list.map { it.user_id }.distinct()
            val profiles = fetchProfilesMap(userIds)
            list.forEach { f ->
                f.username = profiles[f.user_id]?.username ?: "User"
            }
            list
        } else emptyList()
    }

    suspend fun updateFeedbackStatus(id: Long, newStatus: String): Boolean = withContext(Dispatchers.IO) {
        val map = mapOf("status" to newStatus)
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/feedback_reports?id=eq.$id")
            .patch(moshi.adapter(Map::class.java).toJson(map).toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().isSuccessful
    }

    // ADMIN RPC CALLS
    suspend fun adminBanUser(targetId: String, reason: String?, durationHours: Int?): Boolean = withContext(Dispatchers.IO) {
        val map = mutableMapOf<String, Any?>("target_id" to targetId, "reason" to reason)
        if (durationHours != null) map["duration_hours"] = durationHours
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/rpc/admin_ban_user")
            .post(moshi.adapter(Map::class.java).toJson(map).toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().isSuccessful
    }

    suspend fun adminUnbanUser(targetId: String): Boolean = withContext(Dispatchers.IO) {
        val map = mapOf("target_id" to targetId)
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/rpc/admin_unban_user")
            .post(moshi.adapter(Map::class.java).toJson(map).toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().isSuccessful
    }

    suspend fun adminAddCoin(targetId: String, amount: Int, topupId: Long?): Boolean = withContext(Dispatchers.IO) {
        val map = mutableMapOf<String, Any?>("target_id" to targetId, "amount" to amount)
        if (topupId != null) map["topup_id"] = topupId
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/rpc/admin_add_coin")
            .post(moshi.adapter(Map::class.java).toJson(map).toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().isSuccessful
    }

    suspend fun adminSetRole(targetId: String, newRole: String): Boolean = withContext(Dispatchers.IO) {
        val map = mapOf("target_id" to targetId, "new_role" to newRole)
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/rpc/admin_set_role")
            .post(moshi.adapter(Map::class.java).toJson(map).toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().isSuccessful
    }

    suspend fun adminSetLevel(targetId: String, newLevel: Int): Boolean = withContext(Dispatchers.IO) {
        val map = mapOf("target_id" to targetId, "new_level" to newLevel)
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/rpc/admin_set_level")
            .post(moshi.adapter(Map::class.java).toJson(map).toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().isSuccessful
    }

    suspend fun adminAddExp(targetId: String, amount: Int): Boolean = withContext(Dispatchers.IO) {
        val map = mapOf("target_id" to targetId, "amount" to amount)
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/rpc/admin_add_exp")
            .post(moshi.adapter(Map::class.java).toJson(map).toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().isSuccessful
    }

    suspend fun adminSetUnlimited(targetId: String, enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        val map = mapOf("target_id" to targetId, "enabled" to enabled)
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/rpc/admin_set_unlimited")
            .post(moshi.adapter(Map::class.java).toJson(map).toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().isSuccessful
    }

    suspend fun getAllProfiles(): List<UserProfile> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/profiles?order=created_at.desc")
            .get().build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, UserProfile::class.java)
            moshi.adapter<List<UserProfile>>(type).fromJson(response.body?.string() ?: "") ?: emptyList()
        } else emptyList()
    }

    private suspend fun fetchProfilesMap(userIds: List<String>): Map<String, UserProfile> = withContext(Dispatchers.IO) {
        if (userIds.isEmpty()) return@withContext emptyMap()
        // Panggil RPC get_public_profiles (SECURITY DEFINER) - lihat
        // get_public_profiles_migration.sql. Ini WAJIB dijalankan dulu di Supabase,
        // karena query langsung ke tabel "profiles" buat user lain kemungkinan
        // diblokir RLS (profiles biasanya cuma izinin baca profil sendiri).
        val body = moshi.adapter(Map::class.java)
            .toJson(mapOf("ids" to userIds))
            .toRequestBody(jsonMediaType)
        val request = newRequestBuilder("$SUPABASE_URL/rest/v1/rpc/get_public_profiles")
            .post(body)
            .build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, UserProfile::class.java)
            val list = moshi.adapter<List<UserProfile>>(type).fromJson(response.body?.string() ?: "") ?: emptyList()
            list.associateBy { it.id }
        } else emptyMap()
    }
}
