package com.example.notesapp_sqlitedatabase.Repository

import com.example.notesapp_sqlitedatabase.Model.Note
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class NoteRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val notesCollection = firestore.collection("notes")

    // Lấy tất cả ghi chú từ Firestore
    suspend fun getAllNotes(): List<Note> {
        return try {
            val snapshot = notesCollection.get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Note::class.java)?.apply {
                    id = doc.id // Gán id cho Note từ document id
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Lấy ghi chú theo ID từ Firestore
    suspend fun getNoteById(id: String): Note? {
        return try {
            val snapshot = notesCollection.document(id).get().await()
            snapshot.toObject(Note::class.java)?.apply {
                this.id = id
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Thêm hoặc cập nhật ghi chú vào Firestore
    suspend fun upsertNote(note: Note) {
        try {
            notesCollection.document(note.id).set(note).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Xóa ghi chú khỏi Firestore
    suspend fun deleteNote(id: String) {
        try {
            notesCollection.document(id).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
