package com.rakku.app.ui.manga

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakku.app.data.local.SessionManager
import com.rakku.app.data.model.MangaDetailResponse
import com.rakku.app.data.model.MangaDownloadResponse
import com.rakku.app.data.model.MangaItem
import com.rakku.app.data.remote.RakkuApiRepository
import com.rakku.app.data.remote.SupabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class MangaListUiState {
    object Loading : MangaListUiState()
    data class Success(val latest: List<MangaItem>, val popular: List<MangaItem>) : MangaListUiState()
    data class Error(val message: String) : MangaListUiState()
}

sealed class MangaDetailUiState {
    object Idle : MangaDetailUiState()
    object Loading : MangaDetailUiState()
    data class Success(val detail: MangaDetailResponse, val isBookmarked: Boolean) : MangaDetailUiState()
    data class Error(val message: String) : MangaDetailUiState()
}

sealed class MangaReaderUiState {
    object Idle : MangaReaderUiState()
    object Loading : MangaReaderUiState()
    data class Success(val chapterData: MangaDownloadResponse) : MangaReaderUiState()
    data class Error(val message: String) : MangaReaderUiState()
}

class MangaViewModel(
    private val rakkuApiRepository: RakkuApiRepository,
    private val supabaseRepository: SupabaseRepository,
    val sessionManager: SessionManager
) : ViewModel() {

    private val _listState = MutableStateFlow<MangaListUiState>(MangaListUiState.Loading)
    val listState: StateFlow<MangaListUiState> = _listState

    private val _detailState = MutableStateFlow<MangaDetailUiState>(MangaDetailUiState.Idle)
    val detailState: StateFlow<MangaDetailUiState> = _detailState

    private val _readerState = MutableStateFlow<MangaReaderUiState>(MangaReaderUiState.Idle)
    val readerState: StateFlow<MangaReaderUiState> = _readerState

    var searchQuery = MutableStateFlow("")

    init {
        loadMangaList()
    }

    fun loadMangaList() {
        searchQuery.value = ""
        viewModelScope.launch {
            _listState.value = MangaListUiState.Loading
            try {
                val res = rakkuApiRepository.getMangaHome()
                val latest = res.data ?: res.latest ?: emptyList()
                val popular = res.popular ?: emptyList()
                _listState.value = MangaListUiState.Success(latest, popular)
            } catch (e: Exception) {
                _listState.value = MangaListUiState.Error(e.message ?: "Gagal memuat manga")
            }
        }
    }

    fun searchManga(query: String) {
        searchQuery.value = query
        if (query.isBlank()) {
            loadMangaList()
            return
        }
        viewModelScope.launch {
            _listState.value = MangaListUiState.Loading
            try {
                val res = rakkuApiRepository.searchManga(query)
                val searchResults = res.data ?: res.latest ?: emptyList()
                _listState.value = MangaListUiState.Success(searchResults, emptyList())
            } catch (e: Exception) {
                _listState.value = MangaListUiState.Error(e.message ?: "Gagal mencari manga")
            }
        }
    }

    fun loadMangaDetail(url: String) {
        viewModelScope.launch {
            _detailState.value = MangaDetailUiState.Loading
            try {
                val detail = rakkuApiRepository.getMangaDetail(url)
                val userId = sessionManager.getUserId()
                var isBookmarked = false
                if (userId != null) {
                    val bookmarks = supabaseRepository.getBookmarks(userId)
                    isBookmarked = bookmarks.any { it.ref_id == url && it.content_type == "manga" }
                }
                _detailState.value = MangaDetailUiState.Success(detail, isBookmarked)
            } catch (e: Exception) {
                _detailState.value = MangaDetailUiState.Error(e.message ?: "Gagal memuat detail manga")
            }
        }
    }

    fun toggleBookmark(url: String, title: String, thumb: String?) {
        val userId = sessionManager.getUserId() ?: return
        val current = _detailState.value
        if (current is MangaDetailUiState.Success) {
            viewModelScope.launch {
                if (current.isBookmarked) {
                    val bookmarks = supabaseRepository.getBookmarks(userId)
                    val target = bookmarks.firstOrNull { it.ref_id == url && it.content_type == "manga" }
                    target?.id?.let { supabaseRepository.removeBookmark(it) }
                    _detailState.value = current.copy(isBookmarked = false)
                } else {
                    supabaseRepository.addBookmark(userId, "manga", url, title, thumb)
                    _detailState.value = current.copy(isBookmarked = true)
                }
            }
        }
    }

    fun loadChapter(chapterUrl: String) {
        viewModelScope.launch {
            _readerState.value = MangaReaderUiState.Loading
            try {
                val res = rakkuApiRepository.getMangaChapter(chapterUrl)
                _readerState.value = MangaReaderUiState.Success(res)
            } catch (e: Exception) {
                _readerState.value = MangaReaderUiState.Error(e.message ?: "Gagal memuat chapter manga")
            }
        }
    }
}
