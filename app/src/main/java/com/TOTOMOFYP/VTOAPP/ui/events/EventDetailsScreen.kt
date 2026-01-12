package com.TOTOMOFYP.VTOAPP.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.foundation.clickable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(
    event: Event,
    onNavigateBack: () -> Unit,
    onEditEvent: () -> Unit,
    onTryOnOutfit: () -> Unit,
    onChangeOutfit: () -> Unit,
    viewModel: EventViewModel = viewModel(factory = EventViewModelFactory(LocalContext.current))
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFullScreenOutfit by remember { mutableStateOf(false) }
    var currentEvent by remember { mutableStateOf(event) }
    val scrollState = rememberScrollState()
    
    // Get the current event state from ViewModel to ensure we have the latest data
    val uiState by viewModel.uiState.collectAsState()
    
    // Update current event when the ViewModel state changes
    LaunchedEffect(uiState.upcomingEvents, uiState.pastEvents, currentEvent.id) {
        val allEvents = uiState.upcomingEvents + uiState.pastEvents
        val updatedEvent = allEvents.find { it.id == currentEvent.id }
        if (updatedEvent != null) {
            currentEvent = updatedEvent
        }
    }

    Scaffold(
        topBar = {
            // Empty top bar like wardrobe and outfit pages
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // 🔹 Wrap everything in ONE card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) // 👈 transparency
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header Row (Back, Title, Edit, Delete)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                            Text(
                                text = "Event Details",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row {
                            IconButton(onClick = onEditEvent) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Title + Event Type + Completed Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(currentEvent.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = currentEvent.eventType.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Date & Time
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Date & Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(currentEvent.date, style = MaterialTheme.typography.bodyLarge)
                        if (!currentEvent.isAllDay) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(currentEvent.time, style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            Text("All Day", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    // Location
                    if (currentEvent.location.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(currentEvent.location, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    // Description
                    if (currentEvent.description.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(currentEvent.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    // Outfit
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Planned Outfit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (currentEvent.outfitImageUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(currentEvent.outfitImageUrl).crossfade(true).build(),
                                contentDescription = "Planned outfit",
                                modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        } else if (currentEvent.plannedOutfitId != null) {
                            Text("Outfit Selected (No preview available)", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            TextButton(onClick = onChangeOutfit) { Text("Add Outfit") }
                        }
                    }

                    // Reminder
                    if (currentEvent.reminderEnabled) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Reminder Active", fontWeight = FontWeight.Bold)
                                Text("You'll be notified 1 hour before the event", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Event") },
            text = { Text("Are you sure you want to delete this event? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEvent(currentEvent.id)
                        showDeleteDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Complete Confirmation Dialog
    
    // Full Screen Outfit Image Dialog
    if (showFullScreenOutfit && currentEvent.outfitImageUrl != null) {
        Dialog(
            onDismissRequest = { showFullScreenOutfit = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { showFullScreenOutfit = false }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(currentEvent.outfitImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Full size outfit",
                    modifier = Modifier
                        .fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                
                // Close button
                IconButton(
                    onClick = { showFullScreenOutfit = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(50)
                        )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
}