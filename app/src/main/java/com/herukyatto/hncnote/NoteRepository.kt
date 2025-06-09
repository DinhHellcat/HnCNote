package com.herukyatto.hncnote

import kotlinx.coroutines.flow.Flow

/**
 * Repository là một lớp trung gian giúp trừu tượng hóa (che giấu) nguồn dữ liệu.
 * Nó cung cấp một API sạch sẽ cho phần còn lại của ứng dụng để làm việc với dữ liệu.
 * Constructor (hàm khởi tạo) của nó nhận vào một đối tượng NoteDao để có thể
 * truy cập vào các hàm của DAO.
 */
class NoteRepository(private val noteDao: NoteDao) {

    // Lấy ra một Flow chứa danh sách tất cả các Note từ DAO.
    // Đây là nguồn dữ liệu chính cho màn hình danh sách ghi chú.
    // Bất cứ khi nào dữ liệu trong database thay đổi, Flow này sẽ tự động phát ra giá trị mới.
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    /**
     * Các hàm suspend để gọi các hàm tương ứng trong DAO.
     * Việc gọi các hàm này từ một Coroutine sẽ đảm bảo các thao tác database
     * được thực hiện trên một luồng nền, không làm treo giao diện.
     * ViewModel sau này sẽ gọi các hàm này.
     */
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