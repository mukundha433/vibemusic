package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.ui.VibeAppFrame
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.VibeViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: VibeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Dynamic theme control toggled inside VIP Settings
            val isDarkTheme = remember { mutableStateOf(true) }

            MyApplicationTheme(darkTheme = isDarkTheme.value) {
                VibeAppFrame(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}
