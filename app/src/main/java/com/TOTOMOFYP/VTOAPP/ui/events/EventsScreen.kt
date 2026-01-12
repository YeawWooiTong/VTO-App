package com.TOTOMOFYP.VTOAPP.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    onAddEventClick: () -> Unit,
    onEventClick: (Event) -> Unit,
    viewModel: EventViewModel = viewModel(factory = EventViewModelFactory(LocalContext.current))
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    
    LaunchedEffect(Unit) {
        viewModel.loadEvents()
    }
    
    // Refresh events when returning to this screen (for example after adding an event)
    LaunchedEffect(uiState.upcomingEvents.size, uiState.pastEvents.size) {
        // This will trigger recomposition when events are added/removed
        android.util.Log.d("EventsScreen", "Events updated - Upcoming: ${uiState.upcomingEvents.size}, Past: ${uiState.pastEvents.size}")
    }
    
    // Combine all events for calendar display
    val allEvents = remember(uiState.upcomingEvents, uiState.pastEvents) {
        uiState.upcomingEvents + uiState.pastEvents
    }
    
    // Filter events for selected date
    val eventsForSelectedDate = remember(allEvents, selectedDate) {
        val targetDateString = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val targetDateString2 = selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        
        allEvents.filter { event ->
            // Check both possible date formats
            event.date == targetDateString || event.date == targetDateString2
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
        ) {
            // Title like wardrobe page
            Text(
                text = "Events",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 16.dp)
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                // Calendar Section
                item {
                    EventCalendar(
                        events = allEvents,
                        selectedDate = selectedDate,
                        onDateSelected = { date ->
                            selectedDate = date
                        },
                        onAddEventClick = { date ->
                            // If date is provided, pass it to add event screen
                            // For now, just navigate to add event
                            onAddEventClick()
                        }
                    )
                }
                
                // Calendar Legend
                item {
                    CalendarLegend()
                }
                
                // Events for Selected Date Section
                item {
                    EventsSection(
                        title = "Events on ${formatSelectedDate(selectedDate)}",
                        icon = Icons.Default.Event,
                        events = eventsForSelectedDate,
                        onEventClick = onEventClick,
                        emptyMessage = "No events on this date. Tap + on calendar to add an event!"
                    )
                }
                
                // All Upcoming Events Section
                item {
                    EventsSection(
                        title = "All Upcoming Events",
                        icon = Icons.Default.Event,
                        events = uiState.upcomingEvents,
                        onEventClick = onEventClick,
                        emptyMessage = "No upcoming events. Tap + on calendar to create your first event!"
                    )
                }
                
                // Past Events Section
                if (uiState.pastEvents.isNotEmpty()) {
                    item {
                        EventsSection(
                            title = "Past Events",
                            icon = Icons.Default.History,
                            events = uiState.pastEvents,
                            onEventClick = onEventClick,
                            emptyMessage = "No past events yet."
                        )
                    }
                }
            }
            
            // Error handling
            uiState.errorMessage?.let { error ->
                LaunchedEffect(error) {
                    // Show snackbar or handle error
                }
            }
        }
    }
    }
}

// Helper function to format the selected date
fun formatSelectedDate(date: LocalDate): String {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val yesterday = today.minusDays(1)
    
    return when (date) {
        today -> "Today"
        tomorrow -> "Tomorrow" 
        yesterday -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
}

@Composable
fun EventsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    events: List<Event>,
    onEventClick: (Event) -> Unit,
    emptyMessage: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    events.forEach { event ->
                        EventCard(
                            event = event,
                            onClick = { onEventClick(event) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EventCard(
    event: Event,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Outfit image or type indicator
            if (!event.outfitImageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(event.outfitImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Event outfit",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = event.eventType.displayName.first().toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "${event.date} • ${event.time}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                if (event.location.isNotEmpty()) {
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                Text(
                    text = event.eventType.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            
        }
    }
}

 