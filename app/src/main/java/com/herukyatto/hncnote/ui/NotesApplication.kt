package com.herukyatto.hncnote.ui

import android.app.Application
import com.herukyatto.hncnote.data.NoteDatabase
import com.herukyatto.hncnote.data.NoteRepository

class NotesApplication : Application() {
    val database by lazy { NoteDatabase.getDatabase(this) }
    val repository by lazy { NoteRepository(database.noteDao()) }
}