package com.herukyatto.hncnote

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory((application as NotesApplication).repository)
    }
    private lateinit var adapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        setupRecyclerView()
        setupObservers()
        setupSearchView()
        setupFab()
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.notesRecyclerView)
        adapter = NoteAdapter(
            onItemClicked = { selectedNote ->
                val intent = Intent(this@MainActivity, NoteEditorActivity::class.java)
                intent.putExtra("EXTRA_NOTE", selectedNote)
                startActivity(intent)
            },
            onItemLongClicked = { selectedNote ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc muốn xóa ghi chú?")
                    .setPositiveButton("Xóa") { _, _ ->  noteViewModel.delete(selectedNote) }
                    .setNegativeButton("Hủy", null).show()
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(this, 2)
    }

    private fun setupObservers() {
        // Observer giờ chỉ có một nhiệm vụ duy nhất: nhận danh sách mới và gửi cho adapter.
        noteViewModel.allNotes.observe(this) { notes ->
            notes?.let {
                adapter.submitList(it)
            }
        }
    }

    // TOÀN BỘ LOGIC QUAN TRỌNG SẼ NẰM Ở ĐÂY
    private fun setupSearchView() {
        val searchView = findViewById<SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            @SuppressLint("NotifyDataSetChanged")
            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText.orEmpty()

                // Cập nhật từ khóa cho cả ViewModel và Adapter ngay lập tức
                noteViewModel.setSearchQuery(query)
                adapter.searchQuery = query

                // Lệnh quan trọng: Buộc adapter phải vẽ lại TẤT CẢ các item
                // đang hiển thị với trạng thái searchQuery mới nhất.
                // Lệnh này giải quyết triệt để vấn đề "chậm 1 nhịp".
                adapter.notifyDataSetChanged()

                return true
            }
        })
    }

    private fun setupFab() {
        val fab = findViewById<FloatingActionButton>(R.id.addNoteFab)
        fab.setOnClickListener {
            val intent = Intent(this@MainActivity, NoteEditorActivity::class.java)
            startActivity(intent)
        }
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort -> {
                showSortDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf("Mới nhất", "Cũ nhất", "Theo tiêu đề (A-Z)")
        val currentSortOrder = noteViewModel.sortOrderState.value
        val checkedItem = when (currentSortOrder) {
            SortOrder.BY_DATE_DESC -> 0
            SortOrder.BY_DATE_ASC -> 1
            SortOrder.BY_TITLE_ASC -> 2
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Sắp xếp theo")
            .setSingleChoiceItems(sortOptions, checkedItem) { dialog, which ->
                val selectedOrder = when (which) {
                    0 -> SortOrder.BY_DATE_DESC
                    1 -> SortOrder.BY_DATE_ASC
                    else -> SortOrder.BY_TITLE_ASC
                }
                noteViewModel.setSortOrder(selectedOrder)
                dialog.dismiss()
            }
            .show()
    }
}