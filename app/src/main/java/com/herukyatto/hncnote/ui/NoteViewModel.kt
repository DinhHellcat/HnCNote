package com.herukyatto.hncnote.ui

import androidx.lifecycle.*
import com.herukyatto.hncnote.data.Note
import com.herukyatto.hncnote.data.NoteRepository
import com.herukyatto.hncnote.data.SortOrder
import com.herukyatto.hncnote.utils.StringUtils
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


    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSortOrder(order: SortOrder) {
        sortOrder.value = order
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

    fun toggleFavorite(note: Note) = viewModelScope.launch {
        val updatedNote = note.copy(isFavorite = !note.isFavorite)
        repository.update(updatedNote)
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}