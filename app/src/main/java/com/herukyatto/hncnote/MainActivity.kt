package com.herukyatto.hncnote

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    // Dùng activity-ktx để lấy ViewModel một cách gọn gàng
    // và truyền vào factory của chúng ta.
    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory((application as NotesApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // EdgeToEdge không còn cần thiết nữa vì ta đã xử lý nó
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.notesRecyclerView)
        val adapter = NoteAdapter() // Tạo adapter rỗng
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // Quan sát (observe) LiveData từ ViewModel
        // Bất cứ khi nào danh sách ghi chú trong database thay đổi,
        // đoạn code này sẽ được chạy.
        noteViewModel.allNotes.observe(this) { notes ->
            // Khi có dữ liệu mới, gửi nó cho adapter để cập nhật UI.
            notes?.let { adapter.submitList(it) }
        }

        val fab = findViewById<FloatingActionButton>(R.id.addNoteFab)
        fab.setOnClickListener {
            // Tạm thời thêm một note mới để kiểm tra
            val newNote = Note(title = "Ghi chú mới", content = "Nội dung được thêm từ nút FAB.")
            noteViewModel.insert(newNote)
        }
    }
}