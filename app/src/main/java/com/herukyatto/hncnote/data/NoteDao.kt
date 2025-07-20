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

    // --- CÁC HÀNH ĐỘNG CƠ BẢN ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(note: Note)

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note) // Dùng để xóa vĩnh viễn

    // --- CÁC TRUY VẤN LẤY DỮ LIỆU ---

    // Lấy các ghi chú đã ghim (không phân biệt thư mục)
    @Query("SELECT * FROM notes_table WHERE is_pinned = 1 AND is_in_trash = 0 ORDER BY last_modified DESC")
    fun getPinnedNotes(): Flow<List<Note>>

    // Lấy các ghi chú chưa ghim trong một thư mục cụ thể, sắp xếp theo ngày mới nhất

    @Query("SELECT * FROM notes_table WHERE is_in_trash = 0 AND folder_id = :folderId ORDER BY is_pinned DESC, is_favorite DESC, last_modified DESC")
    fun getNotesSortedByDateDesc(folderId: Int): Flow<List<Note>>

    @Query("SELECT * FROM notes_table WHERE is_in_trash = 0 AND folder_id = :folderId ORDER BY is_pinned DESC, is_favorite DESC, last_modified ASC")
    fun getNotesSortedByDateAsc(folderId: Int): Flow<List<Note>>

    @Query("SELECT * FROM notes_table WHERE is_in_trash = 0 AND folder_id = :folderId ORDER BY is_pinned DESC, is_favorite DESC, note_title COLLATE NOCASE ASC")
    fun getNotesSortedByTitle(folderId: Int): Flow<List<Note>>

    // Lấy các ghi chú trong thùng rác
    @Query("SELECT * FROM notes_table WHERE is_in_trash = 1 ORDER BY last_modified DESC")
    fun getTrashedNotes(): Flow<List<Note>>

    @Query("DELETE FROM notes_table WHERE folder_id = :folderId")
    suspend fun deleteNotesInFolder(folderId: Int)
}