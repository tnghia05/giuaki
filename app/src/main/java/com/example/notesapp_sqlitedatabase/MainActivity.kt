package com.example.notesapp_sqlitedatabase

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.notesapp_sqlitedatabase.View.NoteDetailScreen
import com.example.notesapp_sqlitedatabase.View.NoteScreen
import com.example.notesapp_sqlitedatabase.ViewModel.NoteViewModel
import com.example.notesapp_sqlitedatabase.Repository.NoteRepository
import com.example.notesapp_sqlitedatabase.View.LoginScreen
import com.example.notesapp_sqlitedatabase.View.RegisterScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            // Tạo repository với Firestore
            val repository = NoteRepository()

            // Khởi tạo ViewModel trực tiếp
            val viewModel: NoteViewModel = viewModel { NoteViewModel(repository) }

            NavHost(navController, startDestination = "login_screen") {
                composable("note_screen") {
                    NoteScreen(viewModel) { noteId ->
                        navController.navigate("note_detail_screen/$noteId")
                    }
                }
                composable("note_detail_screen/{noteId}") { backStackEntry ->
                    val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
                    NoteDetailScreen(noteId, viewModel) { navController.popBackStack() }
                }
                composable("login_screen") {
                    LoginScreen(navController)
                }
                // Thêm route "register"
                composable("register") {
                    RegisterScreen(navController)
                }

            }
        }
    }
}
