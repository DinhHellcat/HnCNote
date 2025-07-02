package com.herukyatto.hncnote.ui

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
import com.herukyatto.hncnote.R
import com.herukyatto.hncnote.data.Note
import java.util.regex.Pattern
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.addCallback
import androidx.activity.viewModels

class NoteEditorActivity : AppCompatActivity() {

    private var currentNote: Note? = null
    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText

    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory((application as NotesApplication).repository)
    }

    // Dùng biến cờ để kiểm soát việc render, an toàn hơn nhiều
    private var isApplyingSpans = false

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Tạm thời chỉ hiển thị một Toast để xác nhận đã chọn được ảnh
            // Ở bước sau, chúng ta sẽ chèn ảnh vào EditText tại đây
            Toast.makeText(this, "Đã chọn ảnh: $it", Toast.LENGTH_LONG).show()
        }
    }

    // Launcher để xin quyền đọc ảnh
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            // Nếu người dùng cấp quyền, mở thư viện ảnh
            openImagePicker()
        } else {
            // Nếu người dùng từ chối, hiển thị thông báo
            Toast.makeText(this, "Bạn đã từ chối quyền truy cập thư viện", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_editor)

        setupViews()
        setupToolbar()
        setupBackButton()
        // Gắn TextWatcher ngay từ đầu
        setupTextWatcher()
        // Tải dữ liệu sau cùng
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
            // Chỉ cần setText, TextWatcher sẽ tự động gọi renderChecklists
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
                    // Chỉ thay đổi text, TextWatcher sẽ lo phần còn lại
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
            val updatedNote = currentNote!!.copy(title = title, content = content, lastModified = currentTime, isFavorite = currentNote!!.isFavorite)
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
            // Case cho nút chèn checklist
            R.id.action_insert_checklist -> {
                if (contentEditText.hasFocus()) {
                    val start = contentEditText.selectionStart
                    val prefix = if (start == 0 || contentEditText.text.getOrNull(start - 1) == '\n') "" else "\n"
                    contentEditText.editableText.insert(start, "$prefix[ ] ")
                } else {
                    contentEditText.requestFocus()
                    val start = contentEditText.selectionStart
                    contentEditText.editableText.insert(start, "\n[ ] ")
                }
                true
            }

            // THÊM CASE MỚI CHO NÚT CHÈN ẢNH
            R.id.action_insert_image -> {
                handleInsertImage()
                true
            }

            // Case cho nút quay lại (mũi tên) trên toolbar
            android.R.id.home -> {
                saveNoteAndFinish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleInsertImage() {
        when {
            // Kiểm tra xem đã được cấp quyền chưa
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Nếu đã có quyền, mở thư viện ảnh
                openImagePicker()
            }
            else -> {
                // Nếu chưa có quyền, bắt đầu quá trình xin quyền
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }
    }

    private fun openImagePicker() {
        // Mở công cụ chọn file hệ thống, chỉ hiển thị các loại ảnh
        pickImageLauncher.launch("image/*")
    }
}