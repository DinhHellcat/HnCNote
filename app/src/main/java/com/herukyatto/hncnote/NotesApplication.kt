package com.herukyatto.hncnote

import android.app.Application

class NotesApplication : Application() {
    // Dùng lazy để database và repository chỉ được tạo khi cần đến.
    val database by lazy { NoteDatabase.getDatabase(this) }
    val repository by lazy { NoteRepository(database.noteDao()) }
}