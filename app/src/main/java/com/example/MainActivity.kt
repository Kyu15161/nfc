package com.example

import android.app.Application
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.NfcAppScreen
import com.example.ui.NfcViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    
    private val viewModel: NfcViewModel by viewModels {
        NfcViewModel.Factory(applicationContext as Application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Query default NFC adapter safely
        try {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        } catch (e: Exception) {
            nfcAdapter = null
        }
        if (nfcAdapter == null) {
            viewModel.setScanningActive(false)
        }

        setContent {
            val isDark by viewModel.isDarkMode.collectAsState()
            MyApplicationTheme(darkTheme = isDark) {
                // Render interactive view with active bindings
                NfcAppScreen(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Enable Modern reader mode flags securely
        try {
            nfcAdapter?.let { adapter ->
                if (adapter.isEnabled) {
                    viewModel.setScanningActive(true)
                    val flags = NfcAdapter.FLAG_READER_NFC_A or
                                NfcAdapter.FLAG_READER_NFC_B or
                                NfcAdapter.FLAG_READER_NFC_F or
                                NfcAdapter.FLAG_READER_NFC_V or
                                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
                    
                    adapter.enableReaderMode(this, { tag ->
                        viewModel.onPhysicalCardDetected(tag)
                    }, flags, null)
                } else {
                    viewModel.setScanningActive(false)
                }
            }
        } catch (e: Exception) {
            viewModel.setScanningActive(false)
        }
    }

    override fun onPause() {
        super.onPause()
        // Stop listener and release resource focus safely
        try {
            nfcAdapter?.disableReaderMode(this)
        } catch (e: Exception) {
            // Safe fallback if NFC service is dead or unavailable
        }
    }
}

