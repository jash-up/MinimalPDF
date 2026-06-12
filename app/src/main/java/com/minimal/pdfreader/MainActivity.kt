package com.minimal.pdfreader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.minimal.pdfreader.ui.PdfViewer
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val pdfUriState = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (intent?.action == Intent.ACTION_VIEW) {
            pdfUriState.value = intent.data
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    val pdfUri by pdfUriState.collectAsState()
                    
                    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
                    ) { uri: Uri? ->
                        if (uri != null) {
                            pdfUriState.value = uri
                        }
                    }

                    if (pdfUri != null) {
                        PdfViewer(
                            uri = pdfUri!!,
                            onOpenNewFile = { launcher.launch(arrayOf("application/pdf")) }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().clickable { launcher.launch(arrayOf("application/pdf")) }, 
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No PDF selected. Tap to Open a PDF.", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == Intent.ACTION_VIEW) {
            pdfUriState.value = intent.data
        }
    }
}
