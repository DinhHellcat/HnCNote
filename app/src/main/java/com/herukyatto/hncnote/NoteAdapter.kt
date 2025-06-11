package com.herukyatto.hncnote

import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

            // Lấy màu từ resources
            val highlightColor = itemView.context.getColor(R.color.search_highlight_color)

            titleTextView.text = highlightText(fakeTitle, query, highlightColor)
            contentTextView.text = highlightText(note.content, query, highlightColor)
        }

        // Sửa lại hàm highlightText để nhận vào màu
        private fun highlightText(text: String, query: String, color: Int): SpannableString {
            val spannableString = SpannableString(text)
            if (query.isNotBlank()) {
                // Bỏ dấu cả text và query để tìm vị trí
                val unaccentedText = StringUtils.unaccent(text)
                val unaccentedQuery = StringUtils.unaccent(query)

                val regex = unaccentedQuery.toRegex(RegexOption.IGNORE_CASE)
                regex.findAll(unaccentedText).forEach { matchResult ->
                    spannableString.setSpan(
                        BackgroundColorSpan(color),
                        matchResult.range.first,
                        matchResult.range.last + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            return spannableString
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