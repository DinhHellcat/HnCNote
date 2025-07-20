package com.herukyatto.hncnote.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.herukyatto.hncnote.R
import com.herukyatto.hncnote.data.Folder

class FolderAdapter(
    private val onFolderClicked: (Folder) -> Unit,
    private val onDeleteClicked: (Folder) -> Unit
) : ListAdapter<Folder, FolderAdapter.FolderViewHolder>(FolderComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.folder_item, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current) // Sửa lại để truyền cả Folder
        holder.itemView.setOnClickListener { onFolderClicked(current) }
        holder.deleteIcon.setOnClickListener { onDeleteClicked(current) }
    }

    class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val folderNameTextView: TextView = itemView.findViewById(R.id.folder_name)
        val deleteIcon: ImageView = itemView.findViewById(R.id.delete_folder_icon) // Thêm tham chiếu

        fun bind(folder: Folder) {
            folderNameTextView.text = folder.name
            // Ẩn nút xóa cho thư mục mặc định
            deleteIcon.visibility = if (folder.id == 1) View.GONE else View.VISIBLE
        }
    }

    class FolderComparator : DiffUtil.ItemCallback<Folder>() {
        override fun areItemsTheSame(oldItem: Folder, newItem: Folder): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Folder, newItem: Folder): Boolean = oldItem == newItem
    }
}