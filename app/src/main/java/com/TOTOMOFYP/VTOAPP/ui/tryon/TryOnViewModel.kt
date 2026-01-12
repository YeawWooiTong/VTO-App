package com.TOTOMOFYP.VTOAPP.ui.tryon

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.TOTOMOFYP.VTOAPP.auth.AuthViewModel
import com.TOTOMOFYP.VTOAPP.repositories.KlingAIVirtualTryOn
import com.TOTOMOFYP.VTOAPP.ui.wardrobe.ClothingImageItem
import com.TOTOMOFYP.VTOAPP.ui.wardrobe.ClothingImageStorage
import com.TOTOMOFYP.VTOAPP.ui.wardrobe.WardrobeCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import com.TOTOMOFYP.VTOAPP.ui.outfits.OutfitStorage
import android.graphics.Canvas

class TryOnViewModel(private val authViewModel: AuthViewModel) : ViewModel() {
    
    private val clothingImageStorage = ClothingImageStorage()
    private val outfitStorage = OutfitStorage()
    
    // User photo state
    private val _userPhotoUri = MutableStateFlow<Uri?>(null)
    val userPhotoUri: StateFlow<Uri?> = _userPhotoUri.asStateFlow()
    
    // Selected clothing items state (supports up to 2 items: upper + lower)
    private val _selectedClothingItem = MutableStateFlow<ClothingImageItem?>(null)
    val selectedClothingItem: StateFlow<ClothingImageItem?> = _selectedClothingItem.asStateFlow()
    
    private val _selectedUpperClothing = MutableStateFlow<ClothingImageItem?>(null)
    val selectedUpperClothing: StateFlow<ClothingImageItem?> = _selectedUpperClothing.asStateFlow()
    
    private val _selectedLowerClothing = MutableStateFlow<ClothingImageItem?>(null)
    val selectedLowerClothing: StateFlow<ClothingImageItem?> = _selectedLowerClothing.asStateFlow()
    
    // Try-on mode: single item or combination
    private val _tryOnMode = MutableStateFlow<TryOnMode>(TryOnMode.SINGLE)
    val tryOnMode: StateFlow<TryOnMode> = _tryOnMode.asStateFlow()
    
    // Wardrobe items state
    private val _wardrobeItems = MutableStateFlow<List<ClothingImageItem>>(emptyList())
    val wardrobeItems: StateFlow<List<ClothingImageItem>> = _wardrobeItems.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Generated image state
    private val _generatedImageUri = MutableStateFlow<Uri?>(null)
    val generatedImageUri: StateFlow<Uri?> = _generatedImageUri.asStateFlow()
    
    // Clothes selector visible state
    private val _clothesSelectorVisible = MutableStateFlow(false)
    val clothesSelectorVisible: StateFlow<Boolean> = _clothesSelectorVisible.asStateFlow()
    
    // Saving state
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    
    // Success message state
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    fun setUserPhoto(uri: Uri?) {
        _userPhotoUri.value = uri
    }
    
    fun selectClothingItem(item: ClothingImageItem?) {
        _selectedClothingItem.value = item
    }
    
    fun selectUpperClothing(item: ClothingImageItem?) {
        _selectedUpperClothing.value = item
    }
    
    fun selectLowerClothing(item: ClothingImageItem?) {
        _selectedLowerClothing.value = item
    }
    
    fun setTryOnMode(mode: TryOnMode) {
        _tryOnMode.value = mode
        // Clear selections when switching modes
        if (mode == TryOnMode.SINGLE) {
            _selectedUpperClothing.value = null
            _selectedLowerClothing.value = null
        } else {
            _selectedClothingItem.value = null
        }
    }
    
    fun showClothesSelector(show: Boolean) {
        _clothesSelectorVisible.value = show
    }
    
    fun loadWardrobeItems() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val userId = authViewModel.currentUser.value?.uid
            if (userId == null) {
                _errorMessage.value = "User not logged in"
                _isLoading.value = false
                return@launch
            }
            
