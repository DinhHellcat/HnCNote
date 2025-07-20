package com.herukyatto.hncnote.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.herukyatto.hncnote.R
import com.herukyatto.hncnote.data.Note
import com.herukyatto.hncnote.data.SortOrder

class MainActivity : AppCompatActivity() {

    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory((application as NotesApplication).repository)
    }
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.notesRecyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)

        setupDrawer(toolbar)
        setupRecyclerView()
        setupFolderRecyclerView()
        setupObservers()
        setupSearchView()
        setupFab()
    }

    private fun setupDrawer(toolbar: Toolbar) {
        drawerLayout = findViewById(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter(
            onItemClicked = { selectedNote ->
                val intent = Intent(this@MainActivity, NoteEditorActivity::class.java)
                intent.putExtra("EXTRA_NOTE", selectedNote)
                startActivity(intent)
            },
            onItemLongClicked = { selectedNote ->
                showDeleteConfirmationDialog(selectedNote)
            },
            onFavoriteClicked = { noteToFavorite ->
                noteViewModel.toggleFavorite(noteToFavorite)
            }
        )
        recyclerView.adapter = noteAdapter
        recyclerView.layoutManager = GridLayoutManager(this, 2)
    }

    private fun setupFolderRecyclerView() {
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val foldersRecyclerView = navView.findViewById<RecyclerView>(R.id.foldersRecyclerView)
        folderAdapter = FolderAdapter { selectedFolder ->
            noteViewModel.setCurrentFolder(selectedFolder.id)
            drawerLayout.closeDrawers()
        }
        foldersRecyclerView.adapter = folderAdapter
        foldersRecyclerView.layoutManager = LinearLayoutManager(this)
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
                noteAdapter.submitList(it)
            }
        }

        noteViewModel.allFolders.observe(this) { folders ->
            folderAdapter.submitList(folders)
            val currentFolder = folders.find { it.id == noteViewModel.currentFolderIdState.value }
            supportActionBar?.title = currentFolder?.name ?: "Ghi chú"
        }
    }

    private fun setupSearchView() {
        val searchView = findViewById<SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText.orEmpty()
                noteAdapter.searchQuery = query
                noteViewModel.setSearchQuery(query)
                noteAdapter.notifyDataSetChanged()
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
}