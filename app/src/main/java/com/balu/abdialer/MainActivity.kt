package com.balu.abdialer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.balu.abdialer.ui.DialerScreen
import com.balu.abdialer.ui.theme.ABDialerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ABDialerTheme {
                DialerScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
