package com.example.simpleledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.simpleledger.ui.LedgerApp
import com.example.simpleledger.ui.theme.SimpleLedgerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LedgerApplication).container
        setContent {
            val appearance by container.preferences.appearance.collectAsStateWithLifecycle()
            SimpleLedgerTheme(colorStyle = appearance.color) {
                LedgerApp(
                    container = container,
                    appearance = appearance,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
