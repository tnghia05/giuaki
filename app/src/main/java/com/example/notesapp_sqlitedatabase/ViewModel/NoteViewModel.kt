package com.example.notesapp_sqlitedatabase.ViewModel

import android.net.Uri
import androidx.lifecycle.*
import com.example.notesapp_sqlitedatabase.Model.Note
import com.example.notesapp_sqlitedatabase.Repository.NoteRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _filteredNotes = MutableLiveData<List<Note>>()
    val filteredNotes: LiveData<List<Note>> = _filteredNotes

    private val _filters = MutableLiveData<Map<String, Boolean>>(emptyMap())
    val filters: LiveData<Map<String, Boolean>> = _filters

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    init {
        fetchAllNotes()
    }
    suspend fun uploadImages(imageUris: List<Uri>): List<String> {
        val uploadedUrls = mutableListOf<String>()
        for (uri in imageUris) {
            val storageRef = FirebaseStorage.getInstance().reference
                .child("images/${System.currentTimeMillis()}_${uri.lastPathSegment}")

            val uploadTask = storageRef.putFile(uri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            uploadedUrls.add(downloadUrl)
        }
        return uploadedUrls
    }


    // Lấy toàn bộ ghi chú từ Firestore
    private fun fetchAllNotes() = viewModelScope.launch {
        try {
            val snapshot = db.collection("notes").get().await()
            val notes = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Note::class.java)?.copy(id = doc.id)
            }
            _filteredNotes.value = applyFilters(notes, _filters.value ?: emptyMap(), _searchQuery.value ?: "")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Áp dụng bộ lọc và tìm kiếm
    private fun applyFilters(notes: List<Note>, filters: Map<String, Boolean>, query: String): List<Note> {
        var filteredList = notes

        // Lọc theo từ khóa tìm kiếm
        if (query.isNotBlank()) {
            filteredList = filteredList.filter { it.title.contains(query, ignoreCase = true) }
        }

        // Nếu chọn "ALL" thì trả về toàn bộ
        if (filters["ALL"] == true) return filteredList

        // Áp dụng bộ lọc
        if (filters["FAVORITE"] == true) {
            filteredList = filteredList.filter { it.isFavorite }
        }

        if (filters["COMPLETED"] == true) {
            filteredList = filteredList.filter { it.isCompleted }
        }

        // Sắp xếp theo tiêu chí
        return filteredList.sortedWith(
            compareByDescending<Note> { filters["COMPLETED"] == true && it.isCompleted }
                .thenByDescending { filters["FAVORITE"] == true && it.isFavorite }
                .thenBy { if (filters["TITLE_A-Z"] == true) it.title.lowercase() else "" }
                .thenByDescending { if (filters["TIME"] == true) it.timestamp else 0L }
        )
    }

    // Cập nhật bộ lọc
    fun toggleFilter(filter: String) {
        val currentFilters = _filters.value?.toMutableMap() ?: mutableMapOf()

        if (filter == "ALL") {
            currentFilters.clear()
            currentFilters["ALL"] = true
        } else {
            currentFilters.remove("ALL")

            if (filter == "TITLE_A-Z") currentFilters.remove("TIME")
            else if (filter == "TIME") currentFilters.remove("TITLE_A-Z")

            if (currentFilters.containsKey(filter)) {
                currentFilters.remove(filter)
            } else {
                currentFilters[filter] = true
            }
        }

        _filters.value = currentFilters
        fetchAllNotes()
    }

    // Cập nhật từ khóa tìm kiếm
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        fetchAllNotes()
    }

    // Đánh dấu ghi chú hoàn thành
    fun toggleComplete(note: Note) = viewModelScope.launch {
        val updatedNote = note.copy(isCompleted = !note.isCompleted)
        upsertNote(updatedNote)
    }

    // Đánh dấu ghi chú yêu thích
    fun toggleFavorite(note: Note) = viewModelScope.launch {
        val updatedNote = note.copy(isFavorite = !note.isFavorite)
        upsertNote(updatedNote)
    }

    // Thêm ghi chú mới
    fun insert(note: Note) = viewModelScope.launch {
        upsertNote(note)
    }

    // Xóa ghi chú
    fun delete(noteId: String) = viewModelScope.launch {
        try {
            db.collection("notes").document(noteId).delete().await()
            fetchAllNotes()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteImageFromFirebase(imageUrl: String, onComplete: (Boolean) -> Unit) {
        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
        storageReference.delete()
            .addOnSuccessListener {
                onComplete(true) // Xóa thành công
            }
            .addOnFailureListener {
                onComplete(false) // Xóa thất bại
            }
    }

    // Thêm hoặc cập nhật ghi chú
    private suspend fun upsertNote(note: Note) {
        try {
            val noteRef = if (note.id != "") {
                db.collection("notes").document(note.id)
            } else {
                db.collection("notes").document()
            }

            val newNote = note.copy(id = noteRef.id)
            noteRef.set(newNote).await()
            fetchAllNotes()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Lấy ghi chú theo ID
    fun getNoteById(id: String, onResult: (Note?) -> Unit) = viewModelScope.launch {
        try {
            val snapshot = db.collection("notes").document(id).get().await()
            onResult(snapshot.toObject(Note::class.java)?.copy(id = snapshot.id))
        } catch (e: Exception) {
            e.printStackTrace()
            onResult(null)
        }
    }

}
