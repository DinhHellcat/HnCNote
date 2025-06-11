package com.herukyatto.hncnote

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class NoteAdapter(
    private val onItemClicked: (Note) -> Unit,
    private val onItemLongClicked: (Note) -> Unit
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
        holder.bind(current, searchQuery)
    }

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.noteTitleTextView)
        private val contentTextView: TextView = itemView.findViewById(R.id.noteContentTextView)

        fun bind(note: Note, query: String) {
            val fakeTitle = if (note.title.isNotBlank()) note.title else formatTimestamp(note.lastModified)
            titleTextView.text = highlightText(fakeTitle, query)
            val contentWithChecklists = renderChecklistSpans(note.content, itemView.context)
            contentTextView.text = highlightText(contentWithChecklists, query)
        }

        private fun renderChecklistSpans(text: String, context: Context): SpannableString {
            val spannableString = SpannableString(text)
            val lineHeight = if (contentTextView.lineHeight > 0) contentTextView.lineHeight else (contentTextView.textSize * 1.2).toInt()

            val uncheckedDrawable = ContextCompat.getDrawable(context, R.drawable.ic_checkbox_unchecked)!!.apply {
                setBounds(0, 0, lineHeight, lineHeight)
            }
            val checkedDrawable = ContextCompat.getDrawable(context, R.drawable.ic_checkbox_checked)!!.apply {
                setBounds(0, 0, lineHeight, lineHeight)
            }

            // Sửa lại Pattern, không cần khoảng trắng ở cuối
            var matcher = Pattern.compile("\\[ \\]").matcher(spannableString)
            while (matcher.find()) {
                spannableString.setSpan(ImageSpan(uncheckedDrawable, ImageSpan.ALIGN_BOTTOM), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            matcher = Pattern.compile("\\[x\\]").matcher(spannableString)
            while (matcher.find()) {
                spannableString.setSpan(ImageSpan(checkedDrawable, ImageSpan.ALIGN_BOTTOM), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
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
                spannable.setSpan(
                    android.text.style.BackgroundColorSpan(highlightColor),
                    matchResult.range.first,
                    matchResult.range.last + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return spannable
        }

        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    class NotesComparator : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}