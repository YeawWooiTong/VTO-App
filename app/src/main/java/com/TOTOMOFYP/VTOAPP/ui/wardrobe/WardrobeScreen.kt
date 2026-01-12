package com.TOTOMOFYP.VTOAPP.ui.wardrobe

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.TOTOMOFYP.VTOAPP.R
import com.TOTOMOFYP.VTOAPP.auth.AuthViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import kotlinx.coroutines.launch

@Composable
fun WardrobeScreen(
    authViewModel: AuthViewModel,
    onAddClothingItem: () -> Unit,
    onClothingItemClick: (ClothingItem) -> Unit,
    wardrobeViewModel: WardrobeViewModel = viewModel(
        factory = WardrobeViewModel.Factory(authViewModel)
    )
) {
    val currentUser by authViewModel.currentUser.observeAsState()
    val uiState by wardrobeViewModel.uiState.collectAsState()
    val currentCategory by wardrobeViewModel.currentCategory.collectAsState()
    val capturedImageUri by wardrobeViewModel.capturedImageUri.collectAsState()
    val segmentedBitmap by wardrobeViewModel.segmentedBitmap.collectAsState()
    
    var showCamera by remember { mutableStateOf(false) }
    var isProcessingSegmentation by remember { mutableStateOf(false) }
    var showConfirmClothing by remember { mutableStateOf(false) }
    var showMediaPicker by remember { mutableStateOf(false) }
    
    val categories = listOf(
        WardrobeCategory.ALL,
        WardrobeCategory.TOP,
        WardrobeCategory.BOTTOM,
        WardrobeCategory.FULL_BODY
    )
    
    val context = LocalContext.current
    
    // Create a temporary file for storing the camera image
    val photoFile = remember {
        try {
            val storageDir = context.getExternalFilesDir("Pictures")
            File.createTempFile(
                "JPEG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}_",
                ".jpg",
                storageDir
            )
        } catch (ex: IOException) {
            Log.e("WardrobeScreen", "Error creating image file", ex)
            null
        }
    }
    
    // URI for the captured image
    val photoUri = remember(photoFile) {
        photoFile?.let {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                it
            )
        }
    }
    
    // Camera launcher using the device's default camera app
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            // After capturing, directly process with automatic segmentation
            wardrobeViewModel.setCapturedImageUri(photoUri.toString())
            processImageWithAutoSegmentation(
                imageUri = photoUri,
                context = context,
                wardrobeViewModel = wardrobeViewModel,
                setLoading = { isProcessingSegmentation = it },
                onSuccess = { showConfirmClothing = true }
            )
        } else {
            Log.e("WardrobeScreen", "Failed to capture image or photoUri is null")
            Toast.makeText(context, "Failed to capture image", Toast.LENGTH_SHORT).show()
        }
    }

    // Add gallery image picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // After selecting from gallery, directly process with automatic segmentation
            wardrobeViewModel.setCapturedImageUri(uri.toString())
            processImageWithAutoSegmentation(
                imageUri = uri,
                context = context,
                wardrobeViewModel = wardrobeViewModel,
                setLoading = { isProcessingSegmentation = it },
                onSuccess = { showConfirmClothing = true }
            )
        } else {
            Toast.makeText(context, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Storage permission launcher for older Android versions
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, launch gallery
            galleryLauncher.launch("image/*")
        } else {
            Toast.makeText(
                context,
                "Storage permission is required to select images",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    // Helper function to launch gallery with permission check
    val launchGallery = {
        // Check if we need storage permission (for Android < 13)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                // Permission already granted, launch gallery
                galleryLauncher.launch("image/*")
            } else {
                // Request permission
                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {
            // Android 13+ uses the system picker which doesn't require permission
            galleryLauncher.launch("image/*")
        }
    }

    if (isProcessingSegmentation) {
        // Show loading screen during automatic segmentation
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Processing image with AI...",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Detecting and segmenting clothes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else if (showConfirmClothing && segmentedBitmap != null) {
        ConfirmClothingScreen(
            authViewModel = authViewModel,
            segmentedBitmap = segmentedBitmap!!,
            initialCategory = currentCategory,
            onSuccess = { itemId ->
                // After successful AI categorization and server upload
                showConfirmClothing = false
                wardrobeViewModel.setSegmentedBitmap(null)
                // Reload wardrobe items from Firebase to show newly added item
                wardrobeViewModel.loadClothingItems()
                
                // Navigate to clothing item details if we got an item ID
                if (itemId.isNotEmpty()) {
                    // After loading items, find the new item and navigate to it
                    // We can create a temporary ClothingItem for navigation
                    val newItem = ClothingItem(
                        id = itemId,
                        imageUrl = "",
                        imageName = "New Item",
                        category = currentCategory,
                        color = "",
                        timestamp = Date(),
                        description = "",
                        pattern = "",
                        style = "",
                        occasion = ""
                    )
                    // Navigate to the clothing item details
                    onClothingItemClick(newItem)
                }
            },
            onBack = {
                // Return to the main screen without uploading
                showConfirmClothing = false
                wardrobeViewModel.setSegmentedBitmap(null)
            },
            viewModel = wardrobeViewModel
        )
    } else if (showCamera) {
        // Launch the device's default camera app when showCamera is true
        LaunchedEffect(Unit) {
            if (photoUri != null) {
                cameraLauncher.launch(photoUri)
                // Reset showCamera after launching
                showCamera = false
            } else {
                Toast.makeText(context, "Cannot create photo file", Toast.LENGTH_SHORT).show()
                showCamera = false
            }
        }
        // Show loading indicator while camera is launching
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(
            topBar = { 
                // Empty topBar with padding
                Spacer(modifier = Modifier.height(48.dp))
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { 
                        // Show media picker dialog
                        showMediaPicker = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(bottom = 76.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add clothing item"
                    )
                }
            },
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(
                left = 0.dp,
                top = 0.dp,
                right = 0.dp,
                bottom = 0.dp
            )
        ) { paddingValues ->
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
            ) {
                // Title with significantly increased top padding 
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = "My Wardrobe",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Replace Tab row with scrollable row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = category == currentCategory,
                            onClick = { wardrobeViewModel.setCategory(category) },
                            label = {
                                Text(
                                    text = category.name.lowercase().capitalize(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (category == currentCategory) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
                
                // Content area
                when (uiState) {
                    is WardrobeUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is WardrobeUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (uiState as WardrobeUiState.Error).message,
                                color = Color.Red,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    is WardrobeUiState.Empty -> {
                        EmptyWardrobeState(
                            category = currentCategory,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is WardrobeUiState.Success -> {
                        val clothingImages = (uiState as WardrobeUiState.Success).items
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(
                                start = 8.dp,
                                end = 8.dp, 
                                top = 8.dp,
                                bottom = 88.dp
                            ),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(clothingImages) { imageItem ->
                                ClothingImageCard(
                                    item = imageItem,
                                    allItems = clothingImages, // Pass all items for smart naming
                                    onClick = {
                                        // Convert ClothingImageItem to ClothingItem for navigation
                                        onClothingItemClick(
                                            ClothingItem(
                                                id = imageItem.id,
                                                imageUrl = imageItem.imageUrl,
                                                imageName = imageItem.imageName,
                                                category = imageItem.category,
                                                color = imageItem.color,
                                                description = imageItem.description,
                                                pattern = imageItem.pattern,
                                                style = imageItem.style,
                                                occasion = imageItem.occasion,
                                                timestamp = Date(imageItem.uploadTimestamp)
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Media picker dialog
    if (showMediaPicker) {
        MediaPickerDialog(
            onDismiss = { showMediaPicker = false },
            onCameraSelected = { 
                showMediaPicker = false
                showCamera = true 
            },
            onGallerySelected = {
                showMediaPicker = false
                // Launch gallery with permission check
                launchGallery()
            }
        )
    }
}

@Composable
fun MediaPickerDialog(
    onDismiss: () -> Unit,
    onCameraSelected: () -> Unit,
    onGallerySelected: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add to Wardrobe",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Choose how to add a clothing item to your wardrobe",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Camera option
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onCameraSelected)
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = "Camera",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Camera",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    // Gallery option
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onGallerySelected)
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Gallery",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Gallery",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ClothingImageCard(
    item: ClothingImageItem,
    allItems: List<ClothingImageItem>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // Use Coil to load the image from URL
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.imageUrl)
                        .crossfade(true)
                        .listener(
                            onStart = { Log.d("ClothingImage", "Loading image: ${item.imageUrl}") },
                            onSuccess = { _, _ -> Log.d("ClothingImage", "Successfully loaded: ${item.imageUrl}") },
                            onError = { _, error -> Log.e("ClothingImage", "Failed to load: ${item.imageUrl}", error.throwable) }
                        )
                        .build(),
                    contentDescription = "Clothing Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f) // Square aspect ratio to prevent overflow
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit, // Fit the entire image without cropping
                    placeholder = painterResource(id = R.drawable.ic_camera),
                    error = painterResource(id = R.drawable.ic_camera)
                )
                
                // Status indicator
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.Green)
                            .align(Alignment.TopEnd)
                    )
            }
            
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (item.displayName.isNotEmpty()) {
                        item.displayName
                    } else {
                        generateSmartItemName(item, allItems)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = item.category.name.capitalize(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EmptyWardrobeState(
    category: WardrobeCategory,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FavoriteBorder,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = Color.LightGray
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No ${category.name.lowercase().capitalize()} found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Add items to your wardrobe to see them here",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// Helper function to capitalize first letter
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

// Helper function to parse color string to Color
internal fun parseColor(colorName: String): Color {
    return when (colorName.lowercase()) {
        "red" -> Color.Red
        "blue" -> Color.Blue
        "green" -> Color.Green
        "black" -> Color.Black
        "white" -> Color.White
        else -> Color.Gray
    }
}

// Process image with automatic segmentation
private fun processImageWithAutoSegmentation(
    imageUri: Uri,
    context: Context,
    wardrobeViewModel: WardrobeViewModel,
    setLoading: (Boolean) -> Unit,
    onSuccess: () -> Unit
) {
    setLoading(true)
    
    // Use coroutine scope from the composition
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
        try {
            val apiClient = com.TOTOMOFYP.VTOAPP.repositories.ApiClient()
            
            // Check if API server is reachable
            Log.d("WardrobeScreen", "Checking if API server is reachable...")
            val isReachable = apiClient.isServerReachable()
            
            if (!isReachable) {
                throw IOException("API server at ${apiClient.getSegmentationApiUrl()} is not reachable. Please check if the server is running.")
            }
            
            // Load bitmap from URI
            val bitmap = com.TOTOMOFYP.VTOAPP.util.ImageUtils.loadBitmapFromUri(context, imageUri)
            if (bitmap == null) {
                throw IOException("Failed to load image from URI")
            }
            
            // Save bitmap to temp file
            val tempFile = com.TOTOMOFYP.VTOAPP.util.ImageUtils.saveBitmapToTempFile(context, bitmap)
            Log.d("WardrobeScreen", "Image saved to temp file: ${tempFile.absolutePath}")
            
            // Show processing message
            Toast.makeText(
                context,
                "Processing image with automatic segmentation...",
                Toast.LENGTH_LONG
            ).show()
            
            // Call automatic segmentation API
            Log.d("WardrobeScreen", "Calling automatic segmentation API")
            val segmentedBitmap = apiClient.segmentImageAutomatic(
                context = context,
                imagePath = tempFile.absolutePath
            )
            
            Log.d("WardrobeScreen", "Automatic segmentation successful")
            
            // Set the segmented bitmap and trigger confirm screen
            wardrobeViewModel.setSegmentedBitmap(segmentedBitmap)
            setLoading(false)
            onSuccess() // This will set showConfirmClothing = true
            
        } catch (e: Exception) {
            Log.e("WardrobeScreen", "Automatic segmentation failed", e)
            val errorMsg = when {
                e.message?.contains("not reachable") == true -> "API server is not reachable. Please check if the server is running."
                e.message?.contains("timed out") == true -> "Connection timed out. Please check that the API server is running and accessible."
                e.message?.contains("Failed to connect") == true -> "Could not connect to the API server. Please check your network connection and server status."
                e.message?.contains("Connection refused") == true -> "Connection refused. The API server may not be running."
                e.message?.contains("No clothes detected") == true -> "No clothes were detected in the image. Please try with a clearer image of clothing."
                else -> "Segmentation failed: ${e.message}"
            }
            
            Toast.makeText(
                context,
                errorMsg,
                Toast.LENGTH_LONG
            ).show()
            
            // Reset states on error
            wardrobeViewModel.setCapturedImageUri(null)
            setLoading(false)
        }
    }
}

/**
 * Generates a smart name for clothing items based on their style and count
 * Examples: "Casual 1", "Formal 2", "Business 3", etc.
 */
fun generateSmartItemName(currentItem: ClothingImageItem, allItems: List<ClothingImageItem>): String {
    val style = currentItem.style.trim()
    
    // If style is empty or not available, use category as fallback
    val baseName = if (style.isNotEmpty() && 
                      style.lowercase() != "unknown" && 
                      style.lowercase() != "test clothing item" && 
                      style.lowercase() != "not specified") {
        style.replaceFirstChar { it.uppercase() }
    } else {
        // Use category as fallback
        when (currentItem.category.name.lowercase()) {
            "top" -> "Top"
            "bottom" -> "Bottom" 
            "full_body" -> "Outfit"
            else -> "Item"
        }
    }
    
    // Filter items with the same style/category
    val sameStyleItems = allItems.filter { item ->
        val itemStyle = item.style.trim()
        val itemBaseName = if (itemStyle.isNotEmpty() && 
                               itemStyle.lowercase() != "unknown" && 
                               itemStyle.lowercase() != "test clothing item" && 
                               itemStyle.lowercase() != "not specified") {
            itemStyle.replaceFirstChar { it.uppercase() }
        } else {
            when (item.category.name.lowercase()) {
                "top" -> "Top"
                "bottom" -> "Bottom"
                "full_body" -> "Outfit" 
                else -> "Item"
            }
        }
        itemBaseName == baseName
    }
    
    // Sort by upload timestamp to maintain consistent numbering
    val sortedItems = sameStyleItems.sortedBy { it.uploadTimestamp }
    
    // Find the index of current item in the sorted list
    val itemIndex = sortedItems.indexOfFirst { it.id == currentItem.id }
    val itemNumber = if (itemIndex >= 0) itemIndex + 1 else 1
    
    return "$baseName $itemNumber"
} 