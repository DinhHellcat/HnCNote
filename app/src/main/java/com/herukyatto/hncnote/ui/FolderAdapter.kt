package com.herukyatto.hncnote.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.herukyatto.hncnote.R
import com.herukyatto.hncnote.data.Folder

class FolderAdapter(
    private val onFolderClicked: (Folder) -> Unit
) : ListAdapter<Folder, FolderAdapter.FolderViewHolder>(FolderComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.folder_item, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current.name)
        holder.itemView.setOnClickListener {
            onFolderClicked(current)
        }
    }

    class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val folderNameTextView: TextView = itemView.findViewById(R.id.folder_name)

        fun bind(name: String) {
            folderNameTextView.text = name
        }
    }

    class FolderComparator : DiffUtil.ItemCallback<Folder>() {
        override fun areItemsTheSame(oldItem: Folder, newItem: Folder): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Folder, newItem: Folder): Boolean = oldItem == newItem
    }
}