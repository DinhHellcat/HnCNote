package com.herukyatto.hncnote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Note::class, Folder::class], version = 9, exportSchema = false) // Thêm Folder::class và tăng version
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}