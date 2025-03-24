package com.example.notesapp_sqlitedatabase.View

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notesapp_sqlitedatabase.Model.Note
import com.example.notesapp_sqlitedatabase.ViewModel.NoteViewModel
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(noteId: String, viewModel: NoteViewModel, onBack: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isSaved by remember { mutableStateOf(false) } // Trạng thái đã lưu
    var subImageUriList by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var uploadedImageUrls = remember { mutableStateListOf<String>() }

    val coroutineScope = rememberCoroutineScope()


    LaunchedEffect(noteId) {
        if (noteId.isNotBlank()) {
            viewModel.getNoteById(noteId) { note ->
                note?.let {
                    title = it.title
                    content = it.content
                    uploadedImageUrls.clear()
                    uploadedImageUrls.addAll(it.imageUrls) // Thay đổi tại đây
                    Log.d("NoteDetailScreen", "Danh sách ảnh đã upload: $uploadedImageUrls")
                }
            }
        }
    }

    // Chọn danh sách ảnh phụ (nhiều ảnh)
    val subImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        subImageUriList = uris
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId.isBlank()) "Thêm ghi chú" else "Chỉnh sửa ghi chú") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        Log.d("NoteDetailScreen", "Lưu ghi chú")
                        Log.d("NoteDetailScreen", "Tiêu đề: $title - Nội dung: $content - Đã lưu: $isSaved - ID: $noteId")
                        saveNote(viewModel, noteId, title, content, uploadedImageUrls) {
                            isSaved = true
                        }
                        onBack()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Lưu")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Tiêu đề") },
                textStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Nội dung") },
                textStyle = TextStyle(fontSize = 18.sp),
                modifier = Modifier.fillMaxWidth().height(250.dp).padding(bottom = 16.dp)
            )

            // Chọn danh sách ảnh phụ
            Text("Thêm Ảnh ", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Button(onClick = {
                subImagePickerLauncher.launch("image/*")
            }) {
                Text("Select Additional Images")
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(subImageUriList.ifEmpty { uploadedImageUrls }) { image ->

                    Box(modifier = Modifier.size(160.dp)) {
                        // Ảnh hiển thị với hiệu ứng bo góc và bóng
                        Image(
                            painter = rememberAsyncImagePainter(model = image),
                            contentDescription = "Selected Image",
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .shadow(8.dp, RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )


                    }
                }
            }


            // Nút lưu với hiệu ứng tải
            var isUploading by remember { mutableStateOf(false) }

            ElevatedButton(
                onClick = {
                    coroutineScope.launch {
                        isUploading = true

                        // Xóa ảnh cũ khỏi Firebase
                        uploadedImageUrls.forEach { imageUrl ->
                            viewModel.deleteImageFromFirebase(imageUrl) {}
                        }

                        // Tải ảnh mới lên Firebase
                        val newImageUrls = viewModel.uploadImages(subImageUriList)

                        uploadedImageUrls.clear()
                        uploadedImageUrls.addAll(newImageUrls)

                        // Lưu ghi chú
                        saveNote(viewModel, noteId, title, content, uploadedImageUrls) {
                            isSaved = true
                            onBack()
                        }

                        isUploading = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                if (isUploading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Lưu", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }




        }
    }
}
// Lưu và thoát
fun saveAndExit(
    viewModel: NoteViewModel,
    noteId: String,
    title: String,
    content: String,
    imageUrls: List<String>,
    isSaved: Boolean,
    onBack: () -> Unit
) {
    if (!isSaved) {
        saveNote(viewModel, noteId, title, content, imageUrls) {
            onBack()
        }
    } else {
        onBack()
    }
}

// Lưu ghi chú lên Firestore
fun saveNote(
    viewModel: NoteViewModel,
    noteId: String,
    title: String,
    content: String,
    imageUrls: List<String>,
    onSaved: () -> Unit
) {
    if (title.isNotBlank() || content.isNotBlank()) {
        val timestamp = System.currentTimeMillis()

        if (noteId.isBlank()) {
            val newNote = Note(
                id = "",
                title = title,
                content = content,
                timestamp = timestamp,
                imageUrls = imageUrls
            )
            viewModel.insert(newNote)
        } else {
            viewModel.getNoteById(noteId) { oldNote ->
                if (oldNote != null) {
                    val updatedNote = oldNote.copy(
                        title = title,
                        content = content,
                        timestamp = timestamp,
                        imageUrls = imageUrls
                    )
                    viewModel.insert(updatedNote)
                }
            }
        }
        onSaved()
    }
}