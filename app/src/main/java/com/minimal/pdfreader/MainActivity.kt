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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    var pdfUri by remember { mutableStateOf(intent?.data) }
                    
                    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                        androidx.activity.result.contract.ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        if (uri != null) {
                            pdfUri = uri
                        }
                    }

                    if (pdfUri != null) {
                        PdfViewer(
                            uri = pdfUri!!,
                            onOpenNewFile = { launcher.launch("application/pdf") }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().clickable { launcher.launch("application/pdf") }, 
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No PDF selected. Tap to Open a PDF.", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
