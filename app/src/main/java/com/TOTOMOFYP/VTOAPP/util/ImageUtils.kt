package com.TOTOMOFYP.VTOAPP.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Utility class for handling image operations
 */
object ImageUtils {
    private const val TAG = "ImageUtils"

    /**
     * Loads a bitmap from a Uri with proper rotation based on EXIF data
     */
    fun loadBitmapFromUri(context: Context, imageUri: Uri): Bitmap? {
        return try {
            Log.d(TAG, "Loading bitmap from URI: $imageUri")
            // Open input stream from the Uri
            val inputStream = context.contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for URI: $imageUri")
                return null
            }
            
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from input stream")
                return null
            }
            
            Log.d(TAG, "Bitmap loaded successfully, size: ${bitmap.width}x${bitmap.height}")

            // Check and correct orientation
            val rotation = getImageRotation(context, imageUri)
            if (rotation != 0) {
                Log.d(TAG, "Rotating bitmap by $rotation degrees")
                val matrix = Matrix()
                matrix.postRotate(rotation.toFloat())
                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                // Original bitmap is no longer needed
                bitmap.recycle()
                return rotatedBitmap
            } else {
                return bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from URI", e)
            e.printStackTrace()
            null
        }
    }

    /**
     * Gets the rotation angle for an image based on EXIF data
     */
    private fun getImageRotation(context: Context, imageUri: Uri): Int {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(imageUri)
            if (inputStream == null) return 0

            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error getting image rotation", e)
            e.printStackTrace()
            return 0
        } finally {
            inputStream?.close()
        }
    }

    /**
     * Saves a bitmap to a temporary file
     */
    fun saveBitmapToTempFile(context: Context, bitmap: Bitmap): File {
        try {
            Log.d(TAG, "Saving bitmap to temp file, bitmap size: ${bitmap.width}x${bitmap.height}")
            
            // Make sure the cache directory exists
            val cacheDir = context.cacheDir
            if (!cacheDir.exists()) {
                Log.d(TAG, "Cache directory doesn't exist, creating it")
                cacheDir.mkdirs()
            }
            
            // Create a unique filename with timestamp and random number to avoid collisions
            val timestamp = System.currentTimeMillis()
            val random = (Math.random() * 1000).toInt()
            val filename = "temp_image_${timestamp}_${random}.png"
            
            val tempFile = File(cacheDir, filename)
            Log.d(TAG, "Temp file path: ${tempFile.absolutePath}")
            
            // Use PNG to preserve transparency (segmented images need transparent backgrounds)
            FileOutputStream(tempFile).use { out ->
                // Use PNG format to preserve alpha channel (transparency)
                val success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                if (!success) {
                    Log.e(TAG, "Failed to compress bitmap")
                    throw IOException("Failed to compress bitmap")
                }
                out.flush()
            }
            
            // Verify file was created and has content
            if (!tempFile.exists()) {
                Log.e(TAG, "Temp file was not created")
                throw IOException("Temp file was not created")
            }
            
            if (tempFile.length() == 0L) {
                Log.e(TAG, "Temp file was created but is empty")
                throw IOException("Temp file is empty")
            }
            
            Log.d(TAG, "Bitmap saved successfully, file size: ${tempFile.length()} bytes")
            return tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap to temp file", e)
            throw e
        }
    }

    /**
     * Get a file path from a Uri
     */
    fun getFilePathFromUri(context: Context, uri: Uri): String? {
        return try {
            Log.d(TAG, "Getting file path from URI: $uri")
            // For media files, using content resolver to get the file path
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    val path = it.getString(columnIndex)
                    Log.d(TAG, "File path found: $path")
                    return path
                }
            }
            
            // Try to get the path directly
            val path = uri.path
            Log.d(TAG, "Using direct path: $path")
            path
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file path from URI", e)
            e.printStackTrace()
            null
        }
    }

    /**
     * Rotates a bitmap by the specified degrees (90, 180, 270, etc.)
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        return try {
            Log.d(TAG, "Rotating bitmap by $degrees degrees")
            val matrix = Matrix()
            matrix.postRotate(degrees)
            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            Log.d(TAG, "Bitmap rotated successfully")
            rotatedBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error rotating bitmap", e)
            bitmap // Return original bitmap if rotation fails
        }
    }
} 