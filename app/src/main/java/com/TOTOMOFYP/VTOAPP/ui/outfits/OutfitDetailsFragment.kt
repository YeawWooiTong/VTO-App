package com.TOTOMOFYP.VTOAPP.ui.outfits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.TOTOMOFYP.VTOAPP.R
import com.TOTOMOFYP.VTOAPP.auth.AuthViewModel
import com.TOTOMOFYP.VTOAPP.ui.base.BaseFragment
import com.TOTOMOFYP.VTOAPP.ui.base.ComposeScreen
import com.TOTOMOFYP.VTOAPP.ui.theme.VTOAppTheme
import com.TOTOMOFYP.VTOAPP.ui.util.optimizedImageRequest
import kotlinx.coroutines.launch

class OutfitDetailsFragment : BaseFragment() {
    
    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var outfitsViewModel: OutfitsViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize the ViewModel with the factory
        outfitsViewModel = ViewModelProvider(
            this, 
            OutfitsViewModel.Factory(authViewModel)
        )[OutfitsViewModel::class.java]
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val outfitId = arguments?.getString("outfitId") ?: ""
        
        return ComposeView(requireContext()).apply {
            setContent {
                VTOAppTheme {
                    ComposeScreen {
                        OutfitDetailsScreen(
                            outfitId = outfitId,
                            outfitsViewModel = outfitsViewModel,
                            onBackClick = { findNavController().navigateUp() },
                            onDeleteClick = { 
                                outfitsViewModel.deleteOutfit(outfitId)
                                findNavController().navigateUp()
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitDetailsScreen(
    outfitId: String,
    outfitsViewModel: OutfitsViewModel,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val uiState by outfitsViewModel.uiState.collectAsState()
    
    val outfit = remember(uiState) {
        if (uiState is OutfitsViewModel.OutfitsUiState.Success) {
            (uiState as OutfitsViewModel.OutfitsUiState.Success)
                .outfits
                .find { it.id == outfitId }
        } else null
    }
    
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val scaffoldState = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            // Title with back button and actions (similar to other pages)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
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
                        text = outfit?.name ?: "Outfit Details",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Delete button
                IconButton(onClick = { showDeleteConfirmation = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete outfit",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) { paddingValues ->
        if (outfit != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Improved Outfit image display
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.8f) // Maintain image aspect ratio
                            .background(Color.Black.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Display image based on whether we have a URL or resource
                        if (outfit.imageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(outfit.imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = outfit.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit // Use Fit instead of Crop to see full image
                            )
                        } else {
                            Image(
                                painter = androidx.compose.ui.res.painterResource(id = outfit.imageRes),
                                contentDescription = outfit.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit // Use Fit instead of Crop
                            )
                        }
                    }
                }
                
                // Favorite button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { 
                            // Toggle favorite and provide immediate feedback
                            outfitsViewModel.toggleFavorite(outfitId, !outfit.isFavorite) 
                        }
                    ) {
                        Icon(
                            imageVector = if (outfit.isFavorite) 
                                Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (outfit.isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (outfit.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                // Outfit details
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = outfit.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = outfit.date,
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                    
                    // Display AI-generated metadata if available
                    outfit.metadata?.let { metadata ->
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "AI Analysis",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                if (metadata.description.isNotEmpty()) {
                                    MetadataRow("Description", metadata.description)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                
                                if (metadata.occasion.isNotEmpty()) {
                                    MetadataRow("Occasion", metadata.occasion)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                
                                if (metadata.color.isNotEmpty()) {
                                    MetadataRow("Colors", metadata.color)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                
                                if (metadata.style.isNotEmpty()) {
                                    MetadataRow("Style", metadata.style)
                                }
                            }
                        }
                    }
                    
                }
            }
        } else {
            // Loading or error state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                if (uiState is OutfitsViewModel.OutfitsUiState.Loading) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        text = "Outfit not found",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Outfit") },
            text = { Text("Are you sure you want to delete this outfit? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )
    }
} 