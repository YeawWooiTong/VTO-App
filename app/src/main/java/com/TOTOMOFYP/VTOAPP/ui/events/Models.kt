package com.TOTOMOFYP.VTOAPP.ui.events

import java.util.Date

data class Event(
    val id: String = "",
    val title: String = "",
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val description: String = "",
    val plannedOutfitId: String? = null,
    val outfitImageUrl: String? = null,
    val isAllDay: Boolean = false,
    val reminderEnabled: Boolean = true,
    val reminderTime: Long = 1L, // Hours before event to remind (default 1 hour)
    val eventType: EventType = EventType.OTHER,
    val createdAt: Long = System.currentTimeMillis()
)

enum class EventType(val displayName: String) {
    DINNER("Dinner"),
    WEDDING("Wedding"),
    PARTY("Party"),
    MEETING("Meeting"),
    VACATION("Vacation"),
    DAILY("Daily Outfit"),
    DATE("Date"),
    BUSINESS("Business"),
    CASUAL("Casual"),
    OTHER("Other")
}

data class EventReminder(
    val eventId: String,
    val eventTitle: String,
    val eventDateTime: Long,
    val outfitImageUrl: String?,
    val isActive: Boolean = true
) 