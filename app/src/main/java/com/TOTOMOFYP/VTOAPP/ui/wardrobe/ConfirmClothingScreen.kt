package com.TOTOMOFYP.VTOAPP.ui.wardrobe

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.TOTOMOFYP.VTOAPP.auth.AuthViewModel
import com.TOTOMOFYP.VTOAPP.ui.theme.bottomNavPadding
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.widget.Toast
import android.util.Log

/**
 * ConfirmClothingScreen allows users to preview and save segmented clothing items.
 * After successful AI categorization and save, it navigates to clothing item details.
 * 
 * @param onSuccess Called with the new item ID to navigate to clothing details.
 *                  If item ID is empty, should just refresh wardrobe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmClothingScreen(
    authViewModel: AuthViewModel,
    segmentedBitmap: Bitmap,
    initialCategory: WardrobeCategory,
    onSuccess: (String) -> Unit, // Accepts item ID parameter for navigation to details
    onBack: () -> Unit,
    viewModel: WardrobeViewModel? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var isProcessing by remember { mutableStateOf(false) }
    var processingStatus by remember { mutableStateOf("Ready to process...") }
    var aiResult by remember { mutableStateOf<String?>(null) }
    var currentBitmap by remember { mutableStateOf(segmentedBitmap) }
    var rotationDegrees by remember { mutableStateOf(0f) }
    
    
    Scaffold(
        topBar = { 
            // Empty top bar like wardrobe and outfit pages
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Title and navigation row
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
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Preview",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
            if (isProcessing) {
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
                            text = processingStatus,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .bottomNavPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Clothing Preview",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Display the segmented image (rotatable)
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = "Segmented clothing item",
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.LightGray)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Rotate button
                    OutlinedButton(
                        onClick = {
                            if (!isProcessing) {
                                rotationDegrees += 90f
                                if (rotationDegrees >= 360f) {
                                    rotationDegrees = 0f
                                }
                                currentBitmap = com.TOTOMOFYP.VTOAPP.util.ImageUtils.rotateBitmap(
                                    segmentedBitmap, rotationDegrees
                                )
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Rotate clothing item",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rotate")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Hint for user
                    Text(
                        text = "💡 Tip: Click 'Save' to automatically categorize and save this item to your wardrobe using AI.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Button(
                        onClick = {
                            // Start AI categorization and save process
                            val userId = authViewModel.currentUser.value?.uid
                            if (userId != null) {
                                coroutineScope.launch {
                                    isProcessing = true
                                    processingStatus = "Analyzing with Gemini AI..."
                                    
                                    try {
                                        // Save current rotated bitmap to temp file
                                        val tempFile = com.TOTOMOFYP.VTOAPP.util.ImageUtils.saveBitmapToTempFile(
                                            context, 
                                            currentBitmap
                                        )
                                        
                                        processingStatus = "Processing AI categorization..."
                                        
                                        // Call categorization API using ApiClient
                                        val apiClient = com.TOTOMOFYP.VTOAPP.repositories.ApiClient()
                                        val result = apiClient.categorizeClothingWithAI(
                                            userId = userId,
                                            imagePath = tempFile.absolutePath
                                        )
                                        
                                        if (result.success) {
                                            processingStatus = "Saving to wardrobe..."
                                            Log.d("ConfirmClothingScreen", "AI categorization successful, result: $result")
                                            
                                            // Smart names are now generated by the server automatically
                                            // No need for Android-side generation
                                            
                                            // Refresh wardrobe data to show the new item
                                            viewModel?.loadClothingItems()
                                            
                                            Toast.makeText(
                                                context,
                                                "Clothing item analyzed and saved to wardrobe!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            
                                            // Navigate to clothing item details with the new item ID
                                            if (!result.itemId.isNullOrEmpty()) {
                                                Log.d("ConfirmClothingScreen", "Navigating to clothing details with item ID: ${result.itemId}")
                                                onSuccess(result.itemId)
                                            } else {
                                                // Fallback: just refresh wardrobe if we can't get item ID
                                                Log.w("ConfirmClothingScreen", "No item ID in response, refreshing wardrobe")
                                                onSuccess("") // Empty string indicates just refresh wardrobe
                                            }
                                        } else {
                                            // Handle API failure
                                            Log.e("ConfirmClothingScreen", "AI categorization failed: ${result.message}")
                                            Toast.makeText(
                                                context,
                                                result.message,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        
                                    } catch (e: Exception) {
                                        Log.e("ConfirmClothingScreen", "AI categorization failed", e)
                                        val errorMsg = when {
                                            e.message?.contains("not reachable") == true -> "AI server is not reachable"
                                            e.message?.contains("timed out") == true -> "AI analysis timed out"
                                            else -> "AI analysis failed: ${e.message}"
                                        }
                                        
                                        Toast.makeText(
                                            context,
                                            errorMsg,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "User not authenticated",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            }
            }
        }
    }
}

 