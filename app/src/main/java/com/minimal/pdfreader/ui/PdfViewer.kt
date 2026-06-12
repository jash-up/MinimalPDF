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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
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
fun PdfViewer(uri: Uri, onOpenNewFile: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("MinimalPDFPrefs", android.content.Context.MODE_PRIVATE) }
    val uriString = uri.toString()
    val initialPage = remember(uriString) { sharedPrefs.getInt(uriString, 0) }
    
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var thumbFileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var thumbPdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var isDarkMode by remember { mutableStateOf(false) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showThumbnails by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPage)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uri) {
        isLoading = true
        errorMessage = null
        
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Ignore if permission taking fails (e.g. for content not via SAF)
        }
        
        // Restore page state for the new URI (safe on main thread)
        val newUriString = uri.toString()

        withContext(Dispatchers.IO) {
            try {
                // Clean up old resources on background thread
                pdfRenderer?.close()
                fileDescriptor?.close()
                thumbPdfRenderer?.close()
                thumbFileDescriptor?.close()
                
                withContext(Dispatchers.Main) {
                    pdfRenderer = null
                    fileDescriptor = null
                    thumbPdfRenderer = null
                    thumbFileDescriptor = null
                    pageCount = 0
                }

                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Could not open file."
                    }
                    return@withContext
                }

                val cachedFile = File(context.cacheDir, "cached_pdf_${uri.hashCode()}.pdf")
                val outputStream = FileOutputStream(cachedFile)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
                
                val fd = ParcelFileDescriptor.open(cachedFile, ParcelFileDescriptor.MODE_READ_ONLY)
                
                val renderer = PdfRenderer(fd)

                val fdThumb = ParcelFileDescriptor.open(cachedFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val rendererThumb = PdfRenderer(fdThumb)
                
                withContext(Dispatchers.Main) {
                    fileDescriptor = fd
                    pdfRenderer = renderer
                    thumbFileDescriptor = fdThumb
                    thumbPdfRenderer = rendererThumb
                    pageCount = renderer.pageCount
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    errorMessage = "Error loading PDF: ${e.localizedMessage}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    pdfRenderer?.close()
                    fileDescriptor?.close()
                    thumbPdfRenderer?.close()
                    thumbFileDescriptor?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    } else if (errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(errorMessage!!, color = Color.Red)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onOpenNewFile) {
                    Text("Try Another File")
                }
            }
        }
    } else if (pdfRenderer != null && pageCount > 0) {
        val firstVisible = remember { derivedStateOf { listState.firstVisibleItemIndex } }
        
        LaunchedEffect(firstVisible.value) {
            sharedPrefs.edit().putInt(uriString, firstVisible.value).apply()
        }
        
        LaunchedEffect(uriString) {
            val savedPage = sharedPrefs.getInt(uriString, 0)
            if (savedPage in 0 until pageCount) {
                listState.scrollToItem(savedPage)
            }
        }
        
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        
        val configuration = LocalConfiguration.current
        LaunchedEffect(configuration.orientation) {
            scale = 1f
            offset = Offset.Zero
        }

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
                    IconButton(onClick = { showThumbnails = !showThumbnails }) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Grid View",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = onOpenNewFile) {
                        Text("📁")
                    }
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

            if (showThumbnails && thumbPdfRenderer != null) {
                ThumbnailGrid(
                    pdfRenderer = thumbPdfRenderer!!,
                    pageCount = pageCount,
                    currentPageIndex = firstVisible.value,
                    isDarkMode = isDarkMode,
                    onPageSelected = { index ->
                        showThumbnails = false
                        coroutineScope.launch {
                            listState.scrollToItem(index)
                        }
                    }
                )
            }
        }
    }
}
