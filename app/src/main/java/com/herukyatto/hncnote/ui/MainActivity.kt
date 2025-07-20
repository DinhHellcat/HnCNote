package com.herukyatto.hncnote.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.herukyatto.hncnote.R
import com.herukyatto.hncnote.data.Note
import com.herukyatto.hncnote.data.SortOrder
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory((application as NotesApplication).repository)
    }
    private lateinit var adapter: NoteAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        recyclerView = findViewById(R.id.notesRecyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)

        setupRecyclerView()
        setupObservers()
        setupSearchView()
        setupFab()
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter(
            onItemClicked = { selectedNote ->
                val intent = Intent(this@MainActivity, NoteEditorActivity::class.java)
                intent.putExtra("EXTRA_NOTE", selectedNote)
                startActivity(intent)
            },
            onItemLongClicked = { selectedNote ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("Chuyển vào thùng rác") // Sửa tiêu đề
                    .setMessage("Ghi chú sẽ được chuyển vào thùng rác.") // Sửa thông điệp
                    .setPositiveButton("Chuyển") { _, _ ->
                        // SỬA LẠI DÒNG NÀY
                        noteViewModel.moveToTrash(selectedNote)
                    }
                    .setNegativeButton("Hủy", null).show()
            },
            onFavoriteClicked = { noteToFavorite ->
                noteViewModel.toggleFavorite(noteToFavorite)
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(this, 2)
    }

    private fun setupFab() {
        val fab = findViewById<FloatingActionButton>(R.id.addNoteFab)
        fab.setOnClickListener {
            val intent = Intent(this@MainActivity, NoteEditorActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupSearchView() {
        val searchView = findViewById<SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText.orEmpty()
                // Cập nhật query cho cả ViewModel và Adapter
                noteViewModel.setSearchQuery(query)
                adapter.searchQuery = query
                // Buộc Adapter vẽ lại toàn bộ, đảm bảo highlight và checkbox luôn đúng
                adapter.notifyDataSetChanged()
                return true
            }
        })
    }

    private fun setupObservers() {
        noteViewModel.allNotes.observe(this) { notes ->
            notes?.let {
                if (it.isEmpty() && noteViewModel.searchQueryState.value.isBlank()) {
                    recyclerView.visibility = View.GONE
                    emptyStateLayout.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    emptyStateLayout.visibility = View.GONE
                }
                adapter.submitList(it)
            }
        }
    }

    // --- Các hàm xử lý Menu ---

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
            // THÊM CASE MỚI NÀY
            R.id.action_trash -> {
                val intent = Intent(this, TrashActivity::class.java)
                startActivity(intent)
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

    private fun showDeleteConfirmationDialog(note: Note) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Chuyển vào thùng rác")
            .setMessage("Ghi chú sẽ được chuyển vào thùng rác.")
            .setPositiveButton("Chuyển") { _, _ ->
                noteViewModel.moveToTrash(note)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}