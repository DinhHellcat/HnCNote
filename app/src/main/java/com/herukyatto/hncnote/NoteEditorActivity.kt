package com.herukyatto.hncnote

import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import java.util.regex.Pattern

class NoteEditorActivity : AppCompatActivity() {

    private var currentNote: Note? = null
    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText

    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory((application as NotesApplication).repository)
    }

    // Biến cờ để kiểm soát việc render, tránh vòng lặp vô tận
    private var isApplyingSpans = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_editor)

        setupViews()
        setupToolbar()
        setupBackButton()
        // Thiết lập TextWatcher trước khi tải dữ liệu
        setupTextWatcher()
        loadNoteData()
    }

    private fun setupViews() {
        titleEditText = findViewById(R.id.editorTitleEditText)
        contentEditText = findViewById(R.id.editorContentEditText)
        contentEditText.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun loadNoteData() {
        @Suppress("DEPRECATION")
        currentNote = intent.getSerializableExtra("EXTRA_NOTE") as? Note
        currentNote?.let { note ->
            titleEditText.setText(note.title)
            contentEditText.setText(note.content)
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.editorToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { saveNoteAndFinish() }
    }

    private fun setupBackButton() {
        onBackPressedDispatcher.addCallback(this) { saveNoteAndFinish() }
    }

    private fun setupTextWatcher() {
        contentEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // TextWatcher sẽ gọi hàm render chính
                renderChecklists(s)
            }
        })
    }

    private fun renderChecklists(editable: Editable?) {
        if (editable == null || isApplyingSpans) return
        isApplyingSpans = true

        editable.getSpans(0, editable.length, ImageSpan::class.java).forEach { editable.removeSpan(it) }
        editable.getSpans(0, editable.length, ClickableSpan::class.java).forEach { editable.removeSpan(it) }

        val uncheckedDrawable = ContextCompat.getDrawable(this, R.drawable.ic_checkbox_unchecked)!!.apply {
            setBounds(0, 0, contentEditText.lineHeight, contentEditText.lineHeight)
        }
        val checkedDrawable = ContextCompat.getDrawable(this, R.drawable.ic_checkbox_checked)!!.apply {
            setBounds(0, 0, contentEditText.lineHeight, contentEditText.lineHeight)
        }

        applySpansForPattern(editable, "\\[ \\]", uncheckedDrawable)
        applySpansForPattern(editable, "\\[x\\]", checkedDrawable)

        isApplyingSpans = false
    }

    private fun applySpansForPattern(editable: Editable, patternString: String, drawable: android.graphics.drawable.Drawable) {
        val matcher = Pattern.compile(patternString).matcher(editable)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            editable.setSpan(ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val currentText = editable.subSequence(start, end).toString()
                    val newText = if (currentText == "[ ]") "[x]" else "[ ]"
                    editable.replace(start, end, newText)
                }
            }
            editable.setSpan(clickableSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun saveNoteAndFinish() {
        val title = titleEditText.text.toString().trim()
        val content = contentEditText.text.toString().trim()
        if (currentNote != null && title.isBlank() && content.isBlank()) {
            noteViewModel.delete(currentNote!!)
            finish(); return
        }
        if (currentNote == null && title.isBlank() && content.isBlank()) {
            finish(); return
        }
        val currentTime = System.currentTimeMillis()
        if (currentNote != null) {
            val updatedNote = currentNote!!.copy(title = title, content = content, lastModified = currentTime)
            noteViewModel.update(updatedNote)
        } else {
            val newNote = Note(title = title, content = content, lastModified = currentTime)
            noteViewModel.insert(newNote)
        }
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.editor_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_insert_checklist -> {
                // KIỂM TRA XEM FOCUS CÓ ĐANG Ở Ô NỘI DUNG KHÔNG
                if (contentEditText.hasFocus()) {
                    // Nếu đúng, thực hiện chèn checklist như cũ
                    val start = contentEditText.selectionStart
                    val prefix = if (start == 0 || contentEditText.text.getOrNull(start - 1) == '\n') "" else "\n"
                    contentEditText.editableText.insert(start, "$prefix[ ] ")
                } else {
                    // Nếu focus đang ở tiêu đề hoặc nơi khác, không làm gì cả.
                    // Hoặc bạn có thể thêm một Toast để thông báo cho người dùng (tùy chọn):
                    // Toast.makeText(this, "Hãy đặt con trỏ vào phần nội dung", Toast.LENGTH_SHORT).show()
                }
                true
            }
            android.R.id.home -> {
                saveNoteAndFinish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}