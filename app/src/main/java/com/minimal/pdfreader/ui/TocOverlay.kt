package com.minimal.pdfreader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TocOverlay(
    toc: List<TocItem>,
    isDarkMode: Boolean,
    onTocItemSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) Color(0xEE000000) else Color(0xEEFFFFFF))
    ) {
        if (toc.isEmpty()) {
            Text(
                text = "No Table of Contents found.",
                color = if (isDarkMode) Color.White else Color.Black,
                modifier = Modifier.padding(80.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 80.dp, bottom = 80.dp)
            ) {
                items(toc) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTocItemSelected(item.pageIndex) }
                    ) {
                        Text(
                            text = item.title,
                            color = if (isDarkMode) Color.White else Color.Black,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .padding(
                                    start = 16.dp + (item.indentLevel * 16).dp,
                                    end = 16.dp,
                                    top = 12.dp,
                                    bottom = 12.dp
                                )
                        )
                        HorizontalDivider(color = if (isDarkMode) Color.DarkGray else Color.LightGray)
                    }
                }
            }
        }
    }
}
