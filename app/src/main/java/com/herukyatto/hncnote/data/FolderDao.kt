package com.herukyatto.hncnote.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(folder: Folder)

    @Query("SELECT * FROM folders_table ORDER BY name ASC")
    fun getAllFolders(): Flow<List<Folder>>

    @Delete
    suspend fun delete(folder: Folder)
}