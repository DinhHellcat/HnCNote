package com.herukyatto.hncnote

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(note: Note)

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("SELECT * FROM notes_table ORDER BY last_modified DESC")
    fun getNotesSortedByDateDesc(): Flow<List<Note>>

    @Query("SELECT * FROM notes_table ORDER BY last_modified ASC")
    fun getNotesSortedByDateAsc(): Flow<List<Note>>

    // COLLATE NOCASE để sắp xếp không phân biệt chữ hoa/thường
    @Query("SELECT * FROM notes_table ORDER BY note_title COLLATE NOCASE ASC")
    fun getNotesSortedByTitle(): Flow<List<Note>>
}