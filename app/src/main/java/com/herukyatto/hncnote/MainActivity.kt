package com.herukyatto.hncnote

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.content.Intent

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
        val adapter = NoteAdapter(
            // Xử lý click ngắn (để sửa)
            onItemClicked = { selectedNote ->
                val intent = Intent(this@MainActivity, NoteEditorActivity::class.java)
                intent.putExtra("EXTRA_NOTE", selectedNote)
                startActivity(intent)
            },
            // Xử lý click giữ (để xóa)
            onItemLongClicked = { selectedNote ->
                // Tạo và hiển thị Dialog xác nhận
                com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc muốn xóa ghi chú '${selectedNote.title}'?")
                    .setPositiveButton("Xóa") { _, _ ->
                        // Nếu người dùng nhấn "Xóa", gọi ViewModel để xóa
                        noteViewModel.delete(selectedNote)
                    }
                    .setNegativeButton("Hủy", null) // Nhấn "Hủy" thì không làm gì cả
                    .show()
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // Quan sát (observe) LiveData từ ViewModel
        // Bất cứ khi nào danh sách ghi chú trong database thay đổi,
        // đoạn code này sẽ được chạy.
        noteViewModel.allNotes.observe(this) { notes ->
            notes?.let {
                adapter.submitList(it)
                // Lấy từ khóa trực tiếp từ searchView
                val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)
                adapter.searchQuery = searchView.query.toString()
            }
        }

        val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText.orEmpty()
                noteViewModel.setSearchQuery(query)
                // Cập nhật từ khóa cho adapter và yêu cầu vẽ lại
                adapter.searchQuery = query
                adapter.notifyDataSetChanged() // Yêu cầu vẽ lại để highlight
                return true
            }
        })

        val fab = findViewById<FloatingActionButton>(R.id.addNoteFab)
        fab.setOnClickListener {
            // Tạo một Intent để mở NoteEditorActivity
            val intent = Intent(this@MainActivity, NoteEditorActivity::class.java)

            // Quan trọng: Lần này chúng ta KHÔNG đính kèm "EXTRA_NOTE".
            // Điều này báo cho NoteEditorActivity biết đây là trường hợp "tạo mới".
            startActivity(intent)
        }
    }
}