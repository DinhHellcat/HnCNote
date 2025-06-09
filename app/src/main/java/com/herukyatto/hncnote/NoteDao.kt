package com.herukyatto.hncnote

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao // 1. Đánh dấu đây là một Data Access Object
interface NoteDao {

    // 2. Annotation để chèn một đối tượng Note vào bảng
    // onConflict: Chiến lược xử lý khi có xung đột. IGNORE nghĩa là nếu có một
    // ghi chú với cùng `id` đã tồn tại, thì sẽ bỏ qua hành động chèn này.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(note: Note) // suspend: Hàm này sẽ chạy trên luồng nền

    // 3. Annotation để cập nhật một ghi chú đã có
    @Update
    suspend fun update(note: Note)

    // 4. Annotation để xóa một ghi chú
    @Delete
    suspend fun delete(note: Note)

    // 5. Annotation để truy vấn dữ liệu tùy chỉnh bằng câu lệnh SQL
    // Lấy tất cả các cột từ bảng 'notes_table', sắp xếp theo id giảm dần
    // (để các ghi chú mới nhất luôn nằm ở trên cùng)
    @Query("SELECT * from notes_table ORDER BY id DESC")
    fun getAllNotes(): Flow<List<Note>>
}