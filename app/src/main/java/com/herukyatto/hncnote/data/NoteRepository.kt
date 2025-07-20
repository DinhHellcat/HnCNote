package com.herukyatto.hncnote.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao, private val folderDao: FolderDao) {

    fun getNotesSortedByDateDesc(folderId: Int): Flow<List<Note>> = noteDao.getNotesSortedByDateDesc(folderId)
    fun getNotesSortedByDateAsc(folderId: Int): Flow<List<Note>> = noteDao.getNotesSortedByDateAsc(folderId)
    fun getNotesSortedByTitle(folderId: Int): Flow<List<Note>> = noteDao.getNotesSortedByTitle(folderId)

    // Sửa dòng này thành hàm "fun"
    fun getTrashedNotes(): Flow<List<Note>> = noteDao.getTrashedNotes()

    val allFolders: Flow<List<Folder>> = folderDao.getAllFolders()

    suspend fun insertFolder(folder: Folder) {
        folderDao.insert(folder)
    }

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