package com.example.zerotrace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.zerotrace.ui.SentinelPulseScreen
import com.example.zerotrace.ui.SentinelPulseViewModel
import com.example.zerotrace.ui.theme.ZeroTraceTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SentinelPulseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZeroTraceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SentinelPulseScreen(viewModel = viewModel)
                }
            }
        }
    }
}