            clothingImageStorage.getClothingImages(
                userId = userId,
                category = WardrobeCategory.ALL,
                onSuccess = { items ->
                    _wardrobeItems.value = items
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message ?: "Failed to load wardrobe items"
                    _isLoading.value = false
                }
            )
        }
    }
    
    // Generate outfit using KlingAI Virtual Try-On service
    fun generateOutfit(context: Context) {
        viewModelScope.launch {
            try {
                val userPhoto = _userPhotoUri.value
                val currentMode = _tryOnMode.value
                
                if (userPhoto == null) {
                    _errorMessage.value = "Please take a photo first"
                    return@launch
                }
                
                // Validate clothing selection based on mode
                val hasValidSelection = when (currentMode) {
                    TryOnMode.SINGLE -> _selectedClothingItem.value != null
                    TryOnMode.COMBINATION -> {
                        val upper = _selectedUpperClothing.value
                        val lower = _selectedLowerClothing.value
                        upper != null && lower != null
                    }
                }
                
                if (!hasValidSelection) {
                    _errorMessage.value = when (currentMode) {
                        TryOnMode.SINGLE -> "Please select a clothing item"
                        TryOnMode.COMBINATION -> "Please select both upper and lower clothing items"
                    }
                    return@launch
                }
                
                _isLoading.value = true
                
                try {
                    // Convert user photo to base64 with validation
                    val userPhotoBase64 = withContext(Dispatchers.IO) {
                        val inputStream = context.contentResolver.openInputStream(userPhoto)
                        if (inputStream == null) {
                            throw Exception("Failed to open user photo")
                        }
                        
                        // Load and validate user photo
                        val userBitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        
                        if (userBitmap == null) {
                            throw Exception("Failed to decode user photo")
                        }
                        
                        // Validate and potentially resize user photo
                        val validatedUserBitmap = validateAndResizeImage(userBitmap, "user photo")
                        
                        // Convert to base64
                        val outputStream = java.io.ByteArrayOutputStream()
                        validatedUserBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                        val byteArray = outputStream.toByteArray()
                        
                        android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
                    }
                    
                    // Get clothing image(s) and convert to base64
                    val clothingImageBase64 = when (currentMode) {
                        TryOnMode.SINGLE -> {
                            val clothingItem = _selectedClothingItem.value!!
                            convertImageToBase64(context, clothingItem.imageUrl)
                        }
                        TryOnMode.COMBINATION -> {
                            val upperItem = _selectedUpperClothing.value!!
                            val lowerItem = _selectedLowerClothing.value!!
                            
                            // Combine upper and lower clothing images
                            combineClothingImages(context, upperItem.imageUrl, lowerItem.imageUrl)
                        }
                    }
                    
                    // Create virtual try-on task with correct model version
                    val taskId = KlingAIVirtualTryOn.createVirtualTryOnTask(
                        humanImageBase64 = userPhotoBase64,
                        clothImageBase64 = clothingImageBase64,
                        modelName = "kolors-virtual-try-on-v1-5"  // Use the documented stable version
                    )
                    
                    if (taskId == null) {
                        _errorMessage.value = "Failed to create try-on task"
                        _isLoading.value = false
                        return@launch
                    }
                    
                    // Poll task status and get generated image URL
                    val imageUrl = KlingAIVirtualTryOn.pollTaskStatus(taskId)
                    
                    if (imageUrl == null) {
                        _errorMessage.value = "Failed to generate outfit"
                        _isLoading.value = false
                        return@launch
                    }
                    
                    // Download and save the generated image
                    val generatedImageUri = downloadAndSaveImage(context, imageUrl, taskId)
                    _generatedImageUri.value = generatedImageUri
                    
                    // Update the user photo to show the generated image
                    _userPhotoUri.value = generatedImageUri
                } catch (e: Exception) {
                    Log.e("TryOnViewModel", "Error processing images", e)
                    _errorMessage.value = "Error processing images: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("TryOnViewModel", "Error generating outfit", e)
                _errorMessage.value = "Error: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun downloadAndSaveImage(context: Context, imageUrl: String, taskId: String): Uri? = withContext(Dispatchers.IO) {
        try {
            // Download image from URL
            val url = URL(imageUrl)
            val connection = url.openConnection()
            connection.connect()
            val inputStream = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            
            // Create a file in the app's cache directory
            val cacheDir = context.cacheDir
            val imageFile = File(cacheDir, "virtual_tryon_${taskId}.jpg")
            
            // Save the bitmap to the file
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            
            // Convert the file to a URI
            return@withContext Uri.fromFile(imageFile)
        } catch (e: Exception) {
            Log.e("TryOnViewModel", "Error downloading image", e)
            return@withContext null
        }
    }
    
    // Reset state when navigating away or starting new try-on
    fun resetState() {
        _userPhotoUri.value = null
        _selectedClothingItem.value = null
        _selectedUpperClothing.value = null
        _selectedLowerClothing.value = null
        _clothesSelectorVisible.value = false
        _generatedImageUri.value = null
        _tryOnMode.value = TryOnMode.SINGLE
    }
    
    /**
     * Convert image URL to base64
     */
    private suspend fun convertImageToBase64(context: Context, imageUrl: String): String = withContext(Dispatchers.IO) {
        if (imageUrl.startsWith("http")) {
            // If it's a URL, download it directly
            KlingAIVirtualTryOn.downloadImageToBase64(imageUrl)
        } else {
            // If it's a local URI, use the content resolver
            val clothingUri = Uri.parse(imageUrl)
            val inputStream = context.contentResolver.openInputStream(clothingUri)
            if (inputStream == null) {
                throw Exception("Failed to open clothing image")
            }
            val bytes = inputStream.readBytes()
            inputStream.close()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }
    }
    
    /**
     * Combine upper and lower clothing images with white background
     * This creates a single image with both items positioned appropriately
     */
    private suspend fun combineClothingImages(
        context: Context, 
        upperImageUrl: String, 
        lowerImageUrl: String
    ): String = withContext(Dispatchers.IO) {
        try {
            // Download/load both images
            val upperBitmap = loadBitmapFromUrl(context, upperImageUrl)
            val lowerBitmap = loadBitmapFromUrl(context, lowerImageUrl)
            
            // Create combined image with white background
            val combinedBitmap = createCombinedClothingImage(upperBitmap, lowerBitmap)
            
            // Convert combined bitmap to base64
            val outputStream = java.io.ByteArrayOutputStream()
            combinedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            
            return@withContext android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("TryOnViewModel", "Error combining clothing images", e)
            throw Exception("Failed to combine clothing images: ${e.message}")
        }
    }
    
    /**
     * Load bitmap from URL or URI with validation
     */
    private suspend fun loadBitmapFromUrl(context: Context, imageUrl: String): Bitmap {
        val bitmap = if (imageUrl.startsWith("http")) {
            // Download from URL
            val url = URL(imageUrl)
            val connection = url.openConnection()
            connection.connect()
            val inputStream = connection.getInputStream()
            BitmapFactory.decodeStream(inputStream)
        } else {
            // Load from local URI
            val uri = Uri.parse(imageUrl)
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        }
        
        if (bitmap == null) {
            throw Exception("Failed to load image from: $imageUrl")
        }
        
        // Validate and resize if needed
        return validateAndResizeImage(bitmap, "clothing image")
    }
    
    /**
     * Create a combined image with upper clothing on top and lower clothing on bottom
     * with white background, ensuring proper aspect ratio for Kling AI
     */
    private fun createCombinedClothingImage(upperBitmap: Bitmap, lowerBitmap: Bitmap): Bitmap {
        // Kling AI requirements: minimum 300px width/height, reasonable aspect ratio
        val minDimension = 512
        val maxDimension = 1024
        
        // Scale images to reasonable sizes while maintaining aspect ratio
        val scaledUpper = scaleImageToFit(upperBitmap, maxDimension / 2, maxDimension / 2)
        val scaledLower = scaleImageToFit(lowerBitmap, maxDimension / 2, maxDimension / 2)
        
        // Calculate final dimensions - use a reasonable aspect ratio (3:4 or 4:3)
        val maxWidth = maxOf(scaledUpper.width, scaledLower.width, minDimension)
        val totalHeight = scaledUpper.height + scaledLower.height + 40 // Add 40px gap between items
        
        // Ensure minimum dimensions and reasonable aspect ratio
        val finalWidth = maxOf(maxWidth, minDimension)
        val finalHeight = maxOf(totalHeight, minDimension)
        
        // Create a new bitmap with white background
        val combinedBitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(combinedBitmap)
        
        // Fill with white background
        canvas.drawColor(android.graphics.Color.WHITE)
        
        // Calculate positions to center items with some spacing
        val upperX = (finalWidth - scaledUpper.width) / 2f
        val upperY = 20f // Small top margin
        
        val lowerX = (finalWidth - scaledLower.width) / 2f
        val lowerY = upperY + scaledUpper.height + 40f // Gap between items
        
        // Draw upper clothing
        canvas.drawBitmap(scaledUpper, upperX, upperY, null)
        
        // Draw lower clothing
        canvas.drawBitmap(scaledLower, lowerX, lowerY, null)
        
        Log.d("TryOnViewModel", "Combined image created: ${finalWidth}x${finalHeight}, aspect ratio: ${finalWidth.toFloat()/finalHeight}")
        
        return combinedBitmap
    }
    
    /**
     * Scale image to fit within specified dimensions while maintaining aspect ratio
     */
    private fun scaleImageToFit(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        
        val (newWidth, newHeight) = if (aspectRatio > 1) {
            // Landscape: limit by width
            val width = minOf(bitmap.width, maxWidth)
            val height = (width / aspectRatio).toInt()
            width to height
        } else {
            // Portrait: limit by height
            val height = minOf(bitmap.height, maxHeight)
            val width = (height * aspectRatio).toInt()
            width to height
        }
        
        return if (newWidth != bitmap.width || newHeight != bitmap.height) {
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }
    
    /**
     * Validate and resize image to meet Kling AI requirements
     * Based on research and common AI model requirements:
     * - Standard dimensions: 512x768 (portrait), 768x512 (landscape), 512x512 (square)
     * - Aspect ratios: 2:3, 3:2, 1:1 are commonly supported
     * - Higher quality requirements for virtual try-on
     */
    private fun validateAndResizeImage(bitmap: Bitmap, imageType: String): Bitmap {
        val originalAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        
        Log.d("TryOnViewModel", "Original $imageType dimensions: ${bitmap.width}x${bitmap.height}, aspect ratio: $originalAspectRatio")
        
        // Define target dimensions based on common AI model requirements
        // Portrait for human images, square/landscape for clothing
        // Based on official KlingAI requirements: minimum 300px, preserve aspect ratio
        val minDimension = 300  // Official requirement
        val maxDimension = 1024
        
        // Calculate target size that meets minimum requirements but preserves aspect ratio
        val (targetWidth, targetHeight) = if (bitmap.width < minDimension || bitmap.height < minDimension) {
            // Scale up to meet minimum requirements
            val scale = maxOf(minDimension.toFloat() / bitmap.width, minDimension.toFloat() / bitmap.height)
            ((bitmap.width * scale).toInt() to (bitmap.height * scale).toInt())
        } else if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            // Scale down if too large
            val scale = minOf(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
            ((bitmap.width * scale).toInt() to (bitmap.height * scale).toInt())
        } else {
            // Size is acceptable, keep original
            bitmap.width to bitmap.height
        }
        
        Log.d("TryOnViewModel", "Target dimensions for $imageType: ${targetWidth}x${targetHeight}")
        
        // Scale to target dimensions (preserving aspect ratio)
        val finalBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        
        Log.d("TryOnViewModel", "Final $imageType dimensions: ${finalBitmap.width}x${finalBitmap.height}")
        
        return finalBitmap
    }
    
    /**
     * Center crop bitmap to target aspect ratio
     */
    private fun centerCropToAspectRatio(bitmap: Bitmap, targetAspectRatio: Float): Bitmap {
        val originalAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        
        return if (originalAspectRatio > targetAspectRatio) {
            // Image is too wide, crop horizontally
            val newWidth = (bitmap.height * targetAspectRatio).toInt()
            val cropX = (bitmap.width - newWidth) / 2
            Bitmap.createBitmap(bitmap, cropX, 0, newWidth, bitmap.height)
        } else if (originalAspectRatio < targetAspectRatio) {
            // Image is too tall, crop vertically
            val newHeight = (bitmap.width / targetAspectRatio).toInt()
            val cropY = (bitmap.height - newHeight) / 2
            Bitmap.createBitmap(bitmap, 0, cropY, bitmap.width, newHeight)
        } else {
            // Aspect ratio is already correct
            bitmap
        }
    }

    /**
     * Save the generated outfit to Firebase
     */
    fun saveOutfit(context: Context, outfitName: String) {
        viewModelScope.launch {
            try {
                val userId = authViewModel.currentUser.value?.uid
                val generatedImage = _generatedImageUri.value
                
                if (userId == null) {
                    _errorMessage.value = "User not logged in"
                    return@launch
                }
                
                if (generatedImage == null) {
                    _errorMessage.value = "No outfit to save"
                    return@launch
                }
                
                _isSaving.value = true
                
                outfitStorage.saveOutfit(
                    userId = userId,
                    uri = generatedImage,
                    name = outfitName,
                    context = context
                ).fold(
                    onSuccess = { outfitId ->
                        _successMessage.value = "Outfit saved successfully"
                        Log.d("TryOnViewModel", "Saved outfit with ID: $outfitId")
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Failed to save outfit: ${exception.message}"
                        Log.e("TryOnViewModel", "Error saving outfit", exception)
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
                Log.e("TryOnViewModel", "Error in saveOutfit", e)
            } finally {
                _isSaving.value = false
            }
        }
    }
    
    // Clear success message
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
    
    // Clear error message
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    // Check if we have valid clothing selection for current mode
    fun hasValidClothingSelection(): Boolean {
        return when (_tryOnMode.value) {
            TryOnMode.SINGLE -> _selectedClothingItem.value != null
            TryOnMode.COMBINATION -> {
                _selectedUpperClothing.value != null && _selectedLowerClothing.value != null
            }
        }
    }
    
    // Factory to create ViewModel with dependencies
    class Factory(private val authViewModel: AuthViewModel) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TryOnViewModel::class.java)) {
                return TryOnViewModel(authViewModel) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

/**
 * Enum to represent different try-on modes
 */
enum class TryOnMode {
    SINGLE,      // Try on single clothing item
    COMBINATION  // Try on upper + lower combination
} 