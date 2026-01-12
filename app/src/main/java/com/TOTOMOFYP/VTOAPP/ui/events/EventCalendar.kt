package com.TOTOMOFYP.VTOAPP.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCalendar(
    events: List<Event>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onAddEventClick: (LocalDate?) -> Unit, // null for general add, specific date for date-specific add
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    
    // Get events for the current month to show indicators
    val eventsInMonth = remember(events, currentMonth) {
        events.groupBy { event ->
            try {
                // Try different date formats that might be used
                val parsedDate = try {
                    LocalDate.parse(event.date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                } catch (e: Exception) {
                    try {
                        LocalDate.parse(event.date, DateTimeFormatter.ofPattern("MMM d, yyyy"))
                    } catch (e2: Exception) {
                        null
                    }
                }
                parsedDate
            } catch (e: Exception) {
                null
            }
        }.filterKeys { it != null && YearMonth.from(it) == currentMonth }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Calendar Header with Month/Year and Add button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ⬅️ Left side: navigation + month text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f) // this part stretches
                ) {
                    IconButton(
                        onClick = { currentMonth = currentMonth.minusMonths(1) }
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                    }

                    Text(
                        text = currentMonth.format(
                            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    IconButton(
                        onClick = { currentMonth = currentMonth.plusMonths(1) }
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                    }
                }

                // ➕ Right side: Mini FloatingActionButton
                FloatingActionButton(
                    onClick = { onAddEventClick(null) },
                    modifier = Modifier.size(40.dp), // mini size
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Event",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Days of week header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Calendar Grid
            CalendarGrid(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                eventsInMonth = eventsInMonth,
                onDateSelected = onDateSelected,
                onDateLongPress = { date ->
                    onAddEventClick(date)
                }
            )
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    eventsInMonth: Map<LocalDate?, List<Event>>,
    onDateSelected: (LocalDate) -> Unit,
    onDateLongPress: (LocalDate) -> Unit
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // Convert to 0-6 (Sun-Sat)
    val daysInMonth = currentMonth.lengthOfMonth()
    val today = LocalDate.now()
    
    // Create a list of all calendar cells (including empty ones for previous month)
    val calendarDays = mutableListOf<LocalDate?>()
    
    // Add empty cells for days before the first day of the month
    repeat(firstDayOfWeek) {
        calendarDays.add(null)
    }
    
    // Add all days of the current month
    repeat(daysInMonth) { day ->
        calendarDays.add(currentMonth.atDay(day + 1))
    }
    
    // Add empty cells to complete the grid (make it 6 rows)
    while (calendarDays.size < 42) {
        calendarDays.add(null)
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.height(280.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        items(calendarDays) { date ->
            CalendarDayCell(
                date = date,
                isSelected = date == selectedDate,
                isToday = date == today,
                hasEvents = eventsInMonth[date]?.isNotEmpty() == true,
                eventCount = eventsInMonth[date]?.size ?: 0,
                onDateClick = { 
                    date?.let { 
                        onDateSelected(it)
                    }
                },
                onDateLongClick = { date?.let(onDateLongPress) }
            )
        }
    }
}

@Composable
fun CalendarDayCell(
    date: LocalDate?,
    isSelected: Boolean,
    isToday: Boolean,
    hasEvents: Boolean,
    eventCount: Int,
    onDateClick: () -> Unit,
    onDateLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp) // Fixed size instead of aspectRatio to prevent overflow
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = date != null) {
                onDateClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isToday -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                )
                
                // Event indicator
                if (hasEvents) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isToday -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                        )
                        
                        if (eventCount > 1) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                            )
                        }
                        
                        if (eventCount > 2) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem(
            color = MaterialTheme.colorScheme.primaryContainer,
            text = "Today"
        )
        LegendItem(
            color = MaterialTheme.colorScheme.primary,
            text = "Selected"
        )
        LegendItem(
            color = MaterialTheme.colorScheme.primary,
            text = "Has Events",
            showDot = true
        )
    }
}

@Composable
fun LegendItem(
    color: Color,
    text: String,
    showDot: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}