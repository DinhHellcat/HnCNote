package com.herukyatto.hncnote

import android.os.Bundle
import android.widget.EditText
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class NoteEditorActivity : AppCompatActivity() {

    private var currentNote: Note? = null
    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText

    // Lấy ViewModel giống như cách đã làm trong MainActivity
    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory((application as NotesApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_editor)

        titleEditText = findViewById(R.id.editorTitleEditText)
        contentEditText = findViewById(R.id.editorContentEditText)

        // Lấy dữ liệu Note được gửi qua
        if (intent.hasExtra("EXTRA_NOTE")) {
            currentNote = intent.getSerializableExtra("EXTRA_NOTE") as? Note
        }

        // Hiển thị dữ liệu lên giao diện
        currentNote?.let { note ->
            titleEditText.setText(note.title)
            contentEditText.setText(note.content)
        }

        // Xử lý nút quay lại trên Toolbar
        val toolbar = findViewById<Toolbar>(R.id.editorToolbar)
        toolbar.setNavigationOnClickListener {
            // Khi nhấn nút quay lại, lưu ghi chú và kết thúc activity
            saveNoteAndFinish()
        }

        // Xử lý nút Back của hệ thống (vuốt hoặc nhấn nút Back)
        onBackPressedDispatcher.addCallback(this) {
            saveNoteAndFinish()
        }
    }

    private fun saveNoteAndFinish() {
        val title = titleEditText.text.toString()
        val content = contentEditText.text.toString()

        // Nếu không có tiêu đề và nội dung, coi như không tạo/sửa, chỉ thoát
        if (title.isBlank() && content.isBlank()) {
            finish()
            return
        }

        if (currentNote != null) {
            // --- TRƯỜNG HỢP CẬP NHẬT GHI CHÚ ĐÃ CÓ ---
            val updatedNote = currentNote!!.copy(title = title, content = content)
            noteViewModel.update(updatedNote)
        } else {
            // --- TRƯỜNG HỢP TẠO GHI CHÚ MỚI ---
            // (Chúng ta sẽ làm chức năng này sau khi click nút +)
            val newNote = Note(title = title, content = content)
            noteViewModel.insert(newNote)
        }

        finish() // Đóng màn hình soạn thảo
    }
}