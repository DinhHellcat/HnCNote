package com.herukyatto.hncnote

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * @Database: Đánh dấu đây là lớp cơ sở dữ liệu của Room.
 * entities: Liệt kê tất cả các lớp Entity mà database này quản lý.
 * Hiện tại chúng ta chỉ có [Note::class]. Nếu có thêm, bạn sẽ thêm vào đây.
 * version: Phiên bản của database. Khi bạn thay đổi cấu trúc bảng (thêm/sửa/xóa cột trong Note),
 * bạn phải tăng version này lên (từ 1 thành 2, 3...) và cung cấp một chiến lược di chuyển dữ liệu (Migration).
 * exportSchema = false: Tắt việc xuất cấu trúc database ra file JSON, không cần thiết cho dự án này.
 */
@Database(entities = [Note::class], version = 1, exportSchema = false)
abstract class NoteDatabase : RoomDatabase() {

    // Cung cấp một hàm trừu tượng để các thành phần khác có thể lấy ra DAO.
    // Room sẽ tự động sinh code cho hàm này.
    abstract fun noteDao(): NoteDao

    // companion object hoạt động tương tự như các thành phần static trong Java.
    // Chúng ta dùng nó để tạo ra một thể hiện duy nhất của NoteDatabase (Singleton Pattern).
    companion object {
        /**
         * @Volatile: Đảm bảo rằng giá trị của biến INSTANCE luôn là mới nhất và
         * được đồng bộ trên tất cả các luồng (threads) của ứng dụng.
         */
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getDatabase(context: Context): NoteDatabase {
            // Trả về INSTANCE nếu nó đã tồn tại.
            // Nếu chưa, thì tạo database trong một khối synchronized để đảm bảo an toàn luồng.
            return INSTANCE ?: synchronized(this) {
                // Room.databaseBuilder() là hàm để tạo ra database.
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database" // Đây sẽ là tên file database được tạo trên thiết bị.
                ).build()
                INSTANCE = instance
                // Trả về instance vừa tạo
                instance
            }
        }
    }
}