package com.example.notesapp_sqlitedatabase.Model

import com.google.firebase.database.PropertyName


data class Note(
    var id: String = "",
    val title: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    @PropertyName("favorite") val isFavorite: Boolean = false,
    @PropertyName("completed") val isCompleted: Boolean = false,
    val imageUrls: List<String> = emptyList()
)
