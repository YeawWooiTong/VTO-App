package com.TOTOMOFYP.VTOAPP.ui.tryon

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.TOTOMOFYP.VTOAPP.R
import com.TOTOMOFYP.VTOAPP.auth.AuthViewModel

@Composable
fun TryOnScreen(
    authViewModel: AuthViewModel,
    onTryOnClick: () -> Unit,
    onTryOnResultClick: (TryOnResult) -> Unit,
    onVirtualTryOnClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val currentUser by authViewModel.currentUser.observeAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 16.dp)
    ) {
        // Top bar
        Text(
            text = "Virtual Try-On",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        // Avatar container
        AvatarContainer(
            userName = currentUser?.displayName,
            onTryOnClick = onTryOnClick,
            onVirtualTryOnClick = onVirtualTryOnClick
        )

        // Recent try-ons section
        Text(
            text = "Recent Try-Ons",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        RecentTryOnsList(
            userId = currentUser?.uid,
            onTryOnResultClick = onTryOnResultClick
        )

        // Saved outfits section
        Text(
            text = "Saved Outfits",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        SavedOutfitsList(
            userId = currentUser?.uid,
            onTryOnResultClick = onTryOnResultClick
        )
    }
}

@Composable
fun AvatarContainer(
    userName: String?,
    onTryOnClick: () -> Unit,
    onVirtualTryOnClick: () -> Unit
) {
    val greeting = if (userName.isNullOrEmpty()) {
        "Try on clothes from your wardrobe"
    } else {
        "${userName.split(" ").firstOrNull()}'s virtual wardrobe"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
                
                // In real app, this would show an actual avatar or placeholder
                // Image(painter = painterResource(id = R.drawable.avatar_placeholder), contentDescription = null)
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onTryOnClick,
                    modifier = Modifier.fillMaxWidth(0.8f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Try On Now",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onVirtualTryOnClick,
                    modifier = Modifier.fillMaxWidth(0.8f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = "AI Try-On Experience",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RecentTryOnsList(
    userId: String?,
    onTryOnResultClick: (TryOnResult) -> Unit
) {
    // In a real app, would filter by userId
    // Sample data
    val tryOnResults = List(5) { index ->
        TryOnResult(
            id = index.toLong(),
            name = "Try-On ${index + 1}",
            imageRes = R.drawable.ic_outfit,
            date = "${index + 1} hours ago",
            isFavorite = index % 2 == 0
        )
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(tryOnResults) { result ->
            TryOnResultItem(
                result = result,
                onClick = { onTryOnResultClick(result) }
            )
        }
    }
}

@Composable
fun SavedOutfitsList(
    userId: String?,
    onTryOnResultClick: (TryOnResult) -> Unit
) {
    // In a real app, would filter by userId
    // Sample data
    val savedOutfits = List(5) { index ->
        TryOnResult(
            id = index.toLong() + 100,
            name = "Outfit ${index + 1}",
            imageRes = R.drawable.ic_outfit,
            date = "Saved on ${index + 1}/10/2023",
            isFavorite = true
        )
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(savedOutfits) { outfit ->
            TryOnResultItem(
                result = outfit,
                onClick = { onTryOnResultClick(outfit) }
            )
        }
    }
}

@Composable
fun TryOnResultItem(
    result: TryOnResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .size(width = 180.dp, height = 260.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box {
                Image(
                    painter = painterResource(id = result.imageRes),
                    contentDescription = result.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
                
                IconButton(
                    onClick = { /* Toggle favorite */ },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = if (result.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (result.isFavorite) Color.Red else Color.White
                    )
                }
                
                IconButton(
                    onClick = { /* Share */ },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White
                    )
                }
            }
            
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = result.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = result.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Data class
data class TryOnResult(
    val id: Long,
    val name: String,
    val imageRes: Int,
    val date: String,
    val isFavorite: Boolean
) 