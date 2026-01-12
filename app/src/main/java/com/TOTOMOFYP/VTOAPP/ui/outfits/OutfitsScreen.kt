package com.TOTOMOFYP.VTOAPP.ui.outfits

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.TOTOMOFYP.VTOAPP.R
import com.TOTOMOFYP.VTOAPP.auth.AuthViewModel

@Composable
fun OutfitsScreen(
    outfitsViewModel: OutfitsViewModel,
    authViewModel: AuthViewModel,
    onCreateOutfitClick: () -> Unit,
    onOutfitClick: (Outfit) -> Unit,
    onMakeupStorageClick: () -> Unit = {},
    selectForMakeup: Boolean = false
) {
    val currentUser by authViewModel.currentUser.observeAsState()
    val uiState by outfitsViewModel.uiState.collectAsState()
    val filterState by outfitsViewModel.filterState.collectAsState()

    Scaffold(
        topBar = {
            // Empty top bar to ensure proper edge-to-edge display
        },
//        floatingActionButton = {
//            FloatingActionButton(
////                onClick = onCreateOutfitClick,
//                onClick = onCreateOutfitClick,
//                containerColor = MaterialTheme.colorScheme.primary,
//                contentColor = MaterialTheme.colorScheme.onPrimary,
//                modifier = Modifier.padding(bottom = 80.dp)
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Add,
//                    contentDescription = "Create outfit"
//                )
//            }
//        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {

            // Title with exchange button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectForMakeup) "Select Outfit for Makeup" else "Outfits",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onMakeupStorageClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Exchange",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Filter dropdown
            val isDropdownExpanded = remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = filterState.selectedCategory,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Filter by Category") },
                    trailingIcon = {
                        IconButton(onClick = { isDropdownExpanded.value = !isDropdownExpanded.value }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.5f) // Take only half width
                        .clickable { isDropdownExpanded.value = !isDropdownExpanded.value },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                DropdownMenu(
                    expanded = isDropdownExpanded.value,
                    onDismissRequest = { isDropdownExpanded.value = false },
                    modifier = Modifier.fillMaxWidth(0.5f)
                ) {
                    filterState.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                outfitsViewModel.setFilterCategory(category)
                                isDropdownExpanded.value = false
                            }
                        )
                    }
                }
            }

            // Content area
            when (uiState) {
                is OutfitsViewModel.OutfitsUiState.Loading -> {
                    LoadingView()
                }
                is OutfitsViewModel.OutfitsUiState.Success -> {
                    val outfits = (uiState as OutfitsViewModel.OutfitsUiState.Success).outfits
                    OutfitsGrid(
                        outfits = outfits,
                        onOutfitClick = onOutfitClick,
                        onFavoriteClick = { outfit, isFavorite ->
                            outfitsViewModel.toggleFavorite(outfit.id, isFavorite)
                        },
                        emptyMessage = "No outfits found. Create your first outfit!",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is OutfitsViewModel.OutfitsUiState.Empty -> {
                    EmptyState(
                        message = "No outfits found. Create your first outfit!",
                        modifier = Modifier.weight(1f)
                    )
                }
                is OutfitsViewModel.OutfitsUiState.Error -> {
                    ErrorView(
                        message = (uiState as OutfitsViewModel.OutfitsUiState.Error).message,
                        onRetry = { outfitsViewModel.loadOutfits() }
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Red,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Retry")
        }
    }
}

@Composable
fun OutfitsGrid(
    outfits: List<Outfit>,
    onOutfitClick: (Outfit) -> Unit,
    onFavoriteClick: (Outfit, Boolean) -> Unit,
    emptyMessage: String,
    modifier: Modifier = Modifier
) {
    if (outfits.isEmpty()) {
        EmptyState(message = emptyMessage, modifier = modifier)
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 88.dp),
            modifier = modifier
        ) {
            items(outfits) { outfit ->
                OutfitCard(
                    outfit = outfit,
                    onClick = { onOutfitClick(outfit) },
                    onFavoriteClick = { isFavorite -> onFavoriteClick(outfit, isFavorite) }
                )
            }
        }
    }
}

@Composable
fun OutfitCard(
    outfit: Outfit,
    onClick: () -> Unit,
    onFavoriteClick: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box {
                // Use AsyncImage with Coil to load from URLs
                if (outfit.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(outfit.imageUrl)
                            .crossfade(true)
                            .placeholder(R.drawable.ic_outfit)
                            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            .build(),
                        contentDescription = outfit.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Fallback to resource image if URL is empty
                    Image(
                        painter = painterResource(id = outfit.imageRes),
                        contentDescription = outfit.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { onFavoriteClick(!outfit.isFavorite) }
                    ) {
                        Icon(
                            imageVector = if (outfit.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (outfit.isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (outfit.isFavorite) Color.Red else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = outfit.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = outfit.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_outfit),
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = Color.LightGray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
} 