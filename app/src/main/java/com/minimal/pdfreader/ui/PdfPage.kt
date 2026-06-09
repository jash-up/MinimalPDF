package com.minimal.pdfreader.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders a single PDF page into a Bitmap lazily.
 * 
 * Includes an elegant True Dark Mode via ColorMatrix inversion, which safely flips 
 * colors on the GPU rather than modifying expensive Bitmap pixels natively.
 */
@Composable
fun PdfPage(pdfRenderer: PdfRenderer, pageIndex: Int, isDarkMode: Boolean) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val density = LocalDensity.current.density
    
    LaunchedEffect(pageIndex) {
        withContext(Dispatchers.IO) {
            try {
                val page = pdfRenderer.openPage(pageIndex)
                val width = (page.width * density * 2f).toInt()
                val height = (page.height * density * 2f).toInt()
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmap = bmp
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (bitmap != null) {
        val colorMatrix = ColorMatrix(
            floatArrayOf(
                -1f,  0f,  0f, 0f, 255f,
                 0f, -1f,  0f, 0f, 255f,
                 0f,  0f, -1f, 0f, 255f,
                 0f,  0f,  0f, 1f,   0f
            )
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
                colorFilter = if (isDarkMode) ColorFilter.colorMatrix(colorMatrix) else null
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(if (isDarkMode) Color.DarkGray else Color.White),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
