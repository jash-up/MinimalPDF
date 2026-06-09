package com.minimal.pdfreader.ui

import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * PdfViewer: A minimalist, native PDF viewer composable.
 * Features:
 * - Lazy loading via android.graphics.pdf.PdfRenderer to prevent OOM errors.
 * - Resolves scaling conflicts by managing pinch-to-zoom and pan on the parent container.
 * - Preserves native vertical scroll performance seamlessly when unscaled (scale == 1f).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewer(uri: Uri) {
    val context = LocalContext.current
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var isDarkMode by remember { mutableStateOf(false) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var showJumpDialog by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = File(context.cacheDir, "temp.pdf")
                val outputStream = FileOutputStream(tempFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                
                val fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(fd)
                pageCount = pdfRenderer?.pageCount ?: 0
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (showJumpDialog) {
        var pageText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("Jump to Page") },
            text = {
                OutlinedTextField(
                    value = pageText,
                    onValueChange = { pageText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val page = pageText.toIntOrNull()
                    if (page != null && page in 1..pageCount) {
                        coroutineScope.launch {
                            listState.scrollToItem(page - 1)
                        }
                    }
                    showJumpDialog = false
                }) {
                    Text("Jump")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (pdfRenderer != null && pageCount > 0) {
        val firstVisible = remember { derivedStateOf { listState.firstVisibleItemIndex } }
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkMode) Color.Black else Color.LightGray)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { isControlsVisible = !isControlsVisible })
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale == 1f) {
                            offset = Offset.Zero
                        } else {
                            val maxOffsetX = (size.width * (scale - 1)) / 2f
                            val maxOffsetY = (size.height * (scale - 1)) / 2f
                            offset = Offset(
                                x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            )
                        }
                    }
                }
        ) {
            LazyColumn(
                state = listState,
                userScrollEnabled = scale == 1f,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            ) {
                items(pageCount) { index ->
                    PdfPage(
                        pdfRenderer = pdfRenderer!!,
                        pageIndex = index,
                        isDarkMode = isDarkMode
                    )
                }
            }

            AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .background(Color(0xCC000000), RoundedCornerShape(24.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { isDarkMode = !isDarkMode }) {
                        Text(if (isDarkMode) "Normal Mode" else "Dark Mode", color = Color.White)
                    }
                }
            }

            AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .background(Color(0xCC000000), RoundedCornerShape(16.dp))
                        .clickable { showJumpDialog = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Page ${firstVisible.value + 1} / $pageCount",
                        color = Color.White
                    )
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}
