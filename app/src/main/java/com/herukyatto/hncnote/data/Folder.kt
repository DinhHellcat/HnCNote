package com.herukyatto.hncnote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders_table")
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String
)