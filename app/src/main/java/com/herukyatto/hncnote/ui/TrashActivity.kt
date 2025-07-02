package com.herukyatto.hncnote.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.herukyatto.hncnote.R
import com.herukyatto.hncnote.data.Note

class TrashActivity : AppCompatActivity() {

    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory((application as NotesApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        setupToolbar()
        setupRecyclerView()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.trashToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.trashRecyclerView)
        val adapter = TrashAdapter { selectedNote ->
            // Khi nhấn giữ một item, hiển thị dialog lựa chọn
            showOptionsDialog(selectedNote)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // Lắng nghe danh sách các ghi chú trong thùng rác và cập nhật giao diện
        noteViewModel.trashedNotes.observe(this) { trashedNotes ->
            adapter.submitList(trashedNotes)
        }
    }

    private fun showOptionsDialog(note: Note) {
        val options = arrayOf("Khôi phục", "Xóa vĩnh viễn")
        MaterialAlertDialogBuilder(this)
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> noteViewModel.restoreFromTrash(note)
                    1 -> noteViewModel.deletePermanently(note)
                }
                dialog.dismiss()
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}