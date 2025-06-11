package com.herukyatto.hncnote

import androidx.lifecycle.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    val searchQueryState: StateFlow<String> = searchQuery.asStateFlow()
    private val sortOrder = MutableStateFlow(SortOrder.BY_DATE_DESC)

    // Phơi bày trạng thái sortOrder ra bên ngoài một cách an toàn (chỉ đọc)
    val sortOrderState: StateFlow<SortOrder> = sortOrder.asStateFlow()

    private val notesFlow = sortOrder.flatMapLatest { order ->
        when (order) {
            SortOrder.BY_DATE_DESC -> repository.getNotesSortedByDateDesc()
            SortOrder.BY_DATE_ASC -> repository.getNotesSortedByDateAsc()
            SortOrder.BY_TITLE_ASC -> repository.getNotesSortedByTitle()
        }
    }

    val allNotes: LiveData<List<Note>> =
        combine(notesFlow, searchQuery) { notes, query ->
            if (query.isBlank()) {
                notes
            } else {
                val unaccentedQuery = StringUtils.unaccent(query) // Bỏ dấu từ khóa
                notes.filter { note ->
                    val fakeTitle = if (note.title.isNotBlank()) note.title else formatTimestamp(note.lastModified)

                    // Bỏ dấu nội dung và tiêu đề trước khi so sánh
                    val unaccentedFakeTitle = StringUtils.unaccent(fakeTitle)
                    val unaccentedContent = StringUtils.unaccent(note.content)

                    unaccentedFakeTitle.contains(unaccentedQuery, ignoreCase = true) ||
                            unaccentedContent.contains(unaccentedQuery, ignoreCase = true)
                }
            }
        }.asLiveData()

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSortOrder(order: SortOrder) {
        sortOrder.value = order
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun insert(note: Note) = viewModelScope.launch {
        repository.insert(note)
    }

    fun update(note: Note) = viewModelScope.launch {
        repository.update(note)
    }

    fun delete(note: Note) = viewModelScope.launch {
        repository.delete(note)
    }
}

class NoteViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}