package com.herukyatto.hncnote.ui

import androidx.lifecycle.*
import com.herukyatto.hncnote.data.*
import com.herukyatto.hncnote.utils.StringUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _sortOrder = MutableStateFlow(SortOrder.BY_DATE_DESC)
    val sortOrderState: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQueryState: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentFolderId = MutableStateFlow(1) // Mặc định là thư mục 1
    val currentFolderIdState: StateFlow<Int> = _currentFolderId.asStateFlow()

    val allFolders: LiveData<List<Folder>> = repository.allFolders.asLiveData()

    // Sửa lại khối logic này
    val allNotes: LiveData<List<Note>> = combine(
        _sortOrder, _currentFolderId
    ) { sortOrder, folderId ->
        Pair(sortOrder, folderId)
    }.flatMapLatest { (sortOrder, folderId) ->
        when (sortOrder) {
            SortOrder.BY_DATE_DESC -> repository.getNotesSortedByDateDesc(folderId)
            SortOrder.BY_DATE_ASC -> repository.getNotesSortedByDateAsc(folderId)
            SortOrder.BY_TITLE_ASC -> repository.getNotesSortedByTitle(folderId)
        }
    }.combine(_searchQuery) { notes, query ->
        if (query.isBlank()) {
            notes
        } else {
            val unaccentedQuery = StringUtils.unaccent(query)
            notes.filter { note ->
                val fakeTitle = if (note.title.isNotBlank()) note.title else formatTimestamp(note.lastModified)
                val unaccentedFakeTitle = StringUtils.unaccent(fakeTitle)
                val unaccentedContent = StringUtils.unaccent(note.content)
                unaccentedFakeTitle.contains(unaccentedQuery, ignoreCase = true) ||
                        unaccentedContent.contains(unaccentedQuery, ignoreCase = true)
            }
        }
    }.asLiveData()

    val trashedNotes: LiveData<List<Note>> = repository.getTrashedNotes().asLiveData()

    fun setSortOrder(order: SortOrder) { _sortOrder.value = order }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setCurrentFolder(folderId: Int) { _currentFolderId.value = folderId }

    fun insert(note: Note) = viewModelScope.launch { repository.insert(note.copy(folderId = _currentFolderId.value)) }
    fun update(note: Note) = viewModelScope.launch { repository.update(note) }
    fun moveToTrash(note: Note) = viewModelScope.launch { repository.update(note.copy(isInTrash = true)) }
    fun toggleFavorite(note: Note) = viewModelScope.launch { repository.update(note.copy(isFavorite = !note.isFavorite)) }
    fun restoreFromTrash(note: Note) = viewModelScope.launch {
        val restoredNote = note.copy(isInTrash = false)
        repository.update(restoredNote)
    }

    fun deletePermanently(note: Note) = viewModelScope.launch {
        repository.delete(note)
    }

    fun insertFolder(folderName: String) = viewModelScope.launch {
        if (folderName.isNotBlank()) {
            repository.insertFolder(Folder(name = folderName))
        }
    }

    init {
        viewModelScope.launch {
            repository.insertFolder(Folder(id = 1, name = "Ghi chú của tôi"))
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun deleteFolder(folder: Folder) = viewModelScope.launch {
        // Không cho phép xóa thư mục mặc định
        if (folder.id == 1) return@launch
        repository.deleteFolderAndNotes(folder)
    }

    fun togglePin(note: Note) = viewModelScope.launch {
        val updatedNote = note.copy(isPinned = !note.isPinned)
        repository.update(updatedNote)
    }
}