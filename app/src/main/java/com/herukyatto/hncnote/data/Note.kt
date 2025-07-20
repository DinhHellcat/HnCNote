package com.herukyatto.hncnote.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes_table")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "note_title")
    val title: String,

    @ColumnInfo(name = "note_content")
    val content: String,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "is_in_trash", defaultValue = "0")
    val isInTrash: Boolean = false,

    @ColumnInfo(name = "folder_id", defaultValue = "1")
    val folderId: Int = 1,

    @ColumnInfo(name = "color", defaultValue = "#FEFDF7")
val color: String = "#FEFDF7"
) : java.io.Serializable