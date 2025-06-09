package com.herukyatto.hncnote

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.note_item, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val current = getItem(position)

        // THÊM LẠI BỘ LẮNG NGHE SỰ KIỆN CLICK NGẮN
        holder.itemView.setOnClickListener {
            onItemClicked(current)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClicked(current)
            true
        }
        holder.bind(current)
    }

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.noteTitleTextView)
        private val contentTextView: TextView = itemView.findViewById(R.id.noteContentTextView)
        // Đã xóa tham chiếu đến noteDateTextView

        fun bind(note: Note) {
            // XỬ LÝ VẤN ĐỀ 1 & 3
            if (note.title.isNotBlank()) {
                titleTextView.text = note.title
                // Đảm bảo text style được reset về mặc định (in đậm)
                titleTextView.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                // Nếu tiêu đề trống, hiển thị ngày tháng thay thế
                titleTextView.text = formatTimestamp(note.lastModified)
                // Vẫn giữ in đậm để giống tiêu đề
                titleTextView.setTypeface(null, android.graphics.Typeface.BOLD)
            }
            contentTextView.text = note.content
            // Không còn cần set text cho dateTextView nữa
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