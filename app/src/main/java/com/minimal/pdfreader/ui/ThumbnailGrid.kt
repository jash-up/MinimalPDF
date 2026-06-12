package com.minimal.pdfreader.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Composable
fun ThumbnailGrid(
    pdfRenderer: PdfRenderer,
    pageCount: Int,
    isDarkMode: Boolean,
    onPageSelected: (Int) -> Unit
) {
    val renderMutex = remember { Mutex() }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) Color.Black else Color.LightGray),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(pageCount) { index ->
            ThumbnailPage(
                pdfRenderer = pdfRenderer,
                renderMutex = renderMutex,
                pageIndex = index,
                isDarkMode = isDarkMode,
                onClick = { onPageSelected(index) }
            )
        }
    }
}

@Composable
fun ThumbnailPage(
    pdfRenderer: PdfRenderer,
    renderMutex: Mutex,
    pageIndex: Int,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(pageIndex) {
        withContext(Dispatchers.IO) {
            renderMutex.withLock {
                try {
                    val page = pdfRenderer.openPage(pageIndex)
                    try {
                        val targetWidth = 300f
                        val aspectRatio = page.height.toFloat() / page.width.toFloat()
                        val targetHeight = targetWidth * aspectRatio
                        
                        val bmp = Bitmap.createBitmap(targetWidth.toInt(), targetHeight.toInt(), Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap = bmp
                    } finally {
                        page.close()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val colorMatrix = remember {
        ColorMatrix(
            floatArrayOf(
                -1f,  0f,  0f, 0f, 255f,
                 0f, -1f,  0f, 0f, 255f,
                 0f,  0f, -1f, 0f, 255f,
                 0f,  0f,  0f, 1f,   0f
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f) // Fallback while loading
            .background(if (isDarkMode) Color.DarkGray else Color.White)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Thumbnail for page ${pageIndex + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = if (isDarkMode) ColorFilter.colorMatrix(colorMatrix) else null
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    }
}
