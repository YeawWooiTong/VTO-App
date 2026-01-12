package com.TOTOMOFYP.VTOAPP.ui.wardrobe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.content.ClipboardManager
import android.content.ClipData
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.TOTOMOFYP.VTOAPP.R
import com.TOTOMOFYP.VTOAPP.ui.theme.VTOAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

class ClothingItemDetailsFragment : Fragment() {

    private val firestoreRepository = FirestoreWardrobeRepository(
        firestore = FirebaseFirestore.getInstance(), 
        auth = FirebaseAuth.getInstance()
    )
    
    private val storage = FirebaseStorage.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                VTOAppTheme {
                    ClothingItemDetailsScreen()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ClothingItemDetailsScreen() {
        val itemId = arguments?.getString("itemId") ?: ""
        var clothingItem by remember { mutableStateOf<ClothingItem?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var showDeleteConfirmation by remember { mutableStateOf(false) }
        var isEditMode by remember { mutableStateOf(false) }
        var editedName by remember { mutableStateOf("") }
        var editedDescription by remember { mutableStateOf("") }
        var editedColor by remember { mutableStateOf("") }
        var editedPattern by remember { mutableStateOf("") }
        var editedStyle by remember { mutableStateOf("") }
        var editedOccasion by remember { mutableStateOf("") }
        var isSaving by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        
        // Load clothing item data
        LaunchedEffect(itemId) {
            if (itemId.isNotEmpty()) {
                try {
                    val item = firestoreRepository.getClothingItemById(itemId)
                    android.util.Log.d("ClothingDetails", "Loaded item: ${item?.id}")
                    android.util.Log.d("ClothingDetails", "Item imageUrl: ${item?.imageUrl}")
                    android.util.Log.d("ClothingDetails", "Item imageName: ${item?.imageName}")
                    android.util.Log.d("ClothingDetails", "Item description: ${item?.description}")
                    clothingItem = item
                    // Initialize edit values
                    if (item != null) {
                        editedName = item.displayName
                        editedDescription = item.description
                        editedColor = item.color
                        editedPattern = item.pattern
                        editedStyle = item.style
                        editedOccasion = item.occasion
                    }
                    isLoading = false
                } catch (e: Exception) {
                    android.util.Log.e("ClothingDetails", "Error loading item", e)
                    errorMessage = "Error loading item: ${e.message}"
                    isLoading = false
                }
            } else {
                errorMessage = "Invalid item ID"
                isLoading = false
            }
        }
        
        Scaffold(
            topBar = { 
                // Empty top bar like wardrobe screen
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    errorMessage != null -> {
                        Text(
                            text = errorMessage ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }
                    clothingItem != null -> {
                        ClothingItemDetailsContent(
                            clothingItem = clothingItem!!,
                            isEditMode = isEditMode,
                            editedName = editedName,
                            editedDescription = editedDescription,
                            editedColor = editedColor,
                            editedPattern = editedPattern,
                            editedStyle = editedStyle,
                            editedOccasion = editedOccasion,
                            isSaving = isSaving,
                            onNameChange = { editedName = it },
                            onDescriptionChange = { editedDescription = it },
                            onColorChange = { editedColor = it },
                            onPatternChange = { editedPattern = it },
                            onStyleChange = { editedStyle = it },
                            onOccasionChange = { editedOccasion = it },
                            onEditToggle = { isEditMode = !isEditMode },
                            onSave = {
                                if (!isSaving) {
                                    isSaving = true
                                    coroutineScope.launch {
                                        try {
                                            // Update Firestore
                                            firestoreRepository.updateClothingItem(
                                                clothingItem!!.id,
                                                editedName,
                                                editedDescription,
                                                editedColor,
                                                editedPattern,
                                                editedStyle,
                                                editedOccasion
                                            )
                                            // Update local state
                                            clothingItem = clothingItem!!.copy(
                                                displayName = editedName,
                                                description = editedDescription,
                                                color = editedColor,
                                                pattern = editedPattern,
                                                style = editedStyle,
                                                occasion = editedOccasion
                                            )
                                            isEditMode = false
                                            Toast.makeText(context, "Item updated successfully!", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed to update item: ${e.message}", Toast.LENGTH_LONG).show()
                                        } finally {
                                            isSaving = false
                                        }
                                    }
                                }
                            },
                            onCancelEdit = {
                                // Reset to original values
                                clothingItem?.let { item ->
                                    editedName = item.displayName
                                    editedDescription = item.description
                                    editedColor = item.color
                                    editedPattern = item.pattern
                                    editedStyle = item.style
                                    editedOccasion = item.occasion
                                }
                                isEditMode = false
                            },
                            onDelete = {
                                showDeleteConfirmation = true
                            }
                        )
                    }
                    else -> {
                        Text(
                            text = "Item not found",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
        
        // Delete confirmation dialog
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete item") },
                text = { Text("Are you sure you want to delete this item? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            clothingItem?.let { item ->
                                // Delete the item
                                coroutineScope.launch {
                                    try {
                                        // First delete from Firestore
                                        firestoreRepository.deleteClothingItem(item.id)
                                        
                                        // Then delete the image from Firebase Storage
                                        val userId = firestoreRepository.currentUserId
                                        val storagePath = "users/$userId/wardrobe/${item.id}.png"
                                        storage.reference.child(storagePath).delete()
                                            .addOnSuccessListener {
                                                // If both operations succeed, navigate back
                                                Toast.makeText(context, "Item deleted successfully", Toast.LENGTH_SHORT).show()
                                                findNavController().navigateUp()
                                            }
                                            .addOnFailureListener { e ->
                                                // If storage deletion fails, just log it (item is already removed from Firestore)
                                                Toast.makeText(context, "Item deleted, but failed to delete image: ${e.message}", Toast.LENGTH_SHORT).show()
                                                findNavController().navigateUp()
                                            }
                                    } catch (e: Exception) {
                                        errorMessage = "Error deleting item: ${e.message}"
                                    }
                                }
                            }
                            showDeleteConfirmation = false
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    @Composable
    fun ClothingItemDetailsContent(
        clothingItem: ClothingItem,
        isEditMode: Boolean,
        editedName: String,
        editedDescription: String,
        editedColor: String,
        editedPattern: String,
        editedStyle: String,
        editedOccasion: String,
        isSaving: Boolean,
        onNameChange: (String) -> Unit,
        onDescriptionChange: (String) -> Unit,
        onColorChange: (String) -> Unit,
        onPatternChange: (String) -> Unit,
        onStyleChange: (String) -> Unit,
        onOccasionChange: (String) -> Unit,
        onEditToggle: () -> Unit,
        onSave: () -> Unit,
        onCancelEdit: () -> Unit,
        onDelete: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Title and navigation row (similar to wardrobe screen)
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
                    IconButton(onClick = { findNavController().navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = if (clothingItem.displayName.isNotEmpty()) {
                            clothingItem.displayName
                        } else {
                            generateItemDisplayName(clothingItem)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Row {
                    // Edit/Save/Cancel buttons
                    if (isEditMode) {
                        IconButton(
                            onClick = onCancelEdit,
                            enabled = !isSaving
                        ) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(
                            onClick = onSave,
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Save,
                                    contentDescription = "Save",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = onEditToggle) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = onDelete,
                        enabled = !isEditMode
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = if (isEditMode) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Main content
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                // Image
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(clothingItem.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Clothing Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Fit,
                        placeholder = painterResource(id = R.drawable.ic_camera),
                        error = painterResource(id = R.drawable.ic_camera)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        // Category (non-editable)
                        DetailRow(
                            label = "Category",
                            value = clothingItem.category.name.lowercase().replaceFirstChar { it.uppercase() },
                            isEditable = false
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Name (editable)
                        DetailRowEditable(
                            label = "Name",
                            value = if (isEditMode) editedName else clothingItem.displayName,
                            isEditMode = isEditMode,
                            onValueChange = onNameChange,
                            placeholder = "Add a name..."
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Editable fields
                        DetailRowEditable(
                            label = "Description",
                            value = if (isEditMode) editedDescription else clothingItem.description,
                            isEditMode = isEditMode,
                            onValueChange = onDescriptionChange,
                            placeholder = "Add a description..."
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        DetailRowEditable(
                            label = "Color",
                            value = if (isEditMode) editedColor else clothingItem.color,
                            isEditMode = isEditMode,
                            onValueChange = onColorChange,
                            placeholder = "Add color..."
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        DetailRowEditable(
                            label = "Pattern",
                            value = if (isEditMode) editedPattern else clothingItem.pattern,
                            isEditMode = isEditMode,
                            onValueChange = onPatternChange,
                            placeholder = "Add pattern..."
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        DetailRowEditable(
                            label = "Style",
                            value = if (isEditMode) editedStyle else clothingItem.style,
                            isEditMode = isEditMode,
                            onValueChange = onStyleChange,
                            placeholder = "Add style..."
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        DetailRowEditable(
                            label = "Occasion",
                            value = if (isEditMode) editedOccasion else clothingItem.occasion,
                            isEditMode = isEditMode,
                            onValueChange = onOccasionChange,
                            placeholder = "Add occasion..."
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Date added (non-editable)
                        DetailRow(
                            label = "Added",
                            value = SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault()).format(clothingItem.timestamp),
                            isEditable = false
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(88.dp)) // Bottom padding for navigation
            }
        }
    }

    @Composable
    fun DetailRow(
        label: String,
        value: String,
        isEditable: Boolean
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (value.isEmpty()) "Not specified" else value,
                style = MaterialTheme.typography.bodyLarge,
                color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
        }
    }

    @Composable
    fun DetailRowEditable(
        label: String,
        value: String,
        isEditMode: Boolean,
        onValueChange: (String) -> Unit,
        placeholder: String
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            if (isEditMode) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(placeholder) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            } else {
                Text(
                    text = if (value.isEmpty()) "Not specified" else value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    /**
     * Generates a display name for individual clothing items
     * Uses style if available, otherwise falls back to category
     */
    private fun generateItemDisplayName(item: ClothingItem): String {
        val style = item.style.trim()
        
        return if (style.isNotEmpty() && 
                  style.lowercase() != "unknown" && 
                  style.lowercase() != "test clothing item" && 
                  style.lowercase() != "not specified") {
            style.replaceFirstChar { it.uppercase() }
        } else {
            when (item.category.name.lowercase()) {
                "top" -> "Top Item"
                "bottom" -> "Bottom Item"
                "full_body" -> "Outfit"
                else -> "Clothing Item"
            }
        }
    }
} 