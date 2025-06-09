package com.herukyatto.hncnote

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    // Dùng asLiveData để chuyển Flow<List<Note>> từ Repository thành LiveData.
    // UI sẽ quan sát LiveData này.
    val allNotes: LiveData<List<Note>> = repository.allNotes.asLiveData()

    /**
     * Chạy một coroutine mới để chèn dữ liệu một cách không đồng bộ.
     */
    fun insert(note: Note) = viewModelScope.launch {
        repository.insert(note)
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