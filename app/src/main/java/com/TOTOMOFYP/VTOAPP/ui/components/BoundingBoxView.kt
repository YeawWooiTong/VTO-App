package com.TOTOMOFYP.VTOAPP.ui.components

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Custom Composable that allows drawing a bounding box on an image
 */
@Composable
fun BoundingBoxView(
    bitmap: Bitmap,
    onBoundingBoxDrawn: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    var startPoint by remember { mutableStateOf<Offset?>(null) }
    var endPoint by remember { mutableStateOf<Offset?>(null) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var scaledImageSize by remember { mutableStateOf(Size(0f, 0f)) }
    var imageOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    val density = LocalDensity.current

    // Canvas size will be determined when drawn
    var canvasSize by remember { mutableStateOf(Size(0f, 0f)) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                imageSize = coordinates.size
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        startPoint = offset
                        endPoint = offset
                    },
                    onDrag = { change, _ ->
                        endPoint = change.position
                    },
                    onDragEnd = {
                        if (startPoint != null && endPoint != null && scaledImageSize.width > 0) {
                            // Adjust points to be relative to the image's position
                            val adjustedStartX = (startPoint!!.x - imageOffset.x)
                            val adjustedStartY = (startPoint!!.y - imageOffset.y)
                            val adjustedEndX = (endPoint!!.x - imageOffset.x)
                            val adjustedEndY = (endPoint!!.y - imageOffset.y)
                            
                            // Check if the points are within the image bounds
                            if (adjustedStartX >= 0 && adjustedStartX <= scaledImageSize.width &&
                                adjustedStartY >= 0 && adjustedStartY <= scaledImageSize.height &&
                                adjustedEndX >= 0 && adjustedEndX <= scaledImageSize.width &&
                                adjustedEndY >= 0 && adjustedEndY <= scaledImageSize.height) {
                                
                                // Convert screen coordinates to image coordinates
                                val scaleFactorX = bitmap.width / scaledImageSize.width
                                val scaleFactorY = bitmap.height / scaledImageSize.height
                                
                                val imageStartX = (adjustedStartX * scaleFactorX).toInt()
                                val imageStartY = (adjustedStartY * scaleFactorY).toInt()
                                val imageEndX = (adjustedEndX * scaleFactorX).toInt()
                                val imageEndY = (adjustedEndY * scaleFactorY).toInt()
                                
                                // Create a rectangle in image coordinates
                                val left = min(imageStartX, imageEndX)
                                val top = min(imageStartY, imageEndY)
                                val right = max(imageStartX, imageEndX)
                                val bottom = max(imageStartY, imageEndY)
                                
                                // Ensure bounds are within image dimensions
                                val boundedLeft = max(0, min(bitmap.width, left))
                                val boundedTop = max(0, min(bitmap.height, top))
                                val boundedRight = max(0, min(bitmap.width, right))
                                val boundedBottom = max(0, min(bitmap.height, bottom))
                                
                                val rect = Rect(boundedLeft, boundedTop, boundedRight, boundedBottom)
                                
                                // Only report if there is a meaningful box (not too small)
                                if (rect.width() > 10 && rect.height() > 10) {
                                    onBoundingBoxDrawn(rect)
                                }
                            }
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Calculate the scaling factor to fit the image correctly
            val canvasWidth = size.width
            val canvasHeight = size.height
            canvasSize = Size(canvasWidth, canvasHeight)
            
            val imageWidth = imageBitmap.width
            val imageHeight = imageBitmap.height
            
            // Calculate aspect ratios
            val imageRatio = imageWidth.toFloat() / imageHeight.toFloat()
            val canvasRatio = canvasWidth / canvasHeight
            
            // Determine dimensions to maintain aspect ratio while fitting within canvas
            val scaledWidth: Float
            val scaledHeight: Float
            
            if (imageRatio > canvasRatio) {
                // Image is wider than canvas (relative to height)
                scaledWidth = canvasWidth
                scaledHeight = canvasWidth / imageRatio
            } else {
                // Image is taller than canvas (relative to width)
                scaledHeight = canvasHeight
                scaledWidth = canvasHeight * imageRatio
            }
            
            // Calculate position to center the image
            val left = (canvasWidth - scaledWidth) / 2
            val top = (canvasHeight - scaledHeight) / 2
            
            // Update image position and scaled size for coordinate conversion
            imageOffset = Offset(left, top)
            scaledImageSize = Size(scaledWidth, scaledHeight)
            
            // Draw the image - convert to IntOffset and IntSize for drawImage
            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                dstSize = IntSize(scaledWidth.roundToInt(), scaledHeight.roundToInt())
            )
            
            // Draw the bounding box if we have start and end points
            if (startPoint != null && endPoint != null) {
                val left = min(startPoint!!.x, endPoint!!.x)
                val top = min(startPoint!!.y, endPoint!!.y)
                val right = max(startPoint!!.x, endPoint!!.x)
                val bottom = max(startPoint!!.y, endPoint!!.y)
                
                drawRect(
                    color = Color.Red,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(width = 4f)
                )
            }
        }
    }
} 