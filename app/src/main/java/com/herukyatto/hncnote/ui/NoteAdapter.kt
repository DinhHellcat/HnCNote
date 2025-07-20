package com.herukyatto.hncnote.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.herukyatto.hncnote.R
import com.herukyatto.hncnote.data.Note
import com.herukyatto.hncnote.utils.StringUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class NoteAdapter(
    private val onItemClicked: (Note) -> Unit,
    private val onItemLongClicked: (Note) -> Unit,
    private val onFavoriteClicked: (Note) -> Unit,
    private val onPinClicked: (Note) -> Unit // Thêm callback mới
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(NotesComparator()) {

    var searchQuery: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.note_item, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val current = getItem(position)
        holder.itemView.setOnClickListener { onItemClicked(current) }
        holder.itemView.setOnLongClickListener {
            onItemLongClicked(current)
            true
        }
        holder.favoriteIcon.setOnClickListener {
            onFavoriteClicked(current)
        }
        holder.pinIcon.setOnClickListener { onPinClicked(getItem(position)) } // Thêm listener
        holder.bind(getItem(position), searchQuery)
    }

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.noteTitleTextView)
        private val contentTextView: TextView = itemView.findViewById(R.id.noteContentTextView)
        val pinIcon: ImageView = itemView.findViewById(R.id.pinIcon) // Thêm tham chiếu
        val favoriteIcon: ImageView = itemView.findViewById(R.id.favoriteIcon)

        fun bind(note: Note, query: String) {
            // --- BƯỚC 1: XÁC ĐỊNH MÀU NỀN VÀ MÀU CHỮ/ICON PHÙ HỢP ---
            val backgroundColor = note.color.toColorInt()

            // Tính toán độ sáng của màu nền
            val luminance = ColorUtils.calculateLuminance(backgroundColor)

            // Nếu nền sáng (độ sáng > 0.5), dùng chữ/icon màu đen. Ngược lại, dùng màu trắng.
            val textColor = if (luminance > 0.5) Color.BLACK else Color.WHITE
            val iconColor =
                if (luminance > 0.5) "#8A000000".toColorInt() else "#B3FFFFFF".toColorInt() // Màu icon mờ hơn một chút

            // --- BƯỚC 2: ÁP DỤNG MÀU SẮC ---
            (itemView as com.google.android.material.card.MaterialCardView).setCardBackgroundColor(
                backgroundColor
            )
            titleTextView.setTextColor(textColor)
            contentTextView.setTextColor(textColor)
            pinIcon.setColorFilter(iconColor)

            // --- BƯỚC 3: HIỂN THỊ DỮ LIỆU (LOGIC CŨ) ---
            val fakeTitle =
                if (note.title.isNotBlank()) note.title else formatTimestamp(note.lastModified)
            val renderedContent = renderContent(note.content, itemView.context)

            titleTextView.text = highlightText(fakeTitle, query)
            contentTextView.text = highlightText(renderedContent, query)

            if (note.isFavorite) {
                favoriteIcon.setImageResource(R.drawable.ic_star_filled_24)
                favoriteIcon.setColorFilter(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.favorite_star_color
                    )
                )
            } else {
                favoriteIcon.setImageResource(R.drawable.ic_star_outline_24)
                favoriteIcon.setColorFilter(iconColor) // Dùng màu icon đã tính toán
            }

            if (note.isPinned) {
                pinIcon.setImageResource(R.drawable.ic_pin_filled_24)
            } else {
                pinIcon.setImageResource(R.drawable.ic_pin_outline_24)
            }
        }

        private fun renderContent(text: String, context: Context): SpannableString {
            var spannableString = SpannableString(text)
            spannableString = renderChecklists(spannableString, context)
            spannableString = renderImages(spannableString, context)
            return spannableString
        }

        private fun renderChecklists(
            spannableString: SpannableString,
            context: Context
        ): SpannableString {
            val lineHeight =
                if (contentTextView.lineHeight > 0) contentTextView.lineHeight else (contentTextView.textSize * 1.2).toInt()

            val uncheckedDrawable =
                ContextCompat.getDrawable(context, R.drawable.ic_checkbox_unchecked)!!
                    .apply { setBounds(0, 0, lineHeight, lineHeight) }
            val checkedDrawable =
                ContextCompat.getDrawable(context, R.drawable.ic_checkbox_checked)!!
                    .apply { setBounds(0, 0, lineHeight, lineHeight) }

            var matcher = Pattern.compile("\\[ ]").matcher(spannableString)
            while (matcher.find()) {
                spannableString.setSpan(
                    ImageSpan(uncheckedDrawable, ImageSpan.ALIGN_BOTTOM),
                    matcher.start(),
                    matcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            matcher = Pattern.compile("\\[x]").matcher(spannableString)
            while (matcher.find()) {
                spannableString.setSpan(
                    ImageSpan(checkedDrawable, ImageSpan.ALIGN_BOTTOM),
                    matcher.start(),
                    matcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return spannableString
        }

        private fun renderImages(
            spannableString: SpannableString,
            context: Context
        ): SpannableString {
            val lineHeight =
                if (contentTextView.lineHeight > 0) contentTextView.lineHeight else (contentTextView.textSize * 1.2).toInt()
            val matcher = Pattern.compile("\\[IMG:(.*?)]").matcher(spannableString)
            val matches = mutableListOf<Pair<Int, Int>>()
            while (matcher.find()) {
                matches.add(Pair(matcher.start(), matcher.end()))
            }

            matches.asReversed().forEach { (start, end) ->
                val tag = spannableString.subSequence(start, end).toString()
                val path = tag.substring(5, tag.length - 1)
                try {
                    val bitmap = BitmapFactory.decodeFile(path)
                    if (bitmap != null) {
                        val imageDrawable =
                            bitmap.toDrawable(context.resources)
                        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                        val height = lineHeight * 3 // Hiển thị ảnh thumbnail cao gấp 3 lần dòng chữ
                        val width = (height * aspectRatio).toInt()
                        imageDrawable.setBounds(0, 0, width, height)
                        val imageSpan = ImageSpan(imageDrawable)
                        spannableString.setSpan(
                            imageSpan,
                            start,
                            end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                } catch (e: Exception) {
                    Log.e("NoteAdapter", "Error loading image from path: $path", e)
                }
            }
            return spannableString
        }

        private fun highlightText(text: CharSequence, query: String): CharSequence {
            if (query.isBlank()) return text
            val spannable = SpannableString.valueOf(text)
            val highlightColor = itemView.context.getColor(R.color.search_highlight_color)
            val unaccentedText = StringUtils.unaccent(text.toString())
            val unaccentedQuery = StringUtils.unaccent(query)
            val regex = unaccentedQuery.toRegex(RegexOption.IGNORE_CASE)
            regex.findAll(unaccentedText).forEach { matchResult ->
                try {
                    spannable.setSpan(
                        android.text.style.BackgroundColorSpan(highlightColor),
                        matchResult.range.first,
                        matchResult.range.last + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } catch (e: IndexOutOfBoundsException) {
                    // Bỏ qua lỗi index nếu có
                }
            }
            return spannable
        }

        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    class NotesComparator : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean = oldItem == newItem
    }
}