package com.herukyatto.hncnote.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.herukyatto.hncnote.R
import com.herukyatto.hncnote.data.Note
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.regex.Pattern
import android.text.util.Linkify

class NoteEditorActivity : AppCompatActivity() {

    private var currentNote: Note? = null
    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText

    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory((application as NotesApplication).repository)
    }

    private var isRenderingSpans = false
    private var textWatcher: TextWatcher? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { imageUri ->
            val localPath = saveImageToInternalStorage(imageUri)
            if (localPath != null) {
                insertImageTagIntoEditText(localPath)
            } else {
                Toast.makeText(this, "Không thể lưu hình ảnh", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) { openImagePicker() }
        else { Toast.makeText(this, "Bạn đã từ chối quyền truy cập thư viện", Toast.LENGTH_SHORT).show() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_editor)

        setupViews()
        setupToolbar()
        setupBackButton()
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
            // Gọi render ngay sau khi gán text để đảm bảo hiển thị đúng
            contentEditText.post {
                renderAllSpans(contentEditText.text)
            }
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
        textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                renderAllSpans(s)
            }
        }
        contentEditText.addTextChangedListener(textWatcher)
    }

    private fun renderAllSpans(editable: Editable?) {
        if (editable == null || isRenderingSpans) return
        isRenderingSpans = true

        editable.getSpans(0, editable.length, ImageSpan::class.java).forEach { editable.removeSpan(it) }
        editable.getSpans(0, editable.length, ClickableSpan::class.java).forEach { editable.removeSpan(it) }

        applyChecklistSpans(editable)
        applyImageSpans(editable)

        Linkify.addLinks(editable, Linkify.WEB_URLS)

        isRenderingSpans = false
    }

    private fun applyChecklistSpans(editable: Editable) {
        val uncheckedDrawable = ContextCompat.getDrawable(this, R.drawable.ic_checkbox_unchecked)!!.apply {
            setBounds(0, 0, contentEditText.lineHeight, contentEditText.lineHeight)
        }
        val checkedDrawable = ContextCompat.getDrawable(this, R.drawable.ic_checkbox_checked)!!.apply {
            setBounds(0, 0, contentEditText.lineHeight, contentEditText.lineHeight)
        }
        applyClickableSpanForPattern(editable, "\\[ \\]", uncheckedDrawable)
        applyClickableSpanForPattern(editable, "\\[x\\]", checkedDrawable)
    }

    private fun applyClickableSpanForPattern(editable: Editable, patternString: String, drawable: Drawable) {
        val matcher = Pattern.compile(patternString).matcher(editable)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            editable.setSpan(ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val currentText = editable.subSequence(start, end).toString()
                    val newText = if (currentText == "[ ]") "[x]" else "[ ]"
                    editable.replace(start, end, newText)
                }
            }
            editable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun applyImageSpans(editable: Editable) {
        val maxWidth = contentEditText.width - contentEditText.paddingLeft - contentEditText.paddingRight
        if (maxWidth <= 0) return

        val matcher = Pattern.compile("\\[IMG:(.*?)\\]").matcher(editable)
        val matches = mutableListOf<Pair<Int, Int>>()
        while (matcher.find()) { matches.add(Pair(matcher.start(), matcher.end())) }

        matches.asReversed().forEach { (start, end) ->
            val tag = editable.subSequence(start, end).toString()
            val path = tag.substring(5, tag.length - 1)
            try {
                val bitmap = decodeSampledBitmapFromFile(path, maxWidth)
                if (bitmap != null) {
                    val imageDrawable = android.graphics.drawable.BitmapDrawable(resources, bitmap)
                    imageDrawable.setBounds(0, 0, bitmap.width, bitmap.height)
                    val imageSpan = ImageSpan(imageDrawable)
                    editable.setSpan(imageSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            } catch (e: Exception) {
                Log.e("NoteEditorActivity", "Error loading image from path: $path", e)
            }
        }
    }

    private fun decodeSampledBitmapFromFile(path: String, reqWidth: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, options)
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1
            val reqHeight = (height.toFloat() / width.toFloat() * reqWidth).toInt()
            if (height > reqHeight || width > reqWidth) {
                val halfHeight: Int = height / 2
                val halfWidth: Int = width / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            options.inSampleSize = inSampleSize
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val fileName = "img_${UUID.randomUUID()}.jpg"
            val file = File(filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun insertImageTagIntoEditText(localPath: String) {
        val editable = contentEditText.editableText
        val start = contentEditText.selectionStart
        val prefix = if (start == 0 || editable.getOrNull(start - 1) == '\n') "" else "\n"
        editable.insert(start, "$prefix[IMG:$localPath]\n")
    }

    private fun saveNoteAndFinish() {
        val title = titleEditText.text.toString().trim()
        val content = contentEditText.text.toString().trim()
        if (currentNote != null && title.isBlank() && content.isBlank()) {
            noteViewModel.moveToTrash(currentNote!!); finish(); return
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
            R.id.action_insert_checklist -> {
                if (contentEditText.hasFocus()) {
                    val start = contentEditText.selectionStart
                    val prefix = if (start == 0 || contentEditText.text.getOrNull(start - 1) == '\n') "" else "\n"
                    contentEditText.editableText.insert(start, "$prefix[ ] ")
                } else {
                    contentEditText.requestFocus()
                    contentEditText.editableText.append("\n[ ] ")
                }
                true
            }
            R.id.action_insert_image -> {
                handleInsertImage()
                true
            }
            android.R.id.home -> {
                saveNoteAndFinish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleInsertImage() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }
    }

    private fun openImagePicker() {
        pickImageLauncher.launch("image/*")
    }


}