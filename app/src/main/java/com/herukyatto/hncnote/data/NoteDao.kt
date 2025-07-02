package com.herukyatto.hncnote.data

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

    @Query("SELECT * FROM notes_table WHERE is_in_trash = 0 ORDER BY is_favorite DESC, last_modified DESC")
    fun getNotesSortedByDateDesc(): Flow<List<Note>>

    @Query("SELECT * FROM notes_table WHERE is_in_trash = 0 ORDER BY is_favorite DESC, last_modified ASC")
    fun getNotesSortedByDateAsc(): Flow<List<Note>>

    @Query("SELECT * FROM notes_table WHERE is_in_trash = 0 ORDER BY is_favorite DESC, note_title COLLATE NOCASE ASC")
    fun getNotesSortedByTitle(): Flow<List<Note>>
}