package com.herukyatto.hncnote.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
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
import android.widget.ImageView
import android.widget.LinearLayout
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

class NoteEditorActivity : AppCompatActivity() {

    private var currentNote: Note? = null
    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var editorRootLayout: View
    private lateinit var colorPickerContainer: LinearLayout
    private var selectedColor: String = "#FEFDF7"
    private val colorSwatches = mutableListOf<ImageView>()

    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory((application as NotesApplication).repository)
    }

    private var textWatcher: TextWatcher? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { imageUri ->
                val localPath = saveImageToInternalStorage(imageUri)
                if (localPath != null) {
                    insertImageTagIntoEditText(localPath)
                } else {
                    Toast.makeText(this, "Không thể lưu hình ảnh", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openImagePicker()
            } else {
                Toast.makeText(this, "Bạn đã từ chối quyền truy cập thư viện", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_editor)

        setupViews()
        setupToolbar()
        setupBackButton()
        setupTextWatcher()
        loadNoteData()
        setupColorPicker()
    }

    private fun setupViews() {
        titleEditText = findViewById(R.id.editorTitleEditText)
        contentEditText = findViewById(R.id.editorContentEditText)
        contentEditText.movementMethod = LinkMovementMethod.getInstance()
        editorRootLayout = findViewById(R.id.editor_root_layout)
        colorPickerContainer = findViewById(R.id.color_picker_container)
    }

    private fun loadNoteData() {
        @Suppress("DEPRECATION")
        currentNote = intent.getSerializableExtra("EXTRA_NOTE") as? Note
        currentNote?.let { note ->
            titleEditText.setText(note.title)
            contentEditText.setText(note.content)
            selectedColor = note.color
            updateBackgroundColor()
            contentEditText.post { renderAllSpans(contentEditText.text) }
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
        if (editable == null) return

        contentEditText.removeTextChangedListener(textWatcher)

        editable.getSpans(0, editable.length, ImageSpan::class.java)
            .forEach { editable.removeSpan(it) }
        editable.getSpans(0, editable.length, ClickableSpan::class.java)
            .forEach { editable.removeSpan(it) }

        applyChecklistSpans(editable)
        applyImageSpans(editable)

        contentEditText.addTextChangedListener(textWatcher)
    }

    private fun applyChecklistSpans(editable: Editable) {
        val uncheckedDrawable =
            ContextCompat.getDrawable(this, R.drawable.ic_checkbox_unchecked)!!.apply {
                setBounds(0, 0, contentEditText.lineHeight, contentEditText.lineHeight)
            }
        val checkedDrawable =
            ContextCompat.getDrawable(this, R.drawable.ic_checkbox_checked)!!.apply {
                setBounds(0, 0, contentEditText.lineHeight, contentEditText.lineHeight)
            }
        applyClickableSpanForPattern(editable, "\\[ \\]", uncheckedDrawable)
        applyClickableSpanForPattern(editable, "\\[x\\]", checkedDrawable)
    }

    private fun applyClickableSpanForPattern(
        editable: Editable,
        patternString: String,
        drawable: Drawable
    ) {
        val matcher = Pattern.compile(patternString).matcher(editable)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            editable.setSpan(
                ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
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
        val maxWidth =
            ((contentEditText.width - contentEditText.paddingLeft - contentEditText.paddingRight) * 0.75).toInt()
        if (maxWidth <= 0) return

        val matcher = Pattern.compile("\\[IMG:(.*?)\\]").matcher(editable)
        val matches = mutableListOf<Pair<Int, Int>>()
        while (matcher.find()) {
            matches.add(Pair(matcher.start(), matcher.end()))
        }

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
            noteViewModel.moveToTrash(currentNote!!)
            finish(); return
        }
        if (currentNote == null && title.isBlank() && content.isBlank()) {
            finish(); return
        }
        val currentTime = System.currentTimeMillis()
        if (currentNote != null) {
            val updatedNote = currentNote!!.copy(
                title = title,
                content = content,
                lastModified = currentTime,
                isFavorite = currentNote!!.isFavorite,
                color = selectedColor
            )
            noteViewModel.update(updatedNote)
        } else {
            val newNote = Note(
                title = title,
                content = content,
                lastModified = currentTime,
                color = selectedColor
            )
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
                    val prefix =
                        if (start == 0 || contentEditText.text.getOrNull(start - 1) == '\n') "" else "\n"
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

    @SuppressLint("InlinedApi")
    private fun handleInsertImage() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }
    }

    // THÊM LẠI HÀM CÒN THIẾU
    private fun openImagePicker() {
        pickImageLauncher.launch("image/*")
    }

    private fun setupColorPicker() {
        val colors = listOf("#FEFDF7", "#FDE7E7", "#E7EEF5", "#E7F5E8", "#FFF5D1", "#EAEAEA")
        for (i in colors.indices) {
            val colorString = colors[i]
            val swatch = ImageView(this)
            val params = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40))
            params.setMargins(dpToPx(8), 0, dpToPx(8), 0)
            swatch.layoutParams = params
            swatch.tag = colorString
            swatch.setOnClickListener {
                selectedColor = it.tag as String
                updateBackgroundColor()
                updateSwatchSelection()
            }
            colorSwatches.add(swatch)
            colorPickerContainer.addView(swatch)
        }
        updateSwatchSelection()
    }

    private fun updateBackgroundColor() {
        editorRootLayout.setBackgroundColor(Color.parseColor(selectedColor))
    }

    private fun updateSwatchSelection() {
        colorSwatches.forEach { swatch ->
            val colorString = swatch.tag as String
            val colorResId = resources.getIdentifier(
                "note_color_${getColorName(colorString)}",
                "color",
                packageName
            )

            if (colorString == selectedColor) {
                swatch.setBackgroundResource(R.drawable.color_swatch_selected)
            } else {
                swatch.setBackgroundResource(R.drawable.color_swatch)
            }
            val drawable = swatch.background.mutate()
            @Suppress("DEPRECATION")
            drawable.setColorFilter(
                ContextCompat.getColor(this, colorResId),
                PorterDuff.Mode.SRC_IN
            )
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun getColorName(hex: String): String {
        return when (hex) {
            "#FDE7E7" -> "red"
            "#E7EEF5" -> "blue"
            "#E7F5E8" -> "green"
            "#FFF5D1" -> "yellow"
            "#EAEAEA" -> "gray"
            else -> "default"
        }
    }
}