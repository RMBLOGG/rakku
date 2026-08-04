package com.rakku.app.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakku.app.data.local.SessionManager
import com.rakku.app.data.model.Announcement
import com.rakku.app.data.model.BookmarkItem
import com.rakku.app.data.model.CommentReport
import com.rakku.app.data.model.FeedbackReport
import com.rakku.app.data.model.HistoryItem
import com.rakku.app.data.model.UserProfile
import com.rakku.app.data.remote.SupabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AdminUiState {
    object Idle : AdminUiState()
    object Loading : AdminUiState()
    data class Success(
        val users: List<UserProfile>,
        val feedbackList: List<FeedbackReport>,
        val commentReports: List<CommentReport>,
        val announcements: List<Announcement>
    ) : AdminUiState()
    data class Error(val message: String) : AdminUiState()
}

class ProfileViewModel(
    val sessionManager: SessionManager,
    private val supabaseRepository: SupabaseRepository
) : ViewModel() {

    val userProfile = sessionManager.currentUserProfile

    private val _bookmarks = MutableStateFlow<List<BookmarkItem>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkItem>> = _bookmarks

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history

    private val _adminState = MutableStateFlow<AdminUiState>(AdminUiState.Idle)
    val adminState: StateFlow<AdminUiState> = _adminState

    init {
        refreshProfile()
    }

    fun refreshProfile() {
        val userId = sessionManager.getUserId() ?: return
        viewModelScope.launch {
            val p = supabaseRepository.fetchUserProfile(userId)
            if (p != null) {
                _bookmarks.value = supabaseRepository.getBookmarks(userId)
                _history.value = supabaseRepository.getWatchHistory(userId)
            }
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            if (supabaseRepository.removeBookmark(id)) {
                _bookmarks.value = _bookmarks.value.filter { it.id != id }
            }
        }
    }

    fun clearAllBookmarks() {
        val userId = sessionManager.getUserId() ?: return
        viewModelScope.launch {
            if (supabaseRepository.clearAllBookmarks(userId)) {
                _bookmarks.value = emptyList()
            }
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            if (supabaseRepository.deleteHistoryItem(id)) {
                _history.value = _history.value.filter { it.id != id }
            }
        }
    }

    fun clearAllHistory() {
        val userId = sessionManager.getUserId() ?: return
        viewModelScope.launch {
            if (supabaseRepository.clearAllHistory(userId)) {
                _history.value = emptyList()
            }
        }
    }

    fun updateProfileInfo(context: Context, newUsername: String?, avatarUri: Uri?, onComplete: (Boolean) -> Unit) {
        val userId = sessionManager.getUserId() ?: return
        viewModelScope.launch {
            var avatarUrl: String? = null
            if (avatarUri != null) {
                avatarUrl = supabaseRepository.uploadAvatar(context, userId, avatarUri)
            }
            val success = supabaseRepository.updateUserProfile(userId, newUsername, avatarUrl)
            if (success) {
                supabaseRepository.fetchUserProfile(userId)
            }
            onComplete(success)
        }
    }

    fun createTopupRequest(amountCoin: Int, price: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = supabaseRepository.createTopupRequest(amountCoin, price)
            onResult(success)
        }
    }

    fun submitFeedback(type: String, message: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = supabaseRepository.submitFeedback(type, message)
            onResult(success)
        }
    }

    // ADMIN FUNCTIONS
    fun loadAdminData() {
        viewModelScope.launch {
            _adminState.value = AdminUiState.Loading
            try {
                val users = supabaseRepository.getAllProfiles()
                val feedback = supabaseRepository.getFeedbackReports()
                val commentReports = supabaseRepository.getCommentReports()
                val announcements = supabaseRepository.getAllAnnouncements()

                _adminState.value = AdminUiState.Success(
                    users = users,
                    feedbackList = feedback,
                    commentReports = commentReports,
                    announcements = announcements
                )
            } catch (e: Exception) {
                _adminState.value = AdminUiState.Error(e.message ?: "Gagal memuat data admin")
            }
        }
    }

    fun adminBanUser(targetId: String, reason: String?, durationHours: Int?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = supabaseRepository.adminBanUser(targetId, reason, durationHours)
            if (success) loadAdminData()
            onResult(success)
        }
    }

    fun adminUnbanUser(targetId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = supabaseRepository.adminUnbanUser(targetId)
            if (success) loadAdminData()
            onResult(success)
        }
    }

    fun adminAddCoin(targetId: String, amount: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = supabaseRepository.adminAddCoin(targetId, amount, null)
            if (success) loadAdminData()
            onResult(success)
        }
    }

    fun adminSetRole(targetId: String, newRole: String) {
        viewModelScope.launch {
            if (supabaseRepository.adminSetRole(targetId, newRole)) loadAdminData()
        }
    }

    fun createAnnouncement(title: String, content: String, active: Boolean, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = supabaseRepository.createAnnouncement(title, content, active)
            if (success) loadAdminData()
            onResult(success)
        }
    }

    fun toggleAnnouncement(id: Long, active: Boolean) {
        viewModelScope.launch {
            if (supabaseRepository.toggleAnnouncement(id, active)) loadAdminData()
        }
    }

    fun updateFeedbackStatus(id: Long, status: String) {
        viewModelScope.launch {
            if (supabaseRepository.updateFeedbackStatus(id, status)) loadAdminData()
        }
    }

    fun deleteReportedComment(commentId: String) {
        viewModelScope.launch {
            if (supabaseRepository.deleteEpisodeComment(commentId)) loadAdminData()
        }
    }

    fun logout() {
        sessionManager.clearSession()
    }
}
