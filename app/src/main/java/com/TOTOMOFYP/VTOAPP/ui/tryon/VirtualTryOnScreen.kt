package com.TOTOMOFYP.VTOAPP.ui.tryon

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.TOTOMOFYP.VTOAPP.R
import com.TOTOMOFYP.VTOAPP.ui.theme.bottomNavPadding
import com.TOTOMOFYP.VTOAPP.ui.theme.bottomActionPadding
import com.TOTOMOFYP.VTOAPP.ui.wardrobe.ClothingImageItem
import com.TOTOMOFYP.VTOAPP.ui.wardrobe.WardrobeCategory
import android.content.res.Resources
import androidx.compose.foundation.layout.fillMaxHeight

private fun getNavigationBarHeight(): Float {
    val resources = Resources.getSystem()
    val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        resources.getDimensionPixelSize(resourceId).toFloat()
    } else {
        56f * resources.displayMetrics.density // A fallback of approximately 56dp
    }
}

@Composable
fun ClothingItemThumbnail(
    clothingItem: ClothingImageItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    mode: TryOnMode = TryOnMode.SINGLE,
    clothingCategory: WardrobeCategory = WardrobeCategory.ALL
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Square aspect ratio to prevent overflow
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(clothingItem.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Clothing item",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit, // Fit to prevent overflow
            placeholder = painterResource(id = R.drawable.ic_camera),
            error = painterResource(id = R.drawable.ic_camera)
        )

        // Show category indicator in combination mode
        if (mode == TryOnMode.COMBINATION) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        when (clothingCategory) {
                            WardrobeCategory.TOP -> Color(0xFF4CAF50) // Green for top
                            WardrobeCategory.BOTTOM -> Color(0xFF2196F3) // Blue for bottom
                            else -> MaterialTheme.colorScheme.outline
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (clothingCategory) {
                        WardrobeCategory.TOP -> "U"
                        WardrobeCategory.BOTTOM -> "L"
                        else -> "?"
                    },
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualTryOnScreen(
    tryOnViewModel: TryOnViewModel,
    onBackClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    val context = LocalContext.current

    // Collect states from ViewModel
    val userPhotoUri by tryOnViewModel.userPhotoUri.collectAsState()
    val selectedClothingItem by tryOnViewModel.selectedClothingItem.collectAsState()
    val selectedUpperClothing by tryOnViewModel.selectedUpperClothing.collectAsState()
    val selectedLowerClothing by tryOnViewModel.selectedLowerClothing.collectAsState()
    val tryOnMode by tryOnViewModel.tryOnMode.collectAsState()
    val wardrobeItems by tryOnViewModel.wardrobeItems.collectAsState()
    val isLoading by tryOnViewModel.isLoading.collectAsState()
    val isSaving by tryOnViewModel.isSaving.collectAsState()
    val successMessage by tryOnViewModel.successMessage.collectAsState()
    val clothesSelectorVisible by tryOnViewModel.clothesSelectorVisible.collectAsState()
    val generatedImageUri by tryOnViewModel.generatedImageUri.collectAsState()

    // Local UI state - removed unused filters

    // Save dialog state
    var showSaveDialog by remember { mutableStateOf(false) }
    var outfitName by remember { mutableStateOf("") }

    // State for bottom sheet height (0.3f = minimized, 0.7f = expanded)
    var sheetHeight by remember { mutableStateOf(0.7f) }

    // State for the bottom sheet visibility - simplified

    // Effect to load wardrobe items when screen is first displayed
    LaunchedEffect(Unit) {
        tryOnViewModel.loadWardrobeItems()
    }

    // Clear success message after a delay
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(3000)
            tryOnViewModel.clearSuccessMessage()
        }
    }
    
    // Clear error message after a delay
    val errorMessage by tryOnViewModel.errorMessage.collectAsState()
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            kotlinx.coroutines.delay(3000) // Show error for 3 seconds
            tryOnViewModel.clearErrorMessage()
        }
    }

    // Save outfit dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Outfit") },
            text = {
                Column {
                    Text("Enter a name for your outfit")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = outfitName,
                        onValueChange = { outfitName = it },
                        label = { Text("Outfit Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (outfitName.isNotBlank()) {
                            tryOnViewModel.saveOutfit(context, outfitName)
                            showSaveDialog = false
                            outfitName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showSaveDialog = false
                        outfitName = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.outline
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Spacer(modifier = Modifier.height(30.dp))
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main content
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // Title with back button and save button (similar to other pages)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Virtual Try-On",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Save button
                Button(
                    onClick = {
                        if (userPhotoUri != null && generatedImageUri != null) {
                            showSaveDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = userPhotoUri != null && generatedImageUri != null && !isSaving
                ) {
                    if (isSaving) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text("Save")
                    }
                }
            }

            // Main content area - takes remaining space
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Photo display area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (userPhotoUri != null) {
                        // Display user photo filling the entire available space
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(userPhotoUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "User Photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop  // Fill the space, crop if necessary
                        )
                    } else {
                        // Show placeholder when no photo is taken
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = "Camera",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Take a photo to try on clothes",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Loading overlay
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Generating your outfit...",
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    // Success message
                    successMessage?.let { message ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .align(Alignment.TopCenter)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF4CAF50).copy(alpha = 0.9f))
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Success",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Error message
                    val errorMessage by tryOnViewModel.errorMessage.collectAsState()
                    if (errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .align(Alignment.TopCenter)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFF5252).copy(alpha = 0.9f))
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Error",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Action buttons at the bottom of the photo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Clothing item preview area - fixed overflow
                        when (tryOnMode) {
                            TryOnMode.SINGLE -> {
                                selectedClothingItem?.let { clothingItem ->
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                width = 2.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(clothingItem.imageUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Selected clothing preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                } ?: Box(modifier = Modifier.size(48.dp))
                            }

                            TryOnMode.COMBINATION -> {
                                // Show both upper and lower items with fixed sizing
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Upper clothing preview
                                    selectedUpperClothing?.let { upperItem ->
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .border(
                                                    width = 1.dp,
                                                    color = Color(0xFF4CAF50),
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(upperItem.imageUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = "Selected upper clothing",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    } ?: Box(modifier = Modifier.size(40.dp))

                                    // Lower clothing preview
                                    selectedLowerClothing?.let { lowerItem ->
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .border(
                                                    width = 1.dp,
                                                    color = Color(0xFF2196F3),
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(lowerItem.imageUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = "Selected lower clothing",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    } ?: Box(modifier = Modifier.size(40.dp))
                                }
                            }
                        }

                        // Wardrobe button
                        FloatingActionButton(
                            onClick = { tryOnViewModel.showClothesSelector(true) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_wardrobe),
                                contentDescription = "Wardrobe",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Generate button at the bottom
                Button(
                    onClick = {
                        if (userPhotoUri != null && tryOnViewModel.hasValidClothingSelection()) {
                            tryOnViewModel.generateOutfit(context)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(48.dp),
                    enabled = userPhotoUri != null && tryOnViewModel.hasValidClothingSelection() && !isLoading
                ) {
                    if (isLoading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "Generate outfit",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }}
            // Clothes selector overlay (bottom sheet) - positioned as overlay
            if (clothesSelectorVisible) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(sheetHeight)
                ) {
                    Column {
                        // Pull handle and close button header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Close button at top left
                            IconButton(
                                onClick = { tryOnViewModel.showClothesSelector(false) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Pull handle in center (draggable)
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        RoundedCornerShape(2.dp)
                                    )
                                    .pointerInput(Unit) {
                                        detectDragGestures { _, dragAmount ->
                                            val deltaY = dragAmount.y
                                            // Reduced sensitivity: 3000px movement = 1.0 (full range)
                                            // Dragging down (positive deltaY) should increase height (expand sheet)
                                            // Dragging up (negative deltaY) should decrease height (contract sheet)
                                            val heightChange = deltaY / 3000f
                                            val newHeight = (sheetHeight - heightChange).coerceIn(0.1f, 0.9f)
                                            sheetHeight = newHeight
                                        }
                                    }
                            )

                            // Empty space for symmetry
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                        
                        // Mode selector: Single item vs Combination
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Single mode button
                            Button(
                                onClick = { tryOnViewModel.setTryOnMode(TryOnMode.SINGLE) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (tryOnMode == TryOnMode.SINGLE)
                                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text(
                                    text = "Single Item",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }

                            
                            // Combination mode button
                            Button(
                                onClick = { tryOnViewModel.setTryOnMode(TryOnMode.COMBINATION) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (tryOnMode == TryOnMode.COMBINATION)
                                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text(
                                    text = "Upper + Lower",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        
                        // Clothes grid - show only wardrobe items
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            // Filter items based on mode
                            val filteredItems = when (tryOnMode) {
                                TryOnMode.SINGLE -> wardrobeItems
                                TryOnMode.COMBINATION -> {
                                    // Filter items by category for combination mode
                                    wardrobeItems.filter { item ->
                                        item.category == WardrobeCategory.TOP ||
                                                item.category == WardrobeCategory.BOTTOM
                                    }
                                }
                            }

                            
                            items(filteredItems) { item ->
                                ClothingItemThumbnail(
                                    clothingItem = item,
                                    isSelected = when (tryOnMode) {
                                        TryOnMode.SINGLE -> selectedClothingItem?.id == item.id
                                        TryOnMode.COMBINATION -> {
                                            selectedUpperClothing?.id == item.id ||
                                                    selectedLowerClothing?.id == item.id
                                        }
                                    },
                                    onClick = {
                                        when (tryOnMode) {
                                            TryOnMode.SINGLE -> {
                                                tryOnViewModel.selectClothingItem(item)
                                                tryOnViewModel.showClothesSelector(false)
                                            }
                                            TryOnMode.COMBINATION -> {
                                                // Select based on item category
                                                when (item.category) {
                                                    WardrobeCategory.TOP -> {
                                                        tryOnViewModel.selectUpperClothing(item)
                                                    }
                                                    WardrobeCategory.BOTTOM -> {
                                                        tryOnViewModel.selectLowerClothing(item)
                                                    }
                                                    else -> {
                                                        // For other categories, treat as single item
                                                        tryOnViewModel.selectClothingItem(item)
                                                    }
                                                }
                                                // Don't close selector immediately in combination mode
                                            }
                                        }
                                    },
                                    mode = tryOnMode,
                                    clothingCategory = item.category
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}