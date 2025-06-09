package com.herukyatto.hncnote

import androidx.lifecycle.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    // Dùng `combine` để kết hợp luồng dữ liệu của `allNotes` và `searchQuery`
    val allNotes: LiveData<List<Note>> = repository.allNotes.combine(searchQuery) { notes, query ->
        if (query.isBlank()) {
            notes // Nếu không có từ khóa tìm kiếm, trả về toàn bộ danh sách
        } else {
            // Nếu có từ khóa, lọc danh sách bằng code Kotlin
            notes.filter { note ->
                // Tạo ra "tiêu đề giả" nếu tiêu đề thật bị trống
                val fakeTitle = if (note.title.isNotBlank()) {
                    note.title
                } else {
                    formatTimestamp(note.lastModified) // Dùng cùng hàm format
                }
                // Kiểm tra xem tiêu đề (thật hoặc giả) HOẶC nội dung có chứa từ khóa không
                fakeTitle.contains(query, ignoreCase = true) ||
                        note.content.contains(query, ignoreCase = true)
            }
        }
    }.asLiveData() // Chuyển kết quả cuối cùng thành LiveData

    // Hàm helper để định dạng thời gian, giống hệt hàm trong Adapter
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
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

/**
 * Factory để tạo ra NoteViewModel với tham số là repository.
 */
class NoteViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}