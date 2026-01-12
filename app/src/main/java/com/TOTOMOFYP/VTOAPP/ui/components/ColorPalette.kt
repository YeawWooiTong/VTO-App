package com.TOTOMOFYP.VTOAPP.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A horizontal color palette component that displays a list of colors and allows selection.
 * This component can be embedded in XML layouts using AndroidView.
 */
@Composable
fun ColorPalette(
    colors: List<Color>,
    selectedColor: Color? = null,
    onColorSelected: (Color) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(colors) { color ->
            ColorSwatch(
                color = color,
                isSelected = color == selectedColor,
                onClick = { onColorSelected(color) }
            )
        }
    }
}

@Composable
fun ColorSwatch(
    color: Color,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Empty content - just showing the colored box
    }
}

@Composable
fun ColorPaletteWithLabels(
    colorData: List<Pair<Color, String>>,
    selectedColor: Color? = null,
    onColorSelected: (Color) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(colorData) { (color, name) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ColorSwatch(
                    color = color,
                    isSelected = color == selectedColor,
                    onClick = { onColorSelected(color) }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ColorPalettePreview() {
    val colors = listOf(
        Color(0xFF3B5999),  // Primary blue
        Color(0xFFFF5A5F),  // Accent pink
        Color(0xFF5D4037),  // Brown
        Color(0xFF4CAF50),  // Green
        Color(0xFFFFC107),  // Amber
        Color(0xFF9E9E9E)   // Gray
    )
    
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text("Color Palette", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        ColorPalette(
            colors = colors,
            selectedColor = colors[1]
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Color Palette with Labels", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        val colorData = listOf(
            Color(0xFF3B5999) to "Blue",
            Color(0xFFFF5A5F) to "Pink",
            Color(0xFF5D4037) to "Brown",
            Color(0xFF4CAF50) to "Green"
        )
        
        ColorPaletteWithLabels(
            colorData = colorData,
            selectedColor = colorData[0].first
        )
    }
} 