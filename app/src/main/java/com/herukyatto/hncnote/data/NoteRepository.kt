package com.herukyatto.hncnote.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    fun getNotesSortedByDateDesc(): Flow<List<Note>> = noteDao.getNotesSortedByDateDesc()
    fun getNotesSortedByDateAsc(): Flow<List<Note>> = noteDao.getNotesSortedByDateAsc()
    fun getNotesSortedByTitle(): Flow<List<Note>> = noteDao.getNotesSortedByTitle()
    fun getTrashedNotes(): Flow<List<Note>> = noteDao.getTrashedNotes()

    suspend fun insert(note: Note) {
        noteDao.insert(note)
    }

    suspend fun update(note: Note) {
        noteDao.update(note)
    }

    suspend fun delete(note: Note) {
        noteDao.delete(note)
    }
}