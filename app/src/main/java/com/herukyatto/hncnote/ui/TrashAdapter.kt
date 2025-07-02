package com.herukyatto.hncnote.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.herukyatto.hncnote.R
import com.herukyatto.hncnote.data.Note

// Adapter này rất giống NoteAdapter nhưng có callback khác
class TrashAdapter(
    private val onItemLongClicked: (Note) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteAdapter.NotesComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteAdapter.NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.note_item, parent, false)
        return NoteAdapter.NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteAdapter.NoteViewHolder, position: Int) {
        val current = getItem(position)
        // Trong thùng rác, nhấn giữ để hiện menu
        holder.itemView.setOnLongClickListener {
            onItemLongClicked(current)
            true
        }
        // Bỏ các listener không cần thiết khác
        holder.itemView.setOnClickListener(null)
        holder.favoriteIcon.setOnClickListener(null)

        // Dùng lại hàm bind của NoteViewHolder để hiển thị
        holder.bind(current, "") // query rỗng vì không có tìm kiếm ở đây
    }
}