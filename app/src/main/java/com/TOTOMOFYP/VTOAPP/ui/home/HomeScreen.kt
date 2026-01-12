package com.TOTOMOFYP.VTOAPP.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.TOTOMOFYP.VTOAPP.R
import com.TOTOMOFYP.VTOAPP.auth.AuthViewModel

@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    onTryOnClick: () -> Unit,
    onWardrobeClick: () -> Unit,
    onOutfitsClick: () -> Unit,
    onFavoritesClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val currentUser by authViewModel.currentUser.observeAsState()
    
    // Get user's first name for welcome message
    val firstName = currentUser?.displayName?.split(" ")?.firstOrNull() ?: "there"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 84.dp)
    ) {
        // Welcome section
        Text(
            text = "Welcome, $firstName!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        // Quick actions section
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        QuickActionsGrid(
            onTryOnClick = onTryOnClick,
            onWardrobeClick = onWardrobeClick,
            onOutfitsClick = onOutfitsClick,
            onFavoritesClick = onFavoritesClick
        )

        // Recent activities section
        Text(
            text = "Recent Activities",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        RecentActivitiesList()

        // Recommended section
        Text(
            text = "Recommended for You",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        RecommendationsList()
    }
}

@Composable
fun QuickActionsGrid(
    onTryOnClick: () -> Unit,
    onWardrobeClick: () -> Unit,
    onOutfitsClick: () -> Unit,
    onFavoritesClick: () -> Unit
) {
    val quickActions = listOf(
        QuickAction(R.drawable.ic_outfit, "Outfits", onOutfitsClick)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        contentPadding = PaddingValues(horizontal = 8.dp),
        modifier = Modifier.height(80.dp)
    ) {
        items(quickActions) { action ->
            QuickActionItem(action)
        }
    }
}

@Composable
fun QuickActionItem(action: QuickAction) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .clickable { action.action() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Image(
                painter = painterResource(id = action.iconRes),
                contentDescription = action.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .size(32.dp)
                    .padding(4.dp)
            )
            
            Text(
                text = action.title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RecentActivitiesList() {
    // Sample data
    val activities = List(5) { index ->
        RecentActivity(
            id = index.toLong(),
            title = "Activity ${index + 1}",
            imageRes = R.drawable.ic_camera,
            time = "${index + 1} hours ago"
        )
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(activities) { activity ->
            RecentActivityItem(activity)
        }
    }
}

@Composable
fun RecentActivityItem(activity: RecentActivity) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .size(width = 160.dp, height = 200.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Image(
                painter = painterResource(id = activity.imageRes),
                contentDescription = activity.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentScale = ContentScale.Crop
            )
            
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = activity.time,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun RecommendationsList() {
    // Sample data
    val recommendations = List(5) { index ->
        Recommendation(
            id = index.toLong(),
            title = "Outfit ${index + 1}",
            description = "Perfect for ${if (index % 2 == 0) "casual" else "formal"} occasions",
            imageRes = R.drawable.ic_outfit
        )
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(recommendations) { recommendation ->
            RecommendationItem(recommendation)
        }
    }
}

@Composable
fun RecommendationItem(recommendation: Recommendation) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .size(width = 140.dp, height = 220.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Image(
                painter = painterResource(id = recommendation.imageRes),
                contentDescription = recommendation.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentScale = ContentScale.Crop
            )
            
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = recommendation.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = recommendation.description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// Data classes
data class RecentActivity(
    val id: Long,
    val title: String,
    val imageRes: Int,
    val time: String
)

data class Recommendation(
    val id: Long,
    val title: String,
    val description: String,
    val imageRes: Int
) 